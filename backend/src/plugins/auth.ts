import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import * as jose from "jose";

declare module "fastify" {
  interface FastifyRequest {
    auth: { userId: string; token: string };
  }
  interface FastifyInstance {
    authenticate: (req: FastifyRequest, reply: FastifyReply) => Promise<void>;
  }
}

export function registerAuth(app: FastifyInstance): void {
  const jwksUrl = process.env.NEON_AUTH_JWKS_URL || "";
  const JWKS = jose.createRemoteJWKSet(new URL(jwksUrl));

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      const { payload } = await jose.jwtVerify(token, JWKS);
      const userId = (payload.sub || payload["userId"] || payload["id"]) as string;

      if (!userId) {
        return reply.code(401).send({ error: "Usuário não encontrado no token" });
      }

      req.auth = { userId, token };
    } catch (err) {
      console.error("[Auth Error]:", err);
      return reply.code(401).send({ error: "Sessão expirada. Faça login novamente." });
    }
  });
}
