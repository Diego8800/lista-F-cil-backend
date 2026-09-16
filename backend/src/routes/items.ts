import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { db } from "../db/client.js";

const UNITS = ["kg", "g", "L", "ml", "un"] as const;

const addItemSchema = z.object({
  product_name: z.string().trim().min(1, "Nome do produto é obrigatório").max(120),
  category_id: z.string().uuid("Categoria inválida"),
  quantity: z.number().positive("Quantidade deve ser maior que zero").max(999999),
  unit: z.enum(UNITS),
  current_price_cents: z.number().int().min(0).nullable().optional(),
  note: z.string().max(500).nullable().optional(),
  establishment_id: z.string().uuid().nullable().optional(),
});

const updateItemSchema = z.object({
  quantity: z.number().positive().max(999999).optional(),
  unit: z.enum(UNITS).optional(),
  current_price_cents: z.number().int().min(0).nullable().optional(),
  note: z.string().max(500).nullable().optional(),
  establishment_id: z.string().uuid().nullable().optional(),
  purchased: z.boolean().optional(),
  category_id: z.string().uuid().optional(),
});

const ITEM_SELECT = `
  SELECT i.id, i.list_id, i.product_id, p.name AS product_name,
         i.category_id, c.name AS category_name,
         i.establishment_id, e.name AS establishment_name,
         i.quantity::float8 AS quantity, i.unit,
         i.previous_price_cents, i.current_price_cents, i.note, i.purchased
  FROM list_items i
  JOIN products p ON p.id = i.product_id
  JOIN categories c ON c.id = i.category_id
  LEFT JOIN establishments e ON e.id = i.establishment_id`;

export async function itemRoutes(app: FastifyInstance): Promise<void> {
  // POST /lists/:id/items
  // Cria o produto automaticamente (catálogo pessoal) e preenche o preço
  // anterior com o último preço registrado para o produto NA MESMA UNIDADE.
  app.post("/lists/:id/items", async (req, reply) => {
    const { id } = req.params as { id: string };
    const parsed = addItemSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    const body = parsed.data;

    const listRows = await sql`
      SELECT id FROM lists WHERE id = ${id} AND user_id = ${userId} AND status = 'active'`;
    if (listRows.length === 0) {
      return reply.code(404).send({ error: "Lista ativa não encontrada" });
    }

    const category = await sql`
      SELECT id FROM categories WHERE id = ${body.category_id} AND user_id = ${userId}`;
    if (category.length === 0) {
      return reply.code(404).send({ error: "Categoria não encontrada" });
    }

    if (body.establishment_id) {
      const est = await sql`
        SELECT id FROM establishments WHERE id = ${body.establishment_id} AND user_id = ${userId}`;
      if (est.length === 0) {
        return reply.code(404).send({ error: "Estabelecimento não encontrado" });
      }
    }

    // Produto: reutiliza (case-insensitive) ou cria no catálogo do usuário.
    let productId: string;
    const existing = await sql`
      SELECT id FROM products WHERE user_id = ${userId} AND lower(name) = lower(${body.product_name})`;
    if (existing.length > 0) {
      productId = (existing[0] as { id: string }).id;
    } else {
      const created = await sql`
        INSERT INTO products (user_id, name) VALUES (${userId}, ${body.product_name})
        RETURNING id`;
      productId = (created[0] as { id: string }).id;
    }

    // Preço anterior: último registro histórico do produto com a MESMA unidade.
    const previous = await sql`
      SELECT price_cents FROM price_history
      WHERE product_id = ${productId} AND unit = ${body.unit}
      ORDER BY recorded_at DESC LIMIT 1`;
    const previousCents =
      (previous[0] as { price_cents: number } | undefined)?.price_cents ?? null;

    try {
      const inserted = await sql`
        INSERT INTO list_items
          (list_id, product_id, category_id, establishment_id, quantity, unit,
           previous_price_cents, current_price_cents, note)
        VALUES (${id}, ${productId}, ${body.category_id}, ${body.establishment_id ?? null},
                ${body.quantity}, ${body.unit}, ${previousCents},
                ${body.current_price_cents ?? null}, ${body.note ?? null})
        ON CONFLICT (list_id, product_id) DO NOTHING
        RETURNING id`;
      if (inserted.length === 0) {
        return reply.code(409).send({ error: "Este produto já está na lista" });
      }
      const itemId = (inserted[0] as { id: string }).id;
      const rows = await sql(`${ITEM_SELECT} WHERE i.id = $1`, [itemId]);
      return reply.code(201).send(rows[0]);
    } catch (err) {
      if ((err as { code?: string }).code === "23503") {
        return reply.code(404).send({ error: "Categoria ou estabelecimento não encontrado" });
      }
      throw err;
    }
  });

  // PATCH /lists/:id/items/:itemId
  app.patch("/lists/:id/items/:itemId", async (req, reply) => {
    const { id, itemId } = req.params as { id: string; itemId: string };
    const parsed = updateItemSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    const body = parsed.data;

    const existing = await sql`
      SELECT i.id, i.product_id, i.unit
      FROM list_items i
      JOIN lists l ON l.id = i.list_id
      WHERE i.id = ${itemId} AND i.list_id = ${id} AND l.user_id = ${userId} AND l.status = 'active'`;
    if (existing.length === 0) {
      return reply.code(404).send({ error: "Item não encontrado na lista ativa" });
    }
    const item = existing[0] as { id: string; product_id: string; unit: string };

    // Ao trocar a unidade, o preço anterior é recalculado para a nova unidade.
    const unitChanged = Boolean(body.unit && body.unit !== item.unit);
    let newPrevious: number | null = null;
    if (unitChanged) {
      const prev = await sql`
        SELECT price_cents FROM price_history
        WHERE product_id = ${item.product_id} AND unit = ${body.unit}
        ORDER BY recorded_at DESC LIMIT 1`;
      newPrevious = (prev[0] as { price_cents: number } | undefined)?.price_cents ?? null;
    }

    const rows = await sql`
      UPDATE list_items
      SET quantity            = COALESCE(${body.quantity ?? null}, quantity),
          unit                = COALESCE(${body.unit ?? null}, unit),
          current_price_cents = COALESCE(${body.current_price_cents ?? null}, current_price_cents),
          note                = COALESCE(${body.note ?? null}, note),
          establishment_id    = COALESCE(${body.establishment_id ?? null}, establishment_id),
          purchased           = COALESCE(${body.purchased ?? null}, purchased),
          category_id         = COALESCE(${body.category_id ?? null}, category_id),
          previous_price_cents = CASE WHEN ${unitChanged} THEN ${newPrevious} ELSE previous_price_cents END,
          updated_at          = now()
      WHERE id = ${itemId} AND list_id = ${id}
      RETURNING id`;
    if (rows.length === 0) {
      return reply.code(404).send({ error: "Item não encontrado" });
    }
    const result = await sql(`${ITEM_SELECT} WHERE i.id = $1`, [itemId]);
    return result[0];
  });

  // DELETE /lists/:id/items/:itemId
  app.delete("/lists/:id/items/:itemId", async (req, reply) => {
    const { id, itemId } = req.params as { id: string; itemId: string };
    const { userId, token } = req.auth;
    const sql = db(token);
    const rows = await sql`
      DELETE FROM list_items i
      USING lists l
      WHERE i.id = ${itemId} AND i.list_id = ${id} AND i.list_id = l.id
        AND l.user_id = ${userId} AND l.status = 'active'
      RETURNING i.id`;
    if (rows.length === 0) {
      return reply.code(404).send({ error: "Item não encontrado na lista ativa" });
    }
    return reply.code(204).send();
  });
}
