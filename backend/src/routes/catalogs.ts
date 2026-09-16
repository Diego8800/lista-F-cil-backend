import type { FastifyInstance, FastifyRequest } from "fastify";
import { z } from "zod";
import { db } from "../db/client.js";

const nameSchema = z.object({
  name: z.string().trim().min(1, "Nome é obrigatório").max(120),
});

const DEFAULT_CATEGORIES = [
  "Hortifruti", "Açougue", "Laticínios", "Padaria", "Mercearia",
  "Bebidas", "Limpeza", "Higiene", "Congelados", "Outros",
];

type Table = "categories" | "establishments";

function isViol(err: unknown, code: string): boolean {
  return (err as { code?: string }).code === code;
}

/**
 * CRUD genérico. O nome da tabela é um literal de união ("categories" |
 * "establishments") interpolado em string SQL — sem risco de injeção e
 * compatível com o driver serverless (sem sql.unsafe).
 */
function registerCrud(app: FastifyInstance, path: string, table: Table): void {
  app.get(`/${path}`, async (req) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    return {
      [path]: await sql(
        `SELECT id, name FROM ${table} WHERE user_id = $1 ORDER BY name`,
        [userId],
      ),
    };
  });

  app.post(`/${path}`, async (req, reply) => {
    const parsed = nameSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    try {
      const rows = await sql(
        `INSERT INTO ${table} (user_id, name) VALUES ($1, $2) RETURNING id, name`,
        [userId, parsed.data.name],
      );
      return reply.code(201).send(rows[0]);
    } catch (err) {
      if (isViol(err, "23505")) {
        return reply.code(409).send({ error: "Já existe um registro com esse nome" });
      }
      throw err;
    }
  });

  app.patch(`/${path}/:id`, async (req, reply) => {
    const { id } = req.params as { id: string };
    const parsed = nameSchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ error: parsed.error.issues[0].message });
    }
    const { userId, token } = req.auth;
    const sql = db(token);
    try {
      const rows = await sql(
        `UPDATE ${table} SET name = $3 WHERE id = $1 AND user_id = $2 RETURNING id, name`,
        [id, userId, parsed.data.name],
      );
      if (rows.length === 0) {
        return reply.code(404).send({ error: "Registro não encontrado" });
      }
      return rows[0];
    } catch (err) {
      if (isViol(err, "23505")) {
        return reply.code(409).send({ error: "Já existe um registro com esse nome" });
      }
      throw err;
    }
  });

  app.delete(`/${path}/:id`, async (req, reply) => {
    const { id } = req.params as { id: string };
    const { userId, token } = req.auth;
    const sql = db(token);
    try {
      const rows = await sql(
        `DELETE FROM ${table} WHERE id = $1 AND user_id = $2 RETURNING id`,
        [id, userId],
      );
      if (rows.length === 0) {
        return reply.code(404).send({ error: "Registro não encontrado" });
      }
      return reply.code(204).send();
    } catch (err) {
      if (isViol(err, "23503")) {
        return reply.code(409).send({ error: "Registro em uso e não pode ser excluído" });
      }
      throw err;
    }
  });
}

export async function catalogRoutes(app: FastifyInstance): Promise<void> {
  registerCrud(app, "categories", "categories");
  registerCrud(app, "establishments", "establishments");

  // POST /categories/seed — categorias padrão do usuário (idempotente)
  app.post("/categories/seed", async (req: FastifyRequest) => {
    const { userId, token } = req.auth;
    const sql = db(token);
    for (const name of DEFAULT_CATEGORIES) {
      await sql`
        INSERT INTO categories (user_id, name) VALUES (${userId}, ${name})
        ON CONFLICT DO NOTHING`;
    }
    return { seeded: DEFAULT_CATEGORIES.length };
  });
}
