import type { FastifyInstance } from "fastify";

export async function authRoutes(app: FastifyInstance) {
  const rawBase = (process.env.NEON_AUTH_BASE_URL || "").replace(/\/$/, "");
  const baseUrl = rawBase.endsWith("/auth") ? rawBase.slice(0, -5) : rawBase;

  app.post("/auth/sign-up/email", async (req, reply) => {
    const res = await fetch(`${baseUrl}/auth/sign-up/email`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body),
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });

  app.post("/auth/sign-in/email", async (req, reply) => {
    const res = await fetch(`${baseUrl}/auth/sign-in/email`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body),
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });

  app.post("/auth/sign-out", async (req, reply) => {
    const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
    const res = await fetch(`${baseUrl}/auth/sign-out`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Cookie": `__Secure-neon-auth.session_token=${token}; neon-auth.session_token=${token}`,
      },
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });

  app.post("/auth/request-password-reset", async (req, reply) => {
    const res = await fetch(`${baseUrl}/auth/request-password-reset`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body),
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });

  app.post("/auth/change-password", async (req, reply) => {
    const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
    const res = await fetch(`${baseUrl}/auth/change-password`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: JSON.stringify(req.body),
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });

  app.post("/auth/update-user", async (req, reply) => {
    const token = (req.headers.authorization || "").replace("Bearer ", "").trim();
    const res = await fetch(`${baseUrl}/auth/update-user`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: JSON.stringify(req.body),
    });
    const data = await res.json();
    return reply.code(res.status).send(data);
  });
}
