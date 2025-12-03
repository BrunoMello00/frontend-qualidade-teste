@echo off
echo ===============================================
echo  🚀 INICIANDO FRONTEND ANGULAR PARA TESTES
echo ===============================================
echo.

echo ✅ Mudando para o diretório do frontend...
cd /d "d:\Faculdade\Qualidade e Teste\frontend-qualidade-teste"

echo.
echo ✅ Verificando se node_modules existe...
if not exist "node_modules" (
    echo ⚠️ node_modules não encontrado. Executando npm install...
    npm install
    if %ERRORLEVEL% neq 0 (
        echo ❌ Erro ao executar npm install
        pause
        exit /b 1
    )
) else (
    echo ✅ node_modules já existe
)

echo.
echo ✅ Iniciando o servidor Angular...
echo 📍 URL: http://localhost:4200
echo 🛑 Para parar o servidor, pressione Ctrl+C
echo.

ng serve --port 4200 --host 0.0.0.0 --disable-host-check