import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { db } from "../db/client.js";
import { finishList } from "../services/listService.js";

const LIST_SELECT = `
  SELECT l.id, l.name, l.budget_cents, l.status, l.finished_at, l.created_at,
    COALESCE((
      SELECT SUM(i.current_price_cents * i.quantity)::bigint
      FROM list_items i
      WHERE i.list_id = l.id AND i.purchased
    ), 0) AS spent_cents
  FROM lists l`;

const createListSchema = z.object({
  name: z.string().trim().min(1, "Nome da lista é obrigatório").max(120),
  budget_cents: z.number().int().min(0, "Budget inválido"),
});

const updateListSchema = z.object({
  name: z.string().trim().min(1).max(120).optional(),
  budget_cents: z.number().int().min(0).optional(),
});

const finishSchema = z.object({
  mode: z.enum(["delete", "transfer"]).optional(),
  new_list_name: z.string().trim().min(1).max(120).optional(),
});

export async function listRoutes(app: FastifyInstance): Promise<void> {
  // GET /lists?status=active|finished
  app.get("/lists", async (req) => {
    const { userId, token } = req.auth;
    const status = (req.query as { status?: string }).status ?? null;
    const sql = db(token);
    return {
      lists: await sql(
        `${LIST_SELECT} WHERE l.user_id = $1 AND ($2::text IS NULL OR l.status = $2)
         ORDER BY l.created_at DESC`,
        [userId, status],
      ),
    };
  });

  // POST /lists  (apenas 1 lista ativa por usuário)
  app.post("/lists", async (req, reply) => {
    const parsed = createListSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    const active = await sql`
      SELECT id FROM lists WHERE user_id = ${userId} AND status = 'active' LIMIT 1`;
    if (active.length > 0) {
      return reply.code(409).send({
        error: "Já existe uma lista ativa. Finalize-a antes de criar outra.",
      });
    }
    const { name, budget_cents } = parsed.data;
    const rows = await sql`
      INSERT INTO lists (user_id, name, budget_cents)
      VALUES (${userId}, ${name}, ${budget_cents})
      RETURNING id, name, budget_cents, status, finished_at, created_at`;
    return reply.code(201).send(rows[0]);
  });

  // GET /lists/:id  (lista + itens, com nomes de produto/categoria/estabelecimento)
  app.get("/lists/:id", async (req, reply) => {
    const { id } = req.params as { id: string };
    const { userId, token } = req.auth;
    const sql = db(token);

    const listRows = await sql(`${LIST_SELECT} WHERE l.id = $1 AND l.user_id = $2`, [id, userId]);
    if (listRows.length === 0) {
      return reply.code(404).send({ error: "Lista não encontrada" });
    }

    const items = await sql`
      SELECT i.id, i.list_id, i.product_id, p.name AS product_name,
             i.category_id, c.name AS category_name,
             i.establishment_id, e.name AS establishment_name,
             i.quantity::float8 AS quantity, i.unit,
             i.previous_price_cents, i.current_price_cents, i.note, i.purchased
      FROM list_items i
      JOIN products p ON p.id = i.product_id
      JOIN categories c ON c.id = i.category_id
      LEFT JOIN establishments e ON e.id = i.establishment_id
      WHERE i.list_id = ${id}
      ORDER BY i.created_at`;

    return { list: listRows[0], items };
  });

  // PATCH /lists/:id  (somente lista ativa: renomear / ajustar budget)
  app.patch("/lists/:id", async (req, reply) => {
    const { id } = req.params as { id: string };
    const parsed = updateListSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    const rows = await sql`
      UPDATE lists
      SET name = COALESCE(${parsed.data.name ?? null}, name),
          budget_cents = COALESCE(${parsed.data.budget_cents ?? null}, budget_cents),
          updated_at = now()
      WHERE id = ${id} AND user_id = ${userId} AND status = 'active'
      RETURNING id, name, budget_cents, status, finished_at, created_at`;
    if (rows.length === 0) {
      return reply.code(404).send({ error: "Lista ativa não encontrada" });
    }
    return rows[0];
  });

  // POST /lists/:id/finish
  app.post("/lists/:id/finish", async (req, reply) => {
    const { id } = req.params as { id: string };
    const parsed = finishSchema.safeParse(req.body ?? {});
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const result = await finishList({
      token,
      userId,
      listId: id,
      mode: parsed.data.mode,
      newListName: parsed.data.new_list_name,
    });
    if (!result.ok) {
      const body: Record<string, unknown> = { error: result.error };
      if (result.nonPurchasedCount != null) body.non_purchased_count = result.nonPurchasedCount;
      return reply.code(result.status).send(body);
    }
    return {
      finished_list_id: result.finishedListId,
      new_list_id: result.newListId,
      non_purchased_count: result.nonPurchasedCount,
    };
  });
}
