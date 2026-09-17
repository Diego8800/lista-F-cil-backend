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
  // Limpa barras e sufixo /auth duplicado no final da URL base
  const rawBase = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");
  const baseUrl = rawBase.endsWith("/auth") ? rawBase.slice(0, -5) : rawBase;

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const fullToken = header.slice("Bearer ".length).trim();
    
    // Extrai o token de sessão base (parte antes do primeiro ponto, se houver)
    const rawSessionToken = fullToken.split(".")[0];

    try {
      const targetUrl = `${baseUrl}/auth/get-session`;

      // Tenta validar no Neon Auth passando os formatos aceitos de cookie/bearer
      const response = await fetch(targetUrl, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${rawSessionToken}`,
          "Cookie": `__Secure-neon-auth.session_token=${fullToken}; neon-auth.session_token=${rawSessionToken}`
        },
      });

      if (!response.ok) {
        console.error(`[Auth Failed] Status: ${response.status} URL: ${targetUrl}`);
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      const data = await response.json();
      console.log("[Neon Auth Decoded Payload]:", JSON.stringify(data));

      // Extrai o ID do usuário retornado pelo Neon Auth
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

      req.auth = { userId, token: fullToken };
    } catch (err) {
      console.error("[Auth Error]:", err);
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
