# Lista Fácil — Aplicativo Android de Lista de Compras

Aplicativo completo de lista de compras com budget, histórico de preços, comparador
de custo proporcional e relatórios.

- **Android**: Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, Hilt, Navigation Compose
- **Backend**: Node.js, TypeScript, Fastify, API REST (deploy no Railway)
- **Banco**: Neon PostgreSQL com **Neon Auth** (Better Auth) e **Row Level Security**
- **Sem Room**: toda a persistência é no PostgreSQL via API REST

---

## Estrutura

```
ListaFacil/
├── app/                    # Aplicativo Android (Android Studio)
│   └── src/main/java/com/listafacil/app/
│       ├── core/           # Money (centavos), unidades de medida
│       ├── data/           # remote (Retrofit DTOs/APIs), local (DataStore+Keystore), repository
│       ├── domain/         # modelos + CompareProductsUseCase
│       ├── di/             # Hilt (Retrofit backend + Neon Auth, interceptor de token)
│       └── ui/             # tema, navegação, telas e ViewModels
└── backend/                # API REST (Node.js + TypeScript + Fastify)
    ├── src/
    │   ├── db/             # migrações (schema + RLS), client serverless
    │   ├── plugins/        # validação JWT (JWKS do Neon Auth)
    │   ├── routes/         # lists, items, catalogs, products, reports
    │   ├── services/       # finalização de lista em transação
    │   └── index.ts
    └── scripts/            # neon-setup.sh / .ps1 (Etapa 0 — Neon Auth)
```

---

## Passo a passo para rodar

### 1) Neon (Etapa 0) — executar no seu computador

```bash
bash backend/scripts/neon-setup.sh        # Linux/macOS
# ou
powershell -File backend/scripts/neon-setup.ps1   # Windows
```

Isso executa: `neon login` → `neon skills -y` → `neon mcp -y` →
`neon link --project-id hidden-morning-33501533 --branch production -y` →
`neon config init` → `neon deploy` (habilita o Neon Auth).

Depois, no **console do Neon**, copie:
- **Auth → Auth endpoint** → `NEON_AUTH_BASE_URL` (termina em `/api/auth/`)
- **Connection Details → DATABASE_URL** (URL administrativa/owner)
- **Connection Details → DATABASE_AUTHENTICATED_URL** (RLS proxy)

### 2) Backend (local)

```bash
cd backend
cp .env.example .env      # preencher as 3 variáveis acima
npm install
npm run migrate           # cria o schema e as policies de RLS
npm run dev               # http://localhost:8080/health
```

### 3) Backend (produção no Railway)

1. Suba a pasta `backend/` para um repositório Git e conecte ao Railway
   (o `railway.json` + `nixpacks.toml` fazem build e start automaticamente).
2. Configure as variáveis de ambiente no Railway:
   `DATABASE_URL`, `DATABASE_AUTHENTICATED_URL`, `NEON_AUTH_BASE_URL`, `PORT=8080`.
3. Anote a URL pública gerada (ex.: `https://listafacil-api.up.railway.app`).

### 4) Android

1. Abra a pasta `app/` no Android Studio (JDK 17, Android SDK 35).
2. Em `app/build.gradle.kts`, ajuste **dois** valores em `defaultConfig`:
   - `BACKEND_BASE_URL` → URL do Railway + `/api/`
   - `NEON_AUTH_BASE_URL` → Auth endpoint do Neon
3. Rode em um emulador/dispositivo (minSdk 26).

---

## Segurança

- Nenhuma credencial administrativa do Neon no APK — o app só fala com a API pública.
- O `user_id` usado nas queries **sempre vem do JWT validado** (JWKS) no backend,
  nunca do corpo da requisição.
- O backend repassa o JWT do usuário à conexão do Neon (`DATABASE_AUTHENTICATED_URL`),
  ativando o **Row Level Security** — o Postgres bloqueia acesso a dados de outros
  usuários mesmo se uma query esquecer o filtro.
- `price_history` é **append-only** (trigger impede UPDATE/DELETE).
- Token de sessão no app criptografado com AES/GCM + Android Keystore.

## Decisões de projeto (aprovadas)

1. Uma lista ativa por usuário por vez.
2. Preço anterior = último registro histórico do produto **na mesma unidade**.
3. Transferência de não comprados → nova lista `"<nome> (continuação)"`, mesmo budget,
   preço atual em branco, preço anterior preservado.
4. Economia = Σ max(0, preço pago − menor preço histórico) × quantidade.
5. Categorias padrão semeadas no primeiro acesso (idempotente).
6. Backend: Fastify + Zod; migrações SQL versionadas.
7. Isolamento: RLS nativa via JWT repassado + validação de sessão no backend.

## API (resumo)

Todas as rotas (exceto `/health`) exigem `Authorization: Bearer <JWT Neon Auth>`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/lists` | Criar lista (nome + budget em centavos) |
| GET | `/lists?status=active\|finished` | Listar listas |
| GET | `/lists/:id` | Lista + itens |
| PATCH | `/lists/:id` | Renomear/ajustar budget (só ativa) |
| POST | `/lists/:id/finish` | Finalizar (mode: delete\|transfer) |
| POST | `/lists/:id/items` | Adicionar produto (preço anterior automático) |
| PATCH/DELETE | `/lists/:id/items/:itemId` | Editar/remover item |
| GET | `/products/:id/history` | Histórico + estatísticas |
| CRUD | `/categories`, `/establishments` | Catálogos |
| POST | `/categories/seed` | Categorias padrão |
| GET | `/reports/*` | 7 relatórios |

## Testes

```bash
# Backend: migrações e seed
cd backend && npm run migrate && npm run seed -- <userId>

# Android (após sincronar o Gradle)
./gradlew test              # testes unitários (comparador, money)
./gradlew assembleDebug     # APK de debug
```

---

## Etapa 9 — Hardening (status)

Já implementado neste código:
- ✅ Rate limiting na API (`@fastify/rate-limit`: 200 req/min por IP)
- ✅ Validação JWT obrigatória em todas as rotas (JWKS) + RLS no banco
- ✅ `price_history` append-only (trigger no banco)
- ✅ Token de sessão criptografado (AES/GCM + Android Keystore)
- ✅ Build de release com minificação R8 + regras ProGuard
- ✅ Testes unitários (comparador, moeda) e teste instrumentado
- ✅ CI: `.github/workflows/backend.yml` (typecheck + build) e `android.yml` (test + assembleDebug)
- ✅ Tratamento de erros: snackbars, estados de loading, 401 → login
- ✅ Smoke test de ponta a ponta (`backend/scripts/smoke-test.sh`)

Checklist manual para produção (depende do seu ambiente):
- [ ] Rodar `bash backend/scripts/neon-setup.sh` (Etapa 0 — Neon Auth real)
- [ ] `cd backend && npm install && cp .env.example .env` (preencher 3 URLs do Neon) e `npm run migrate`
- [ ] Subir backend no Railway + configurar variáveis de ambiente
- [ ] Preencher `BACKEND_BASE_URL` e `NEON_AUTH_BASE_URL` em `app/build.gradle.kts`
- [ ] Buildar no Android Studio: `./gradlew test` → `assembleDebug` → testar em emulador
- [ ] Rodar smoke test: `bash backend/scripts/smoke-test.sh <URL> <JWT>`
- [ ] Gerar keystore de release: `keytool -genkey -v -keystore listafacil.jks ...`
- [ ] `./gradlew assembleRelease` e testar o APK assinado
- [ ] Confirmar no console Neon que o app fala apenas com a URL pública do backend
