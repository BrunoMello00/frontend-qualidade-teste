# Sistema de Vendas e Estoque - Startup Completo (Windows PowerShell)
# Versão adaptada do script bash para Windows

Write-Host "========================================" -ForegroundColor Green
Write-Host "    SISTEMA DE VENDAS E ESTOQUE" -ForegroundColor Green
Write-Host "  Inicialização Completa com Dados" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host

Write-Host "[INFO] Verificando pré-requisitos..." -ForegroundColor Yellow

# Função para matar processos nas portas específicas
function Stop-PortProcesses {
    Write-Host "[INFO] Parando processos anteriores..." -ForegroundColor Yellow
    
    # Matar processo na porta 8080 (backend)
    try {
        $backendProcess = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
        if ($backendProcess) {
            $pid = $backendProcess.OwningProcess
            Write-Host "[INFO] Matando processo backend na porta 8080 (PID: $pid)..." -ForegroundColor Yellow
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        }
    } catch {}
    
    # Matar processo na porta 4200 (frontend)
    try {
        $frontendProcess = Get-NetTCPConnection -LocalPort 4200 -ErrorAction SilentlyContinue
        if ($frontendProcess) {
            $pid = $frontendProcess.OwningProcess
            Write-Host "[INFO] Matando processo frontend na porta 4200 (PID: $pid)..." -ForegroundColor Yellow
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        }
    } catch {}
    
    # Matar processos relacionados
    Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*node*"} | Stop-Process -Force -ErrorAction SilentlyContinue
    
    Write-Host "[OK] Processos anteriores finalizados!" -ForegroundColor Green
    Start-Sleep -Seconds 2
}

# Verificar se Java está instalado
function Test-Java {
    try {
        $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
        Write-Host "[OK] Java encontrado: $javaVersion" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[ERRO] Java não está instalado!" -ForegroundColor Red
        Write-Host "[INFO] Instale o Java e tente novamente." -ForegroundColor Yellow
        return $false
    }
}

# Verificar se Node.js está instalado
function Test-Node {
    try {
        $nodeVersion = node --version
        Write-Host "[OK] Node.js encontrado: $nodeVersion" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[ERRO] Node.js não está instalado!" -ForegroundColor Red
        Write-Host "[INFO] Instale o Node.js e tente novamente." -ForegroundColor Yellow
        return $false
    }
}

# Verificar se Maven está instalado
function Test-Maven {
    try {
        $mvnVersion = mvn --version | Select-Object -First 1
        Write-Host "[OK] Maven encontrado: $mvnVersion" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[ERRO] Maven não está instalado!" -ForegroundColor Red
        Write-Host "[INFO] Instale o Maven e tente novamente." -ForegroundColor Yellow
        return $false
    }
}

# Matar processos existentes
Stop-PortProcesses

# Verificar dependências
if (-not (Test-Java)) { exit 1 }
if (-not (Test-Node)) { exit 1 }
if (-not (Test-Maven)) { exit 1 }

Write-Host
Write-Host "[1/4] Iniciando Backend (Spring Boot)..." -ForegroundColor Blue

# Detectar diretório do backend
$backendDir = "backend-java"
if (-not (Test-Path $backendDir)) {
    $backendDir = "backend"
    if (-not (Test-Path $backendDir)) {
        Write-Host "[ERRO] Diretório de backend não encontrado! (esperado: 'backend-java' ou 'backend')" -ForegroundColor Red
        exit 1
    }
}

# Iniciar backend em nova janela PowerShell
$backendPath = Join-Path $PWD.Path $backendDir
Write-Host "[INFO] Iniciando backend em: $backendPath" -ForegroundColor Yellow

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; Write-Host 'Iniciando Backend Spring Boot...' -ForegroundColor Green; mvn spring-boot:run -q"

Write-Host "[OK] Backend iniciado em nova janela!" -ForegroundColor Green

Write-Host "[2/4] Aguardando Backend inicializar..." -ForegroundColor Blue
Write-Host "[INFO] Aguardando backend inicializar (60 segundos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# Testar se backend está funcionando
$backendUp = $false
for ($i = 1; $i -le 10; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $backendUp = $true
            break
        }
    } catch {}
    Write-Host "[INFO] Aguardando backend... ($i/10)" -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}

if (-not $backendUp) {
    Write-Host "[ERRO] Backend não iniciou corretamente!" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Backend está funcionando!" -ForegroundColor Green

Write-Host
Write-Host "[3/4] Iniciando Frontend (Angular)..." -ForegroundColor Blue

# Detectar diretório do frontend
$frontendDir = "."
if (Test-Path "frontend") {
    $frontendDir = "frontend"
} elseif (-not (Test-Path "package.json")) {
    Write-Host "[ERRO] Diretório de frontend não encontrado! (esperado: 'frontend' ou package.json na raiz)" -ForegroundColor Red
    exit 1
}

# Instalar dependências do frontend se necessário
$nodeModulesPath = Join-Path $frontendDir "node_modules"
if (-not (Test-Path $nodeModulesPath)) {
    Write-Host "[INFO] Instalando dependências do frontend..." -ForegroundColor Yellow
    Set-Location $frontendDir
    npm install
    Set-Location ..
}

# Iniciar frontend em nova janela
$frontendPath = Join-Path $PWD.Path $frontendDir
Write-Host "[INFO] Iniciando frontend em: $frontendPath" -ForegroundColor Yellow

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; Write-Host 'Iniciando Frontend Angular...' -ForegroundColor Green; npm start"

Write-Host "[OK] Frontend iniciado em nova janela!" -ForegroundColor Green

Write-Host "[4/4] Aguardando Frontend inicializar..." -ForegroundColor Blue
Write-Host "[INFO] Aguardando frontend inicializar (30 segundos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Testar se frontend está funcionando
$frontendUp = $false
for ($i = 1; $i -le 20; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:4200" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $frontendUp = $true
            break
        }
    } catch {}
    Write-Host "[INFO] Aguardando frontend... ($i/20)" -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}

if (-not $frontendUp) {
    Write-Host "[ERRO] Frontend não iniciou corretamente!" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Frontend está funcionando!" -ForegroundColor Green

Write-Host
Write-Host "========================================" -ForegroundColor Green
Write-Host "     SISTEMA INICIADO COM SUCESSO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host
Write-Host "🌐 ACESSO AO SISTEMA:" -ForegroundColor Blue
Write-Host "Frontend: http://localhost:4200" -ForegroundColor Blue
Write-Host "Backend:  http://localhost:8080" -ForegroundColor Blue
Write-Host "API Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor Blue
Write-Host "H2 Console: http://localhost:8080/h2-console" -ForegroundColor Blue

Write-Host
Write-Host "🔐 USUÁRIOS PARA TESTE:" -ForegroundColor Green
Write-Host "  ADMIN:      admin@sistema.com      / password" -ForegroundColor Yellow
Write-Host "  VENDEDOR:   vendedor@sistema.com   / password" -ForegroundColor Yellow
Write-Host "  ESTOQUISTA: estoquista@sistema.com / password" -ForegroundColor Yellow

$openBrowser = Read-Host "`nDeseja abrir o sistema no navegador? (s/n)"
if ($openBrowser -eq "s" -or $openBrowser -eq "S") {
    Write-Host "[INFO] Abrindo navegador..." -ForegroundColor Yellow
    Start-Process "http://localhost:4200"
}

Write-Host
Write-Host "🎉 SISTEMA PRONTO!" -ForegroundColor Green
Write-Host "Pressione qualquer tecla para fechar..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")