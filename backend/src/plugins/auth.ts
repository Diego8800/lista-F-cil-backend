import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";

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
          "Cookie": `better-auth.session_token=${token}; __Secure-better-auth.session_token=${token}; neon-auth.session_token=${token}; __Secure-neon-auth.session_token=${token}`
        }
      });

      const rawText = await response.text();
      console.log("[Auth Debug] status:", response.status, "body:", rawText);

      if (!response.ok) {
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      if (!rawText || rawText.trim() === "null") {
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

      req.auth = { userId, token };
    } catch (err) {
      console.error("[Auth Error]:", err);
      return reply.code(401).send({ error: "Falha ao validar autenticação" });
    }
  });
}
