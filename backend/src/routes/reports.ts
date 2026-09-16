import type { FastifyInstance, FastifyRequest } from "fastify";
import { db } from "../db/client.js";

type Range = { from?: string; to?: string };

function rangeClause(alias: string): string {
  return `AND ${alias}.finished_at >= COALESCE($2::timestamptz, '-infinity'::timestamptz)
          AND ${alias}.finished_at <= COALESCE($3::timestamptz, 'infinity'::timestamptz)`;
}

function rangeParams(req: FastifyRequest): [string | null, string | null] {
  const { from, to } = req.query as Range;
  return [from ?? null, to ?? null];
}

export async function reportRoutes(app: FastifyInstance): Promise<void> {
  // Total gasto por lista (listas finalizadas)
  app.get("/reports/spent-by-list", async (req) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    const [from, to] = rangeParams(req);
    return {
      rows: await sql(
        `SELECT l.id, l.name, l.finished_at,
                COALESCE(SUM(i.current_price_cents), 0) AS total_cents
         FROM lists l
         LEFT JOIN list_items i ON i.list_id = l.id AND i.purchased
         WHERE l.user_id = $1 AND l.status = 'finished' ${rangeClause("l")}
         GROUP BY l.id
         ORDER BY l.finished_at DESC`,
        [userId, from, to],
      ),
    };
  });

  // Gastos por categoria
  app.get("/reports/spent-by-category", async (req) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    const [from, to] = rangeParams(req);
    return {
      rows: await sql(
        `SELECT c.name AS category_name, COALESCE(SUM(i.current_price_cents), 0) AS total_cents
         FROM list_items i
         JOIN lists l ON l.id = i.list_id
         JOIN categories c ON c.id = i.category_id
         WHERE l.user_id = $1 AND l.status = 'finished' ${rangeClause("l")}
         GROUP BY c.name
         ORDER BY total_cents DESC`,
        [userId, from, to],
      ),
    };
  });

  // Gastos por período (dia da finalização)
  app.get("/reports/spent-by-period", async (req) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    const [from, to] = rangeParams(req);
    return {
      rows: await sql(
        `SELECT to_char(l.finished_at::date, 'YYYY-MM-DD') AS day,
                SUM(i.current_price_cents) AS total_cents
         FROM lists l
         JOIN list_items i ON i.list_id = l.id AND i.purchased
         WHERE l.user_id = $1 AND l.status = 'finished' ${rangeClause("l")}
         GROUP BY l.finished_at::date
         ORDER BY day`,
        [userId, from, to],
      ),
    };
  });

  // Evolução dos preços (por produto)
  app.get("/reports/price-evolution", async (req) => {
    const { userId, token } = req.auth;
    const { product_id } = req.query as { product_id?: string };
    const sql = db(token);
    return {
      rows: await sql(
        `SELECT p.id AS product_id, p.name AS product_name,
           COALESCE((
             SELECT json_agg(t ORDER BY t.recorded_at) FROM (
               SELECT h.recorded_at, h.price_cents, h.unit,
                      (SELECT e.name FROM establishments e WHERE e.id = h.establishment_id) AS establishment_name
               FROM price_history h
               WHERE h.product_id = p.id
             ) t
           ), '[]'::json) AS points
         FROM products p
         WHERE p.user_id = $1 AND ($2::uuid IS NULL OR p.id = $2)
         ORDER BY p.name`,
        [userId, product_id ?? null],
      ),
    };
  });

  // Maiores aumentos / reduções de preço (primeiro vs. último registro)
  const topChanges = (order: "DESC" | "ASC") => async (req: FastifyRequest) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    return {
      rows: await sql(
        `WITH bounds AS (
           SELECT h.product_id, h.unit,
                  (ARRAY_AGG(h.price_cents ORDER BY h.recorded_at ASC))[1]  AS first_cents,
                  (ARRAY_AGG(h.price_cents ORDER BY h.recorded_at DESC))[1] AS last_cents
           FROM price_history h
           WHERE h.user_id = $1
           GROUP BY h.product_id, h.unit
         )
         SELECT p.name AS product_name, b.unit, b.first_cents, b.last_cents,
                round(((b.last_cents - b.first_cents)::float8 / NULLIF(b.first_cents, 0)) * 100, 1) AS change_pct
         FROM bounds b
         JOIN products p ON p.id = b.product_id
         ORDER BY change_pct ${order}
         LIMIT 20`,
        [userId],
      ),
    };
  };

  app.get("/reports/top-increases", topChanges("DESC"));
  app.get("/reports/top-decreases", topChanges("ASC"));

  // Economia obtida escolhendo o menor preço
  app.get("/reports/savings", async (req) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    const rows = (await sql`
      WITH minp AS (
        SELECT product_id, unit, min(price_cents) AS min_cents
        FROM price_history
        WHERE user_id = ${userId}
        GROUP BY product_id, unit
      )
      SELECT p.name AS product_name,
             SUM(GREATEST(i.current_price_cents - m.min_cents, 0) * i.quantity)::bigint AS savings_cents
      FROM list_items i
      JOIN lists l ON l.id = i.list_id AND l.status = 'finished'
      JOIN minp m ON m.product_id = i.product_id AND m.unit = i.unit
      JOIN products p ON p.id = i.product_id
      WHERE l.user_id = ${userId} AND i.purchased
      GROUP BY p.name
      ORDER BY savings_cents DESC`) as Array<{ product_name: string; savings_cents: number }>;

    return {
      items: rows,
      total_cents: rows.reduce((acc, r) => acc + Number(r.savings_cents), 0),
    };
  });
}
