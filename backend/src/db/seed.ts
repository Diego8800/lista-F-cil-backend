import { adminDb } from "./client.js";

/**
 * Seed: insere as categorias padrão para um usuário (ignorando duplicatas).
 * Uso: npm run seed -- <userId>
 * (Em produção o app chama POST /categories/seed após o login.)
 */
const userId = process.argv[2];
if (!userId) {
  console.error("Informe o userId: npm run seed -- <userId>");
  process.exit(1);
}

const DEFAULT_CATEGORIES = [
  "Hortifruti", "Açougue", "Laticínios", "Padaria", "Mercearia",
  "Bebidas", "Limpeza", "Higiene", "Congelados", "Outros",
];

const sql = adminDb();
for (const name of DEFAULT_CATEGORIES) {
  await sql(
    `INSERT INTO categories (user_id, name) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
    [userId, name],
  );
}
console.log(`Categorias padrão criadas para o usuário ${userId}.`);
process.exit(0);
