#!/usr/bin/env bash
# Smoke test da API Lista Fácil.
# Uso: bash scripts/smoke-test.sh <BASE_URL> <JWT_TOKEN>
# Ex.:   bash scripts/smoke-test.sh http://localhost:8080 eyJhbGciOi...
set -euo pipefail

BASE="${1:?Informe a base URL (ex.: http://localhost:8080)}"
TOKEN="${2:?Informe o JWT da sessão Neon Auth}"
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

echo "==> 0. Health"
curl -fsS "$BASE/health"

echo; echo "==> 1. Categorias padrão (seed)"
curl -fsS -X POST -H "$AUTH" -H "$CT" "$BASE/categories/seed"

echo; echo "==> 2. Criar lista"
LIST=$(curl -fsS -X POST -H "$AUTH" -H "$CT" -d '{"name":"Semana Teste","budget_cents":20000}' "$BASE/lists")
echo "$LIST"
LIST_ID=$(echo "$LIST" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).id))")

echo; echo "==> 3. Categoria id"
CAT=$(curl -fsS -H "$AUTH" "$BASE/categories")
CAT_ID=$(echo "$CAT" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).categories[0].id))")
echo "category_id=$CAT_ID"

echo "==> 4. Adicionar item (preço anterior automático)"
ITEM=$(curl -fsS -X POST -H "$AUTH" -H "$CT" \
  -d "{\"product_name\":\"Arroz Teste\",\"category_id\":\"$CAT_ID\",\"quantity\":5,\"unit\":\"kg\",\"current_price_cents\":2500}" \
  "$BASE/lists/$LIST_ID/items")
echo "$ITEM"
ITEM_ID=$(echo "$ITEM" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).id))")

echo; echo "==> 5. Marcar como comprado"
curl -fsS -X PATCH -H "$AUTH" -H "$CT" -d '{"purchased":true}' "$BASE/lists/$LIST_ID/items/$ITEM_ID" > /dev/null

echo "==> 6. Finalizar lista"
curl -fsS -X POST -H "$AUTH" -H "$CT" -d '{}' "$BASE/lists/$LIST_ID/finish"

echo; echo "==> 7. Histórico do produto"
PRODUCT_ID=$(echo "$ITEM" | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>console.log(JSON.parse(d).product_id))")
curl -fsS -H "$AUTH" "$BASE/products/$PRODUCT_ID/history"

echo; echo "==> 8. Relatórios"
curl -fsS -H "$AUTH" "$BASE/reports/spent-by-list"; echo
curl -fsS -H "$AUTH" "$BASE/reports/savings"

echo; echo "SMOKE TEST OK — todas as etapas responderam com sucesso."
