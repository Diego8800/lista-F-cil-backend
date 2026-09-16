#!/usr/bin/env bash
# Setup do projeto Neon para o Lista Fácil (executar no seu ambiente local).
# Requisitos: Node.js >= 20
set -euo pipefail

echo "==> Instalando Neon CLI e autenticando"
npm i -g neon@latest && neon login

echo "==> Instalando skills e MCP do Neon"
neon skills -y
neon mcp -y

echo "==> Vinculando ao projeto hidden-morning-33501533 (branch production)"
neon link --project-id hidden-morning-33501533 --branch production -y

echo "==> Inicializando configuracao local (gera neon.config.ts com auth: true)"
neon config init

echo "==> Implantando configuracao no Neon (habilita Neon Auth)"
neon deploy

echo ""
echo "Concluido. Agora:"
echo "1. Console Neon -> Auth: copie a 'Auth endpoint' para NEON_AUTH_BASE_URL"
echo "2. Console Neon -> Connection Details: copie DATABASE_URL e DATABASE_AUTHENTICATED_URL para backend/.env"
echo "3. Execute: cd backend && npm install && npm run migrate"
echo "4. Faca o deploy no Railway configurando as mesmas variaveis de ambiente."
