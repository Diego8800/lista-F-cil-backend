import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import { db } from "../db/client.js";

declare module "fastify" {
  interface FastifyRequest {
    auth: { userId: string; token: string };
  }
  interface FastifyInstance {
    authenticate: (req: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }
}

export function registerAuth(app: FastifyInstance): void {
  const rawBase = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");
  const baseUrl = rawBase.endsWith("/auth") ? rawBase.slice(0, -5) : rawBase;
  const origin = "https://lista-f-cil-backend-production.up.railway.app";

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      const targetUrl = `${baseUrl}/auth/get-session`;

      const response = await fetch(targetUrl, {
        method: "GET",
        headers: {
          "Origin": origin,
          "Cookie": `__Secure-neon-auth.session_token=${token}`
        }
      });

      const rawText = await response.text();

      if (!response.ok || !rawText || rawText.trim() === "null") {
        return reply.code(401).send({ error: "Sessão expirada. Faça login novamente." });
      }

      let data: any = {};
      try {
        data = JSON.parse(rawText);
      } catch (e) {
        return reply.code(401).send({ error: "Resposta de sessão inválida" });
      }

      const userId =
        data?.user?.id ||
        data?.session?.userId ||
        data?.session?.user?.id ||
        data?.userId ||
        data?.id;

      if (!userId) {
        return reply.code(401).send({ error: "Usuário não encontrado na sessão" });
      }

      // Garante que o usuário existe na tabela local
      const sql = db(token);
      await sql`INSERT INTO "user" (id) VALUES (${userId}) ON CONFLICT (id) DO NOTHING`;

      req.auth = { userId, token };
    } catch (err) {
      console.error("[Auth Error]:", err);
      return reply.code(401).send({ error: "Falha ao validar autenticação" });
    }
  });
}
