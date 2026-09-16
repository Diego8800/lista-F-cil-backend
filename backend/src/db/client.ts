import { neon } from "@neondatabase/serverless";
import { dbUrl, usesRlsProxy } from "../env.js";

export type Sql = ReturnType<typeof neon>;

/**
 * Retorna um client SQL para o usuário autenticado.
 *
 * Quando configurada DATABASE_AUTHENTICATED_URL (RLS proxy do Neon), o JWT da
 * sessão é repassado ao banco em cada requisição e o Row Level Security do
 * PostgreSQL garante que o usuário só enxerga as próprias linhas — mesmo que
 * uma query esqueça o filtro de usuário, o banco bloqueia o acesso.
 */
export function db(authToken: string): Sql {
  if (usesRlsProxy) return neon(dbUrl, { authToken });
  return neon(dbUrl);
}

/** Client administrativo (migrações) — usa DATABASE_URL (owner), nunca o JWT do usuário. */
export function adminDb(): Sql {
  return neon(process.env.DATABASE_URL ?? dbUrl);
}
