import { randomUUID } from "node:crypto";
import { db } from "../db/client.js";

type FinishInput = {
  token: string;
  userId: string;
  listId: string;
  mode?: "delete" | "transfer";
  newListName?: string;
};

export type FinishResult =
  | { ok: true; finishedListId: string; newListId: string | null; nonPurchasedCount: number }
  | { ok: false; status: number; error: string; nonPurchasedCount?: number };

/**
 * Finalização da lista — executada em UMA transação:
 *  1. valida a lista ativa e os preços informados;
 *  2. grava o histórico de preços dos produtos comprados (preço atual + data);
 *  3. exclui ou transfere os não comprados para uma nova lista;
 *  4. encerra a lista com a data de finalização.
 */
export async function finishList(input: FinishInput): Promise<FinishResult> {
  const { token, userId, listId } = input;
  const sql = db(token);

  const listRows = await sql`
    SELECT id, name, budget_cents, status
    FROM lists
    WHERE id = ${listId} AND user_id = ${userId}`;
  if (listRows.length === 0) {
    return { ok: false, status: 404, error: "Lista não encontrada" };
  }
  const list = listRows[0] as { id: string; name: string; budget_cents: number; status: string };
  if (list.status !== "active") {
    return { ok: false, status: 409, error: "Esta lista já foi finalizada" };
  }

  const items = (await sql`
    SELECT id, product_id, current_price_cents, purchased
    FROM list_items
    WHERE list_id = ${listId}`) as Array<{
    id: string;
    product_id: string;
    current_price_cents: number | null;
    purchased: boolean;
  }>;

  const purchased = items.filter((i) => i.purchased);
  const notPurchased = items.filter((i) => !i.purchased);

  const missingPrice = purchased.filter((i) => i.current_price_cents == null);
  if (missingPrice.length > 0) {
    return {
      ok: false,
      status: 400,
      error: "Informe o preço atual de todos os produtos comprados antes de finalizar.",
    };
  }

  if (notPurchased.length > 0 && !input.mode) {
    return {
      ok: false,
      status: 422,
      error: "NON_PURCHASED",
      nonPurchasedCount: notPurchased.length,
    };
  }

  const historyInserts = purchased.map(
    (item) => sql`
      INSERT INTO price_history
        (user_id, product_id, list_id, price_cents, quantity, unit, establishment_id, recorded_at)
      SELECT ${userId}, i.product_id, i.list_id, i.current_price_cents, i.quantity, i.unit,
             i.establishment_id, now()
      FROM list_items i
      WHERE i.id = ${item.id}`,
  );

  let newListId: string | null = null;
  if (notPurchased.length > 0 && input.mode === "transfer") {
    newListId = randomUUID();
  }

  const statements = [];
  if (newListId) {
    const continuationName = input.newListName ?? `${list.name} (continuação)`;
    statements.push(sql`
      INSERT INTO lists (id, user_id, name, budget_cents)
      VALUES (${newListId}, ${userId}, ${continuationName}, ${list.budget_cents})`);
    statements.push(sql`
      INSERT INTO list_items
        (list_id, product_id, category_id, establishment_id, quantity, unit, previous_price_cents, note)
      SELECT ${newListId}, product_id, category_id, establishment_id, quantity, unit,
             previous_price_cents, note
      FROM list_items
      WHERE list_id = ${listId} AND purchased = false`);
  }
  if (input.mode === "delete") {
    statements.push(sql`
      DELETE FROM list_items
      WHERE list_id = ${listId} AND purchased = false`);
  }
  statements.push(...historyInserts);
  statements.push(sql`
    UPDATE lists
    SET status = 'finished', finished_at = now(), updated_at = now()
    WHERE id = ${listId}`);

  await sql.transaction(statements as never[]);

  return {
    ok: true,
    finishedListId: listId,
    newListId,
    nonPurchasedCount: notPurchased.length,
  };
}
