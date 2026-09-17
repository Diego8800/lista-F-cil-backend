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
  const neonAuthUrl = process.env.NEON_AUTH_BASE_URL || "";

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      const response = await fetch(`${neonAuthUrl}/get-session`, {
        headers: {
          "Authorization": `Bearer ${token}`,
          "Origin": "http://localhost:3000"
        }
      });

      if (!response.ok) {
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      const data = await response.json();
      
      // Log do payload para depuração no Railway
      console.log("[Neon Auth Response]:", JSON.stringify(data));

      // Mapeia todas as possíveis estruturas de ID retornadas pelo Neon Auth / Better Auth
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
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
