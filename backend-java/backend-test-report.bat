@echo off
REM Script para rodar testes e gerar relatório JaCoCo (Windows)
echo Executando testes unitarios e gerando relatorio JaCoCo...
cd /d "%~dp0"
mvn test
IF %ERRORLEVEL% NEQ 0 (
  echo Erro durante mvn test. Verifique logs.
  exit /b %ERRORLEVEL%
)
echo Gerando relatorio JaCoCo (se configurado no pom)...
mvn jacoco:report
IF %ERRORLEVEL% NEQ 0 (
  echo Erro durante jacoco:report. Verifique logs.
  exit /b %ERRORLEVEL%
)
set REPORT=target\site\jacoco\index.html
if exist "%REPORT%" (
  echo Abrindo relatorio: %REPORT%
  start "" "%REPORT%"
) else (
  echo Relatorio JaCoCo nao encontrado em %REPORT%
)

pause
