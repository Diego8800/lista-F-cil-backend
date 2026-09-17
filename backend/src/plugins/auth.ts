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
  // Garante que a URL base termine sem barra final
  const neonAuthUrl = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      // O endpoint correto do Neon/Better Auth é /auth/get-session ou /get-session
      const targetUrl = neonAuthUrl.endsWith("/auth")
        ? `${neonAuthUrl}/get-session`
        : `${neonAuthUrl}/auth/get-session`;

      const response = await fetch(targetUrl, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Cookie": `__Secure-neon-auth.session_token=${token}; neon-auth.session_token=${token}`
        },
      });

      if (!response.ok) {
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      const data = await response.json();
      
      console.log("[Neon Auth Validated Payload]:", JSON.stringify(data));

      // Extrai o ID do usuário da resposta do Neon Auth
      const userId =
        data?.user?.id ||
        data?.session?.userId ||
        data?.session?.user?.id ||
        data?.userId ||
        data?.id;

      if (!userId) {
        console.error("[Auth Failure] Payload sem userId válido:", data);
        return reply.code(401).send({ error: "Usuário não encontrado na sessão" });
      }

      req.auth = { userId, token };
    } catch (err) {
      console.error("[Auth Error]:", err);
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
