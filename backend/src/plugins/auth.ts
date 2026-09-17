import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import jwt from "jsonwebtoken";
import jwksClient from "jwks-rsa";

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
  const baseUrl = rawBase.endsWith("/auth") ? rawBase : `${rawBase}/auth`;

  // URL do JWKS para obter as chaves públicas do Neon Auth
  const jwksUri = process.env.JWKS_URL || `${baseUrl}/.well-known/jwks.json`;

  const client = jwksClient({
    jwksUri,
    cache: true,
    rateLimit: true,
    jwksRequestsPerMinute: 10,
  });

  function getKey(header: jwt.JwtHeader, callback: jwt.SigningKeyCallback) {
    if (!header.kid) {
      return callback(new Error("Token JWT sem kid no cabeçalho"));
    }
    client.getSigningKey(header.kid, (err, key) => {
      if (err) {
        return callback(err);
      }
      const signingKey = key?.getPublicKey();
      callback(null, signingKey);
    });
  }

  app.decorate("authenticate", async (req: FastifyRequest, reply: FastifyReply) => {
    const header = req.headers.authorization;
    if (!header || !header.startsWith("Bearer ")) {
      return reply.code(401).send({ error: "Token de autenticação ausente" });
    }

    const token = header.slice("Bearer ".length).trim();

    try {
      // 1. Tenta validar via JWT assinado com a chave do JWKS
      const decoded = await new Promise<jwt.JwtPayload>((resolve, reject) => {
        jwt.verify(token, getKey, { algorithms: ["RS256", "ES256"] }, (err, decoded) => {
          if (err || !decoded) return reject(err);
          resolve(decoded as jwt.JwtPayload);
        });
      });

      const userId = decoded.sub || decoded.userId || decoded.id;

      if (!userId) {
        return reply.code(401).send({ error: "Token não contém ID de usuário válido" });
      }

      req.auth = { userId, token };
    } catch (jwtError) {
      // 2. Se o token não for um JWT (ex: Session Token opaco), tenta validação direta
      try {
        const response = await fetch(`${baseUrl}/get-session`, {
          method: "GET",
          headers: {
            "Authorization": `Bearer ${token}`,
            "Cookie": `__Secure-neon-auth.session_token=${token}; neon-auth.session_token=${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          const userId = data?.user?.id || data?.session?.userId || data?.userId;
          if (userId) {
            req.auth = { userId, token };
            return;
          }
        }
      } catch (fetchErr) {
        // Ignora erro do fallback
      }

      return reply.code(401).send({ error: "Sessão inválida ou expirada" });
    }
  });
}
