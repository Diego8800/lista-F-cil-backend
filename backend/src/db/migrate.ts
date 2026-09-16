import { adminDb } from "./client.js";
import { migrations } from "./migrations.js";

export async function runMigrations(): Promise<void> {
  const sql = adminDb();

  await sql(`
    CREATE TABLE IF NOT EXISTS _migrations (
      name text PRIMARY KEY,
      applied_at timestamptz NOT NULL DEFAULT now()
    )
  `);

  for (const migration of migrations) {
    const already = await sql(
      `SELECT name FROM _migrations WHERE name = $1`,
      [migration.name],
    );
    if (already.length > 0) continue;

    console.log(`Aplicando migração ${migration.name}...`);
    for (const statement of migration.statements) {
      await sql(statement);
    }
    await sql(`INSERT INTO _migrations (name) VALUES ($1)`, [migration.name]);
    console.log(`Migração ${migration.name} aplicada.`);
  }
}

// Execução direta: npm run migrate
if (process.argv[1] && process.argv[1].endsWith("migrate.ts")) {
  runMigrations()
    .then(() => {
      console.log("Migrações concluídas.");
      process.exit(0);
    })
    .catch((err) => {
      console.error("Falha nas migrações:", err);
      process.exit(1);
    });
}
