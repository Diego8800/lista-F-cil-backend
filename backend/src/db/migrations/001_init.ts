/**
 * Migração 001 — schema de domínio do Lista Fácil.
 * Dinheiro é armazenado em centavos (bigint). Sem tabela de usuários própria:
 * usamos a tabela "user" gerenciada pelo Neon Auth (Better Auth).
 */
export const migration001 = {
  name: "001_init",
  statements: [
    // Garante que a tabela "user" do Neon Auth exista antes das chaves estrangeiras
    `CREATE TABLE IF NOT EXISTS "user" (
      id uuid PRIMARY KEY DEFAULT gen_random_uuid()
    )`,

    `CREATE TABLE IF NOT EXISTS categories (
      id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     uuid NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
      name        text NOT NULL,
      created_at  timestamptz NOT NULL DEFAULT now()
    )`,
    `CREATE UNIQUE INDEX IF NOT EXISTS categories_user_name_unique ON categories (user_id, lower(name))`,

    `CREATE TABLE IF NOT EXISTS products (
      id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     uuid NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
      name        text NOT NULL,
      created_at  timestamptz NOT NULL DEFAULT now()
    )`,
    `CREATE UNIQUE INDEX IF NOT EXISTS products_user_name_unique ON products (user_id, lower(name))`,

    `CREATE TABLE IF NOT EXISTS establishments (
      id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     uuid NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
      name        text NOT NULL,
      created_at  timestamptz NOT NULL DEFAULT now()
    )`,
    `CREATE UNIQUE INDEX IF NOT EXISTS establishments_user_name_unique ON establishments (user_id, lower(name))`,

    `CREATE TABLE IF NOT EXISTS lists (
      id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     uuid NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
      name        text NOT NULL,
      budget_cents bigint NOT NULL CHECK (budget_cents >= 0),
      status      text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'finished')),
      finished_at timestamptz,
      created_at  timestamptz NOT NULL DEFAULT now(),
      updated_at  timestamptz NOT NULL DEFAULT now()
    )`,

    `CREATE TABLE IF NOT EXISTS list_items (
      id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      list_id          uuid NOT NULL REFERENCES lists(id) ON DELETE CASCADE,
      product_id       uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
      category_id      uuid NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
      establishment_id uuid REFERENCES establishments(id) ON DELETE SET NULL,
      quantity         numeric(12,3) NOT NULL CHECK (quantity > 0),
      unit             text NOT NULL CHECK (unit IN ('kg', 'g', 'L', 'ml', 'un')),
      previous_price_cents bigint CHECK (previous_price_cents IS NULL OR previous_price_cents >= 0),
      current_price_cents  bigint CHECK (current_price_cents IS NULL OR current_price_cents >= 0),
      note             text,
      purchased        boolean NOT NULL DEFAULT false,
      created_at       timestamptz NOT NULL DEFAULT now(),
      updated_at       timestamptz NOT NULL DEFAULT now(),
      CONSTRAINT list_items_product_unique UNIQUE (list_id, product_id)
    )`,

    `CREATE TABLE IF NOT EXISTS price_history (
      id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id          uuid NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
      product_id       uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
      list_id          uuid REFERENCES lists(id) ON DELETE SET NULL,
      establishment_id uuid REFERENCES establishments(id) ON DELETE SET NULL,
      price_cents      bigint NOT NULL CHECK (price_cents >= 0),
      quantity         numeric(12,3) NOT NULL CHECK (quantity > 0),
      unit             text NOT NULL,
      recorded_at      timestamptz NOT NULL DEFAULT now()
    )`,

    `CREATE INDEX IF NOT EXISTS idx_lists_user_status ON lists (user_id, status, created_at DESC)`,
    `CREATE INDEX IF NOT EXISTS idx_list_items_list ON list_items (list_id)`,
    `CREATE INDEX IF NOT EXISTS idx_price_history_product ON price_history (product_id, recorded_at DESC)`,
    `CREATE INDEX IF NOT EXISTS idx_price_history_user_time ON price_history (user_id, recorded_at DESC)`,

    // price_history é append-only: nunca atualizada nem excluída pela aplicação.
    `CREATE OR REPLACE FUNCTION price_history_immutable() RETURNS trigger
     LANGUAGE plpgsql AS $$
     BEGIN
       RAISE EXCEPTION 'price_history é append-only e não pode ser alterada';
     END
     $$`,
    `DROP TRIGGER IF EXISTS trg_price_history_immutable ON price_history`,
    `CREATE TRIGGER trg_price_history_immutable
       BEFORE UPDATE OR DELETE ON price_history
       FOR EACH ROW EXECUTE FUNCTION price_history_immutable()`,
  ],
};
