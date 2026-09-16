/**
 * Migração 002 — Row Level Security.
 * Requer o Neon Auth habilitado (função auth.user_id() disponível no banco).
 * Cada usuário enxerga e manipula exclusivamente os próprios dados.
 */

// Garante a existência do schema auth e da função auth.user_id()
const setupAuthSchema = [
  `CREATE SCHEMA IF NOT EXISTS auth`,
  `CREATE OR REPLACE FUNCTION auth.user_id() RETURNS uuid AS $$
     SELECT NULLIF(current_setting('request.jwt.claim.sub', true), '')::uuid;
   $$ LANGUAGE sql STABLE`,
];

const owned = (table: string) => [
  `ALTER TABLE ${table} ENABLE ROW LEVEL SECURITY`,
  `DROP POLICY IF EXISTS ${table}_owner_select ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_owner_insert ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_owner_update ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_owner_delete ON ${table}`,
  `CREATE POLICY ${table}_owner_select ON ${table} FOR SELECT USING (user_id = auth.user_id())`,
  `CREATE POLICY ${table}_owner_insert ON ${table} FOR INSERT WITH CHECK (user_id = auth.user_id())`,
  `CREATE POLICY ${table}_owner_update ON ${table} FOR UPDATE USING (user_id = auth.user_id()) WITH CHECK (user_id = auth.user_id())`,
  `CREATE POLICY ${table}_owner_delete ON ${table} FOR DELETE USING (user_id = auth.user_id())`,
];

const viaLists = (table: string) => [
  `ALTER TABLE ${table} ENABLE ROW LEVEL SECURITY`,
  `DROP POLICY IF EXISTS ${table}_via_list_select ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_via_list_insert ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_via_list_update ON ${table}`,
  `DROP POLICY IF EXISTS ${table}_via_list_delete ON ${table}`,
  `CREATE POLICY ${table}_via_list_select ON ${table} FOR SELECT
     USING (EXISTS (SELECT 1 FROM lists l WHERE l.id = list_id AND l.user_id = auth.user_id()))`,
  `CREATE POLICY ${table}_via_list_insert ON ${table} FOR INSERT
     WITH CHECK (EXISTS (SELECT 1 FROM lists l WHERE l.id = list_id AND l.user_id = auth.user_id()))`,
  `CREATE POLICY ${table}_via_list_update ON ${table} FOR UPDATE
     USING (EXISTS (SELECT 1 FROM lists l WHERE l.id = list_id AND l.user_id = auth.user_id()))
     WITH CHECK (EXISTS (SELECT 1 FROM lists l WHERE l.id = list_id AND l.user_id = auth.user_id()))`,
  `CREATE POLICY ${table}_via_list_delete ON ${table} FOR DELETE
     USING (EXISTS (SELECT 1 FROM lists l WHERE l.id = list_id AND l.user_id = auth.user_id()))`,
];

export const migration002 = {
  name: "002_rls",
  statements: [
    ...setupAuthSchema,
    ...owned("lists"),
    ...owned("products"),
    ...owned("categories"),
    ...owned("establishments"),
    ...viaLists("list_items"),
    ...owned("price_history"),
  ],
};
