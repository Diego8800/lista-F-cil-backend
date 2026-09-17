import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";

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
 */
export function registerAuth(app: FastifyInstance): void {
  const rawBase = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");
  const baseUrl = rawBase.endsWith("/auth") ? rawBase.slice(0, -5) : rawBase;

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
          "Authorization": `Bearer ${token}`
        }
      });

      if (!response.ok) {
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      const rawText = await response.text();
      
      // Se a resposta for "null" ou vazia, a sessão expirou no Neon Auth
      if (!rawText || rawText.trim() === "null") {
        return reply.code(401).send({ error: "Sessão expirada. Faça login novamente." });
      }

      const data = JSON.parse(rawText);

      const userId =
        data?.user?.id ||
        data?.session?.userId ||
        data?.session?.user?.id ||
        data?.userId ||
        data?.id;

      if (!userId) {
        return reply.code(401).send({ error: "Sessão expirada. Faça login novamente." });
      }

      req.auth = { userId, token };
    } catch (err) {
      console.error("[Auth Exception]:", err);
      return reply.code(401).send({ error: "Falha ao validar autenticação" });
    }
  });
}
