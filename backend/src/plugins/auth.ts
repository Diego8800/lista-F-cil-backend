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
  const neonAuthUrl = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    // 1. Log das variáveis de ambiente e token recebido do app Android
    console.log("=== [DEBUG AUTH START] ===");
    console.log("NEON_AUTH_BASE_URL configurado:", neonAuthUrl);
    console.log("Token recebido do Android:", token);

    try {
      // Testamos a rota de sessão padrão do Better Auth / Neon Auth
      const targetUrl = neonAuthUrl.endsWith("/auth")
        ? `${neonAuthUrl}/get-session`
        : `${neonAuthUrl}/auth/get-session`;

      console.log("URL chamada no Neon Auth:", targetUrl);

      const response = await fetch(targetUrl, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${token}`
        }
      });

      console.log("Status HTTP do Neon Auth:", response.status);

      const rawText = await response.text();
      console.log("Resposta BRUTA (raw text) do Neon Auth:", rawText);

      if (!response.ok) {
        console.log("=== [DEBUG AUTH END - FAIL STATUS] ===");
        return reply.code(401).send({ error: "Sessão inválida ou expirada" });
      }

      let data: any = {};
      try {
        data = JSON.parse(rawText);
      } catch (e) {
        console.error("Falha ao parsear JSON do Neon Auth");
      }

      // Procura por qualquer campo que se pareça com o ID do usuário
      const userId =
        data?.user?.id ||
        data?.session?.userId ||
        data?.session?.user?.id ||
        data?.userId ||
        data?.id;

      console.log("UserId extraído:", userId);
      console.log("=== [DEBUG AUTH END] ===");

      if (!userId) {
        return reply.code(401).send({ error: "Usuário não encontrado na sessão" });
      }

      req.auth = { userId, token };
    } catch (err) {
      console.error("[DEBUG AUTH ERROR]:", err);
      console.log("=== [DEBUG AUTH END - EXCEPTION] ===");
      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
