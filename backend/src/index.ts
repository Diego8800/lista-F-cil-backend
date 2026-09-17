import Fastify from "fastify";
import cors from "@fastify/cors";
import rateLimit from "@fastify/rate-limit";
import { env } from "./env.js";
import { runMigrations } from "./db/migrate.js";
import { registerAuth } from "./plugins/auth.js";
import { listRoutes } from "./routes/lists.js";
import { itemRoutes } from "./routes/items.js";
import { catalogRoutes } from "./routes/catalogs.js";
import { productRoutes } from "./routes/products.js";
import { reportRoutes } from "./routes/reports.js";
import { authRoutes } from "./routes/auth.js";

const app = Fastify({ logger: true });

await app.register(cors, { origin: true });

// Proteção contra abuso: 200 requisições/minuto por IP (Etapa 9 — hardening)
await app.register(rateLimit, {
  max: 200,
  timeWindow: "1 minute",
});

app.get("/health", async () => ({ status: "ok" }));

if (env.AUTO_MIGRATE) {
  await runMigrations();
}

registerAuth(app);

await app.register(authRoutes, { prefix: "/api" });

// Rotas protegidas: toda requisição exige sessão Neon Auth válida.
await app.register(async (api) => {
  api.decorateRequest("auth", null);
  api.addHook("preHandler", app.authenticate);
  await api.register(listRoutes);
  await api.register(itemRoutes);
  await api.register(catalogRoutes);
  await api.register(productRoutes);
  await api.register(reportRoutes);
}, { prefix: "/api" });

app.setErrorHandler((err, req, reply) => {
  req.log.error(err);
  const status = (err as { statusCode?: number }).statusCode ?? 500;
  reply
    .code(status)
    .send({ error: status >= 500 ? "Erro interno do servidor" : err.message });
});

try {
  await app.listen({ port: env.PORT, host: "0.0.0.0" });
} catch (err) {
  app.log.error(err);
  process.exit(1);
}
