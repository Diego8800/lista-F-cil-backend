import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import { neonAuthBaseUrl } from "../env.js";

declare module "fastify" {
  interface FastifyRequest {
    /** Dados da sessão validada (preHandler `authenticate`). */
    auth: { userId: string; token: string };
  }
  interface FastifyInstance {
    authenticate: (req: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }
}

/**
 * Valida a sessão diretamente na API do Neon Auth.
 * Suporta o token de sessão enviado pelo app Android.
 */
export function registerAuth(app: FastifyInstance): void {
  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      // Valida o token de sessão diretamente no Neon Auth
      const response = await fetch(`${neonAuthBaseUrl}/get-session`, {
        headers: {
          "Authorization": `Bearer ${token}`,
          "Origin": "http://localhost:3000"
        }
      });

      if (!response.ok) {
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      const data = await response.json();
      const userId = data?.user?.id || data?.session?.userId;

      if (!userId) {
        return reply.code(401).send({ error: "Usuário não encontrado na sessão" });
      }

      req.auth = { userId, token };
    } catch {
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
