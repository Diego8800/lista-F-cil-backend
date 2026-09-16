import "dotenv/config";
import { z } from "zod";

const schema = z.object({
  PORT: z.coerce.number().int().positive().default(8080),
  DATABASE_AUTHENTICATED_URL: z.string().min(1).optional(),
  DATABASE_URL: z.string().min(1).optional(),
  NEON_AUTH_BASE_URL: z.string().min(1),
  AUTO_MIGRATE: z.coerce.boolean().default(true),
  LOG_SQL: z.coerce.boolean().default(false),
});

const parsed = schema.safeParse(process.env);
if (!parsed.success) {
  console.error("Variáveis de ambiente inválidas:", parsed.error.flatten().fieldErrors);
  process.exit(1);
}
export const env = parsed.data;

if (!env.DATABASE_AUTHENTICATED_URL && !env.DATABASE_URL) {
  console.error("DATABASE_AUTHENTICATED_URL ou DATABASE_URL é obrigatória");
  process.exit(1);
}

/** URL usada nas requisições com usuário autenticado (RLS proxy quando disponível). */
export const dbUrl = (env.DATABASE_AUTHENTICATED_URL ?? env.DATABASE_URL)!;
/** true quando o backend repassa o JWT ao Neon (RLS nativa por usuário). */
export const usesRlsProxy = Boolean(env.DATABASE_AUTHENTICATED_URL);

const authBase = env.NEON_AUTH_BASE_URL.endsWith("/")
  ? env.NEON_AUTH_BASE_URL
  : `${env.NEON_AUTH_BASE_URL}/`;

/** JWKS público do Neon Auth (Better Auth) para validação do JWT de sessão. */
export const jwksUrl = new URL(".well-known/jwks.json", authBase).toString();
