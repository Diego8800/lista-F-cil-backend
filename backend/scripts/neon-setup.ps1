# Setup do projeto Neon para o Lista Fácil (PowerShell - executar no seu ambiente local)
$ErrorActionPreference = "Stop"

Write-Host "==> Instalando Neon CLI e autenticando"
npm i -g neon@latest
neon login

Write-Host "==> Instalando skills e MCP do Neon"
neon skills -y
neon mcp -y

Write-Host "==> Vinculando ao projeto hidden-morning-33501533 (branch production)"
neon link --project-id hidden-morning-33501533 --branch production -y

Write-Host "==> Inicializando configuracao local"
neon config init

Write-Host "==> Implantando configuracao no Neon (habilita Neon Auth)"
neon deploy

Write-Host ""
Write-Host "Concluido. Agora:"
Write-Host "1. Console Neon -> Auth: copie a 'Auth endpoint' para NEON_AUTH_BASE_URL"
Write-Host "2. Console Neon -> Connection Details: copie DATABASE_URL e DATABASE_AUTHENTICATED_URL para backend/.env"
Write-Host "3. Execute: cd backend; npm install; npm run migrate"
Write-Host "4. Faca o deploy no Railway configurando as mesmas variaveis de ambiente."
