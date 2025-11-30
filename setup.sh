#!/bin/bash

echo "🚀 Iniciando Projeto Full-Stack - Qualidade e Teste"
echo "=================================================="

echo "📦 Instalando dependências do frontend..."
npm install

echo "🔧 Compilando backend Java..."
cd backend-java
mvn clean compile
cd ..

echo "✅ Projeto configurado com sucesso!"
echo ""
echo "Comandos disponíveis:"
echo "- npm run dev              # Inicia apenas o frontend"
echo "- npm run backend:test     # Executa testes do backend"
echo "- npm run backend:test-report # Testes + relatório de cobertura"
echo "- npm run full:test        # Executa todos os testes (frontend + backend)"
echo "- npm run full:build       # Build completo (frontend + backend)"
echo "- npm run dev:fullstack    # Inicia frontend + backend simultaneamente"
echo ""
echo "🌐 Frontend estará disponível em: http://localhost:4200"
echo "🖥️  Backend estará disponível em: http://localhost:8080"
echo "📊 Relatório de cobertura: backend-java/target/site/jacoco/index.html"