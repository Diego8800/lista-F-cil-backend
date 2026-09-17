import type { FastifyInstance } from "fastify";
import { db } from "../db/client.js";

export async function productRoutes(app: FastifyInstance): Promise<void> {
  // GET /products?q=texto — autocomplete de produtos já usados
  app.get("/products", async (req) => {
    const { userId, token } = req.auth;
    const { q = "" } = req.query as { q?: string };
    const sql = db(token);
    const rows = await sql(
      `SELECT DISTINCT p.id, p.name FROM products p
       WHERE p.user_id = $1 AND p.name ILIKE $2
       ORDER BY p.name LIMIT 10`,
      [userId, `%${q}%`]
    );
    return { products: rows };
  });

  // GET /products/:id/history
  app.get("/products/:id/history", async (req, reply) => {
    const { id } = req.params as { id: string };
    const { userId, token } = req.auth;
    const sql = db(token);

    const product = await sql`
      SELECT id, name FROM products WHERE id = ${id} AND user_id = ${userId}`;
    if (product.length === 0) {
      return reply.code(404).send({ error: "Produto não encontrado" });
    }

    const latest = (await sql`
      SELECT price_cents, unit FROM price_history
      WHERE product_id = ${id}
      ORDER BY recorded_at DESC LIMIT 1`) as Array<{ price_cents: number; unit: string }>;

    const previous = (await sql`
      SELECT price_cents FROM price_history
      WHERE product_id = ${id}
      ORDER BY recorded_at DESC LIMIT 1 OFFSET 1`) as Array<{ price_cents: number }>;

    const agg = (await sql`
      SELECT min(price_cents) AS min_cents, max(price_cents) AS max_cents,
             avg(price_cents)::float8 AS avg_cents
      FROM price_history WHERE product_id = ${id}`) as Array<{
      min_cents: number | null;
      max_cents: number | null;
      avg_cents: number | null;
    }>;

    const history = await sql`
      SELECT h.id, h.price_cents, h.quantity::float8 AS quantity, h.unit,
             h.recorded_at, h.list_id,
             (SELECT e.name FROM establishments e WHERE e.id = h.establishment_id) AS establishment_name
      FROM price_history h
      WHERE h.product_id = ${id}
      ORDER BY h.recorded_at DESC`;

    return {
      product_id: id,
      product_name: (product[0] as { name: string }).name,
      stats: {
        current_cents: latest[0]?.price_cents ?? null,
        previous_cents: previous[0]?.price_cents ?? null,
        min_cents: agg[0]?.min_cents ?? null,
        max_cents: agg[0]?.max_cents ?? null,
        avg_cents: agg[0]?.avg_cents != null ? Math.round(agg[0].avg_cents) : null,
      },
      history,
    };
  });
}
