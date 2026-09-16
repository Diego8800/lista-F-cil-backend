import { createRemoteJWKSet, jwtVerify } from "jose";
import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import { jwksUrl } from "../env.js";

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
 * Valida o JWT da sessão Neon Auth em TODA requisição protegida.
 * O user_id usado nas queries NUNCA vem do corpo da requisição:
 * vem do `sub` do JWT validado via JWKS.
 */
export function registerAuth(app: FastifyInstance): void {
  const jwks = createRemoteJWKSet(new URL(jwksUrl));

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }
    const token = header.slice("Bearer ".length).trim();
    try {
      const { payload } = await jwtVerify(token, jwks);
      if (!payload.sub) throw new Error("JWT sem subject");
      req.auth = { userId: payload.sub, token };
    } catch {
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
