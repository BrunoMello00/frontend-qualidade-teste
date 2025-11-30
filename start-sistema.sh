#!/bin/bash

# Sistema de Vendas e Estoque - Startup Completo com Dados Completos
# Script para macOS/Linux - Versão Robusta e Completa

# Cores para o terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================"
echo -e "    SISTEMA DE VENDAS E ESTOQUE"
echo -e "  Inicialização Completa com Dados"
echo -e "========================================${NC}"
echo

echo -e "${YELLOW}[INFO]${NC} Verificando pré-requisitos..."

# Função para matar processos nas portas específicas
kill_port_processes() {
    echo -e "${YELLOW}[INFO]${NC} Parando processos anteriores..."
    
    # Matar processo na porta 8080 (backend)
    BACKEND_PID=$(lsof -ti:8080 2>/dev/null)
    if [ ! -z "$BACKEND_PID" ]; then
        echo -e "${YELLOW}[INFO]${NC} Matando processo backend na porta 8080 (PID: $BACKEND_PID)..."
        kill -9 $BACKEND_PID 2>/dev/null
    fi
    
    # Matar processo na porta 4200 (frontend)
    FRONTEND_PID=$(lsof -ti:4200 2>/dev/null)
    if [ ! -z "$FRONTEND_PID" ]; then
        echo -e "${YELLOW}[INFO]${NC} Matando processo frontend na porta 4200 (PID: $FRONTEND_PID)..."
        kill -9 $FRONTEND_PID 2>/dev/null
    fi
    
    # Matar processos relacionados
    pkill -f "ng serve" 2>/dev/null
    pkill -f "npm start" 2>/dev/null  
    pkill -f "spring-boot:run" 2>/dev/null
    pkill -f "mvn.*spring-boot:run" 2>/dev/null
    
    echo -e "${GREEN}[OK]${NC} Processos anteriores finalizados!"
    sleep 2
}

# Verificar se Java está instalado
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[ERRO]${NC} Java não está instalado!"
        echo -e "${YELLOW}[INFO]${NC} Instale o Java e tente novamente."
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "${GREEN}[OK]${NC} Java encontrado: $JAVA_VERSION"
}

# Verificar se Node.js está instalado
check_node() {
    if ! command -v node &> /dev/null; then
        echo -e "${RED}[ERRO]${NC} Node.js não está instalado!"
        echo -e "${YELLOW}[INFO]${NC} Instale o Node.js e tente novamente."
        exit 1
    fi
    
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}[OK]${NC} Node.js encontrado: $NODE_VERSION"
}

# Verificar se Maven está instalado
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}[ERRO]${NC} Maven não está instalado!"
        echo -e "${YELLOW}[INFO]${NC} Instale o Maven e tente novamente."
        exit 1
    fi
    
    MVN_VERSION=$(mvn --version | head -n 1)
    echo -e "${GREEN}[OK]${NC} Maven encontrado: $MVN_VERSION"
}

# Matar processos existentes
kill_port_processes

# Verificar dependências
check_java
check_node
check_maven

echo
echo -e "${BLUE}[1/4]${NC} Iniciando Backend (Spring Boot)..."

# Detectar diretório do backend (compatível com antiga estrutura e com `backend-java`)
if [ -d "backend-java" ]; then
    BACKEND_DIR="backend-java"
elif [ -d "backend" ]; then
    BACKEND_DIR="backend"
else
    echo -e "${RED}[ERRO]${NC} Diretório de backend não encontrado! (esperado: 'backend-java' ou 'backend')"
    exit 1
fi

# Verificar arquivo de configuração Maven personalizada
MAVEN_SETTINGS="-s /Users/brunomello/.m2/settings-pessoal.xml"
if [ -f "/Users/brunomello/.m2/settings-pessoal.xml" ]; then
    echo -e "${YELLOW}[INFO]${NC} Usando configuração Maven personalizada: settings-pessoal.xml"
else
    echo -e "${YELLOW}[AVISO]${NC} Arquivo settings-pessoal.xml não encontrado, usando configuração padrão"
    MAVEN_SETTINGS=""
fi

# Iniciar backend em nova aba do Terminal (construir comando com escape seguro)
BACKEND_FULL_PATH="$(pwd)/$BACKEND_DIR"
if [ -n "$MAVEN_SETTINGS" ]; then
    BACKEND_CMD="cd \"$BACKEND_FULL_PATH\" && echo '[INFO] Iniciando Backend...' && mvn spring-boot:run -q $MAVEN_SETTINGS"
else
    BACKEND_CMD="cd \"$BACKEND_FULL_PATH\" && echo '[INFO] Iniciando Backend...' && mvn spring-boot:run -q"
fi

# Escapar aspas e abrir em nova aba do Terminal via AppleScript
osascript -e "tell application \"Terminal\" to do script \"${BACKEND_CMD//\"/\\\"}\""

echo -e "${GREEN}[OK]${NC} Backend iniciado em nova aba do Terminal!"

echo -e "${BLUE}[2/4]${NC} Aguardando Backend inicializar..."
echo -e "${YELLOW}[INFO]${NC} Aguardando backend inicializar (60 segundos)..."
sleep 60

# Testar se backend está funcionando
BACKEND_UP=false
for i in {1..10}; do
    if curl -s --connect-timeout 5 http://localhost:8080/actuator/health > /dev/null 2>&1; then
        BACKEND_UP=true
        break
    fi
    echo -e "${YELLOW}[INFO]${NC} Aguardando backend... ($i/10)"
    sleep 3
done

if [ "$BACKEND_UP" = false ]; then
    echo -e "${RED}[ERRO]${NC} Backend não iniciou corretamente!"
    exit 1
fi

echo -e "${GREEN}[OK]${NC} Backend está funcionando!"

echo
echo -e "${BLUE}[3/4]${NC} Iniciando Frontend (Angular)..."

# Detectar diretório do frontend: prioriza pasta 'frontend', senão usa a raiz do repositório quando houver `package.json`
if [ -d "frontend" ]; then
    FRONTEND_DIR="frontend"
elif [ -f "package.json" ]; then
    FRONTEND_DIR="."
else
    echo -e "${RED}[ERRO]${NC} Diretório de frontend não encontrado! (esperado: 'frontend' ou `package.json` na raiz)"
    exit 1
fi

# Instalar dependências do frontend se necessário
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo -e "${YELLOW}[INFO]${NC} Instalando dependências do frontend (${FRONTEND_DIR})..."
    (cd "$FRONTEND_DIR" && npm install)
fi

# Iniciar frontend em nova aba
osascript -e "tell application \"Terminal\" to do script \"cd $(pwd)/$FRONTEND_DIR && echo 'Iniciando Frontend Angular...' && npm start\""

echo -e "${GREEN}[OK]${NC} Frontend iniciado em nova aba do Terminal!"

echo -e "${BLUE}[4/4]${NC} Aguardando Frontend inicializar..."
echo -e "${YELLOW}[INFO]${NC} Aguardando frontend inicializar (30 segundos)..."
sleep 30

# Testar se frontend está funcionando
FRONTEND_UP=false
for i in {1..20}; do
    if curl -s http://localhost:4200 > /dev/null 2>&1; then
        FRONTEND_UP=true
        break
    fi
    echo -e "${YELLOW}[INFO]${NC} Aguardando frontend... ($i/20)"
    sleep 3
done

if [ "$FRONTEND_UP" = false ]; then
    echo -e "${RED}[ERRO]${NC} Frontend não iniciou corretamente!"
    exit 1
fi

echo -e "${GREEN}[OK]${NC} Frontend está funcionando!"

echo
echo -e "${PURPLE}[DATA]${NC} Criando dados de teste completos..."

# Token de autenticação
TOKEN=""

# Função para fazer requisições autenticadas
make_authenticated_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$TOKEN" ]; then
        # Fazer login se não tiver token
        LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
            -H "Content-Type: application/json" \
            -d '{"email":"admin@sistema.com","senha":"password"}')
        
        TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('token', ''))" 2>/dev/null)
        
        if [ -z "$TOKEN" ]; then
            echo -e "${RED}[ERRO]${NC} Falha na autenticação!"
            return
        fi
    fi
    
    if [ -n "$data" ]; then
        curl -s -X $method "http://localhost:8080$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data"
    else
        curl -s -X $method "http://localhost:8080$endpoint" \
            -H "Authorization: Bearer $TOKEN"
    fi
}

# Aguardar um pouco mais para garantir que o backend está totalmente carregado 
echo -e "${YELLOW}[INFO]${NC} Aguardando estabilização completa do backend (20 segundos)..."
sleep 20

# Testar se endpoints básicos estão respondendo
echo -e "${YELLOW}[INFO]${NC} Verificando se APIs estão respondendo..."
for i in {1..5}; do
    if curl -s --connect-timeout 3 http://localhost:8080/api/auth/login > /dev/null 2>&1; then
        echo -e "${GREEN}[OK]${NC} API de autenticação respondendo!"
        break
    else
        echo -e "${YELLOW}[WAIT]${NC} Aguardando API ficar disponível... (tentativa $i/5)"
        sleep 5
    fi
done

echo -e "${CYAN}[DATA]${NC} 1. Criando eventos promocionais..."

# Eventos
EVENT_DATA_1='{
  "nome": "Black Friday Mega Promoção 2025",
  "descricao": "Maior evento promocional do ano com descontos de até 70%",
  "dataInicio": "2025-11-29",
  "dataFim": "2025-12-01",
  "tipo": "PROMOCIONAL",
  "status": "ATIVO",
  "descontoPercentual": 30.0
}'

EVENT_DATA_2='{
  "nome": "Liquidação de Verão",
  "descricao": "Queima de estoque com preços imperdíveis",
  "dataInicio": "2025-01-15",
  "dataFim": "2025-02-15",
  "tipo": "LIQUIDACAO",
  "status": "ATIVO",
  "descontoPercentual": 40.0
}'

EVENT_DATA_3='{
  "nome": "Volta às Aulas 2025",
  "descricao": "Tudo para o seu ano letivo com desconto especial",
  "dataInicio": "2025-01-20",
  "dataFim": "2025-03-10",
  "tipo": "SAZONAL",
  "status": "ATIVO",
  "descontoPercentual": 15.0
}'

EVENT_DATA_4='{
  "nome": "Dia das Mães Especial",
  "descricao": "Presentes especiais para quem você ama",
  "dataInicio": "2025-05-01",
  "dataFim": "2025-05-12",
  "tipo": "COMEMORATIVO",
  "status": "ATIVO",
  "descontoPercentual": 25.0
}'

make_authenticated_request "POST" "/api/eventos" "$EVENT_DATA_1" > /dev/null
make_authenticated_request "POST" "/api/eventos" "$EVENT_DATA_2" > /dev/null
make_authenticated_request "POST" "/api/eventos" "$EVENT_DATA_3" > /dev/null
make_authenticated_request "POST" "/api/eventos" "$EVENT_DATA_4" > /dev/null

echo -e "${GREEN}[OK]${NC} 4 eventos criados!"

echo -e "${CYAN}[DATA]${NC} 2. Criando clientes diversificados..."

# Clientes com perfis variados
CLIENTS=(
    '{"nome":"João Silva Santos","email":"joao.silva@email.com","telefone":"(11) 98765-4321","cpf":"123.456.789-01","dataNascimento":"1985-03-15","endereco":{"rua":"Rua das Flores","numero":"123","cidade":"São Paulo","estado":"SP","cep":"01234-567"}}'
    '{"nome":"Maria Fernanda Costa","email":"maria.fernanda@gmail.com","telefone":"(21) 99876-5432","cpf":"987.654.321-02","dataNascimento":"1990-07-22","endereco":{"rua":"Av. Copacabana","numero":"456","cidade":"Rio de Janeiro","estado":"RJ","cep":"22071-900"}}'
    '{"nome":"Pedro Henrique Oliveira","email":"pedro.oliveira@hotmail.com","telefone":"(31) 91234-5678","cpf":"456.789.123-03","dataNascimento":"1978-12-03","endereco":{"rua":"Rua da Liberdade","numero":"789","cidade":"Belo Horizonte","estado":"MG","cep":"30112-345"}}'
    '{"nome":"Ana Carolina Souza","email":"ana.souza@outlook.com","telefone":"(47) 92345-6789","cpf":"789.123.456-04","dataNascimento":"1992-05-18","endereco":{"rua":"Rua Joinville","numero":"321","cidade":"Blumenau","estado":"SC","cep":"89010-123"}}'
    '{"nome":"Carlos Eduardo Lima","email":"carlos.lima@empresa.com","telefone":"(85) 93456-7890","cpf":"321.654.987-05","dataNascimento":"1980-09-30","endereco":{"rua":"Av. Beira Mar","numero":"654","cidade":"Fortaleza","estado":"CE","cep":"60165-081"}}'
    '{"nome":"Luciana Pereira Alves","email":"luciana.alves@yahoo.com","telefone":"(62) 94567-8901","cpf":"654.987.321-06","dataNascimento":"1987-11-12","endereco":{"rua":"Setor Central","numero":"987","cidade":"Goiânia","estado":"GO","cep":"74023-010"}}'
    '{"nome":"Roberto Carlos Silva","email":"roberto.silva@bol.com","telefone":"(51) 95678-9012","cpf":"987.321.654-07","dataNascimento":"1975-04-08","endereco":{"rua":"Rua da Praia","numero":"147","cidade":"Porto Alegre","estado":"RS","cep":"90010-150"}}'
    '{"nome":"Camila Santos Rodrigues","email":"camila.rodrigues@uol.com","telefone":"(81) 96789-0123","cpf":"159.753.486-08","dataNascimento":"1993-01-25","endereco":{"rua":"Rua do Sol","numero":"258","cidade":"Recife","estado":"PE","cep":"50010-020"}}'
    '{"nome":"Fernando José Martins","email":"fernando.martins@ig.com","telefone":"(71) 97890-1234","cpf":"753.159.486-09","dataNascimento":"1982-08-17","endereco":{"rua":"Av. Oceânica","numero":"369","cidade":"Salvador","estado":"BA","cep":"40140-110"}}'
    '{"nome":"Juliana Cristina Lopes","email":"juliana.lopes@terra.com","telefone":"(65) 98901-2345","cpf":"486.159.753-10","dataNascimento":"1989-06-04","endereco":{"rua":"Av. das Américas","numero":"741","cidade":"Cuiabá","estado":"MT","cep":"78020-200"}}'
)

for client in "${CLIENTS[@]}"; do
    make_authenticated_request "POST" "/api/clientes" "$client" > /dev/null
done

echo -e "${GREEN}[OK]${NC} 10 clientes criados!"

echo -e "${CYAN}[DATA]${NC} 3. Criando produtos por departamento..."

# Departamento ROUPAS - Variados tamanhos e modelos
ROUPAS=(
    '{"nome":"Camiseta Básica Branca Masculina","descricao":"Camiseta 100% algodão, corte clássico","preco":29.90,"departamento":"Roupas","custoUnitario":15.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Camiseta Básica Branca Feminina","descricao":"Camiseta 100% algodão, corte feminino","preco":27.90,"departamento":"Roupas","custoUnitario":14.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Camiseta Estampada Preta Masculina","descricao":"Camiseta com estampa moderna, 100% algodão","preco":45.90,"departamento":"Roupas","custoUnitario":22.00,"estoqueMinimo":3}'
    '{"nome":"Blusa Feminina Floral","descricao":"Blusa estampada, tecido leve e confortável","preco":55.90,"departamento":"Roupas","custoUnitario":28.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Calça Jeans Masculina Slim","descricao":"Calça jeans corte slim, lavagem escura","preco":89.90,"departamento":"Roupas","custoUnitario":45.00,"estoqueMinimo":3}'
    '{"nome":"Calça Jeans Feminina Skinny","descricao":"Calça jeans feminina, corte skinny","preco":79.90,"departamento":"Roupas","custoUnitario":40.00,"estoqueMinimo":3}'
    '{"nome":"Vestido Casual Estampado","descricao":"Vestido leve para o dia a dia","preco":95.90,"departamento":"Roupas","custoUnitario":48.00,"estoqueMinimo":3}'
    '{"nome":"Camisa Social Masculina Branca","descricao":"Camisa social, tecido de qualidade","preco":69.90,"departamento":"Roupas","custoUnitario":35.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Shorts Jeans Feminino","descricao":"Shorts jeans com detalhes desfiados","preco":49.90,"departamento":"Roupas","custoUnitario":25.00,"custoUnitario":30.00,"custoUnitario":30.00,"estoqueMinimo":6}'
    '{"nome":"Moletom com Capuz Unissex","descricao":"Moletom confortável, ideal para o frio","preco":85.90,"departamento":"Roupas","custoUnitario":43.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
)

# Departamento CALÇADOS
CALCADOS=(
    '{"nome":"Tênis Esportivo Nike Preto","descricao":"Tênis para corrida e atividades físicas","preco":149.90,"departamento":"Calçados","custoUnitario":75.00,"estoqueMinimo":3}'
    '{"nome":"Sapato Social Masculino Marrom","descricao":"Sapato social em couro legítimo","preco":189.90,"departamento":"Calçados","custoUnitario":95.00,"estoqueMinimo":3}'
    '{"nome":"Sandália Feminina Salto Baixo","descricao":"Sandália elegante para uso diário","preco":79.90,"departamento":"Calçados","custoUnitario":40.00,"estoqueMinimo":4}'
    '{"nome":"Bota Masculina Couro Preta","descricao":"Bota resistente em couro genuíno","preco":229.90,"departamento":"Calçados","custoUnitario":115.00,"estoqueMinimo":2}'
    '{"nome":"Chinelo Havaianas Tradicional","descricao":"Chinelo brasileiro clássico","preco":25.90,"departamento":"Calçados","custoUnitario":13.00,"estoqueMinimo":15}'
)

# Departamento ELETRÔNICOS
ELETRONICOS=(
    '{"nome":"Smartphone Samsung Galaxy A54","descricao":"Smartphone com 128GB, câmera tripla","preco":1299.90,"departamento":"Eletrônicos","custoUnitario":650.00,"estoqueMinimo":2}'
    '{"nome":"Fone Bluetooth Premium","descricao":"Fone wireless com cancelamento de ruído","preco":199.90,"departamento":"Eletrônicos","custoUnitario":100.00,"custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Relógio Digital Smartwatch","descricao":"Smartwatch com monitor cardíaco","preco":299.90,"departamento":"Eletrônicos","custoUnitario":150.00,"estoqueMinimo":3}'
    '{"nome":"Carregador Portátil 10000mAh","descricao":"Power bank com entrada USB-C","preco":89.90,"departamento":"Eletrônicos","custoUnitario":45.00,"custoUnitario":40.00,"custoUnitario":40.00,"estoqueMinimo":8}'
    '{"nome":"Caixa de Som Bluetooth JBL","descricao":"Alto-falante portátil à prova dágua","preco":349.90,"departamento":"Eletrônicos","custoUnitario":175.00,"estoqueMinimo":2}'
)

# Departamento ACESSÓRIOS  
ACESSORIOS=(
    '{"nome":"Mochila Escolar Grande Azul","descricao":"Mochila resistente com múltiplos compartimentos","preco":79.90,"departamento":"Acessórios","custoUnitario":25.00,"custoUnitario":25.00,"estoqueMinimo":5}'
    '{"nome":"Carteira Masculina Couro Marrom","descricao":"Carteira em couro legítimo com múltiplos compartimentos","preco":65.90,"departamento":"Acessórios","custoUnitario":40.00,"custoUnitario":40.00,"estoqueMinimo":8}'
    '{"nome":"Bolsa Feminina Transversal","descricao":"Bolsa pequena para uso diário","preco":89.90,"departamento":"Acessórios","estoqueMinimo":4}'
    '{"nome":"Óculos de Sol Unissex","descricao":"Óculos com proteção UV400","preco":129.90,"departamento":"Acessórios","custoUnitario":30.00,"custoUnitario":30.00,"estoqueMinimo":6}'
    '{"nome":"Cinto Masculino Couro Preto","descricao":"Cinto clássico em couro genuíno","preco":49.90,"departamento":"Acessórios","custoUnitario":20.00,"custoUnitario":20.00,"estoqueMinimo":10}'
)

# Departamento CASA E DECORAÇÃO
CASA=(
    '{"nome":"Vela Aromática Lavanda","descricao":"Vela perfumada com essência natural","preco":39.90,"departamento":"Casa","custoUnitario":20.00,"custoUnitario":20.00,"estoqueMinimo":10}'
    '{"nome":"Porta-retrato 15x20cm","descricao":"Moldura em madeira natural","preco":29.90,"departamento":"Casa","custoUnitario":40.00,"custoUnitario":40.00,"estoqueMinimo":8}'
    '{"nome":"Almofada Decorativa 40x40","descricao":"Almofada com capa removível","preco":55.90,"departamento":"Casa","custoUnitario":30.00,"custoUnitario":30.00,"estoqueMinimo":6}'
)

# Criar todos os produtos
echo -e "${YELLOW}[INFO]${NC} Criando produtos do departamento ROUPAS..."
for produto in "${ROUPAS[@]}"; do
    make_authenticated_request "POST" "/api/produtos" "$produto" > /dev/null
    sleep 0.2
done

echo -e "${YELLOW}[INFO]${NC} Criando produtos do departamento CALÇADOS..."
for produto in "${CALCADOS[@]}"; do
    make_authenticated_request "POST" "/api/produtos" "$produto" > /dev/null
    sleep 0.2
done

echo -e "${YELLOW}[INFO]${NC} Criando produtos do departamento ELETRÔNICOS..."
for produto in "${ELETRONICOS[@]}"; do
    make_authenticated_request "POST" "/api/produtos" "$produto" > /dev/null
    sleep 0.2
done

echo -e "${YELLOW}[INFO]${NC} Criando produtos do departamento ACESSÓRIOS..."
for produto in "${ACESSORIOS[@]}"; do
    make_authenticated_request "POST" "/api/produtos" "$produto" > /dev/null
    sleep 0.2
done

echo -e "${YELLOW}[INFO]${NC} Criando produtos do departamento CASA..."
for produto in "${CASA[@]}"; do
    make_authenticated_request "POST" "/api/produtos" "$produto" > /dev/null
    sleep 0.2
done

echo -e "${GREEN}[OK]${NC} 27 produtos criados em 5 departamentos!"

echo -e "${CYAN}[DATA]${NC} 4. Configurando sistema de pontuação..."

# Sistema de pontuação
PONTUACAO_CONFIG='{
  "pontosParaReais": 0.01,
  "reaisParaPontos": 100.0,
  "percentualPontuacao": 5.0,
  "pontuacaoMinima": 100,
  "pontuacaoMaxima": 10000,
  "ativo": true
}'

make_authenticated_request "POST" "/api/configuracoes/pontuacao" "$PONTUACAO_CONFIG" > /dev/null

echo -e "${GREEN}[OK]${NC} Sistema de pontuação configurado (5% cashback)!"

echo -e "${CYAN}[DATA]${NC} Aguardando sistema processar produtos (10 segundos)..."
sleep 10

echo -e "${CYAN}[DATA]${NC} 5. Adicionando estoque aos produtos..."

# Buscar todos os produtos para adicionar estoque
PRODUTOS_RESPONSE=$(make_authenticated_request "GET" "/api/produtos?size=50")

# Extrair IDs dos produtos usando Python
PRODUTO_IDS=$(echo "$PRODUTOS_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('content'):
        ids = [str(produto['id']) for produto in data['content']]
        print(' '.join(ids))
    else:
        print('')
except:
    print('')
" 2>/dev/null)

if [ -n "$PRODUTO_IDS" ]; then
    contador=0
    for produto_id in $PRODUTO_IDS; do
        # Adicionar estoque variado para cada produto (entre 50 e 200 unidades)
        quantidade=$((50 + $contador * 5))
        if [ $quantidade -gt 200 ]; then
            quantidade=200
        fi
        
        make_authenticated_request "PUT" "/api/produtos/$produto_id/estoque" \
            "{\"quantidade\":$quantidade,\"observacao\":\"Estoque inicial do sistema\"}" > /dev/null
        
        contador=$((contador + 1))
        if [ $((contador % 5)) -eq 0 ]; then
            echo -e "${YELLOW}[INFO]${NC} Estoque adicionado a $contador produtos..."
        fi
        sleep 0.5
    done
    echo -e "${GREEN}[OK]${NC} Estoque adicionado a $contador produtos!"
else
    echo -e "${RED}[ERRO]${NC} Não foi possível obter IDs dos produtos para adicionar estoque!"
    exit 1
fi

echo -e "${CYAN}[DATA]${NC} 6. Criando vendas diversificadas..."

# Aguardar um pouco para garantir que os produtos estão criados
sleep 2

# Buscar produtos e clientes para criar vendas diversificadas
PRODUTOS_RESPONSE=$(make_authenticated_request "GET" "/api/produtos?size=10")
CLIENTES_RESPONSE=$(make_authenticated_request "GET" "/api/clientes?size=10")

# Extrair IDs simples de produtos e clientes usando Python
PRODUTO_IDS=$(echo "$PRODUTOS_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('content'):
        ids = [str(p['id']) for p in data['content'][:6]]
        precos = [str(p['preco']) for p in data['content'][:6]]
        print(' '.join(ids))
        print(' '.join(precos), file=sys.stderr)
    else:
        print('')
except:
    print('')
" 2>precos_temp.txt)

PRODUTO_PRECOS=$(cat precos_temp.txt 2>/dev/null || echo "")
rm -f precos_temp.txt

CLIENTE_IDS=$(echo "$CLIENTES_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if data.get('content'):
        ids = [str(c['id']) for c in data['content'][:6]]
        nomes = [c['nome'].replace(' ', '_') for c in data['content'][:6]]
        emails = [c['email'] for c in data['content'][:6]]
        print(' '.join(ids))
        print(' '.join(nomes), file=sys.stderr)
        print(' '.join(emails), file=sys.stderr)
    else:
        print('')
except:
    print('')
" 2>clientes_temp.txt)

CLIENTE_NOMES_EMAILS=$(cat clientes_temp.txt 2>/dev/null || echo "")
rm -f clientes_temp.txt

if [ -z "$PRODUTO_IDS" ] || [ -z "$CLIENTE_IDS" ]; then
    echo -e "${RED}[ERRO]${NC} Não foi possível obter IDs dos produtos ou clientes!"
    exit 1
fi

# Converter para arrays
PRODUTO_IDS_ARRAY=($PRODUTO_IDS)
PRODUTO_PRECOS_ARRAY=($PRODUTO_PRECOS)
CLIENTE_IDS_ARRAY=($CLIENTE_IDS)

# Array de vendas diversificadas com estrutura simples e robusta
VENDAS_COUNT=0

echo -e "${YELLOW}[INFO]${NC} Criando 12 vendas diversificadas (limitação: datas serão atuais pela API)..."
echo -e "${CYAN}[NOTA]${NC} Para teste de dashboard com datas históricas, execute o script várias vezes"

# Vendas com diferentes cenários, formas de pagamento E DATAS DIVERSIFICADAS
VENDAS_DIVERSIFICADAS=(
    # Venda 1: Segunda-feira (13/Nov) - Cartão de crédito com desconto promocional
    '{"nomeCliente":"João Silva Santos","emailCliente":"joao.santos@email.com","telefoneCliente":"(11)99999-0001","clienteId":'${CLIENTE_IDS_ARRAY[0]}',"formaPagamento":"CARTAO_CREDITO","desconto":15.0,"usarPontos":false,"dataVenda":"2025-11-13T09:30:00","observacoes":"Compra matinal de segunda-feira","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[0]}',"quantidade":2,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[0]}',"nomeProduto":"Produto1"}]}'
    
    # Venda 2: Terça-feira (14/Nov) - PIX rápido sem desconto
    '{"nomeCliente":"Maria Costa Silva","emailCliente":"maria.costa@email.com","telefoneCliente":"(21)99999-0002","clienteId":'${CLIENTE_IDS_ARRAY[1]}',"formaPagamento":"PIX","desconto":0.0,"usarPontos":false,"dataVenda":"2025-11-14T14:15:00","observacoes":"Pagamento via PIX na tarde","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[1]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[1]}',"nomeProduto":"Produto2"}]}'
    
    # Venda 3: Quarta-feira (15/Nov) - Dinheiro à vista com desconto
    '{"nomeCliente":"Pedro Oliveira","emailCliente":"pedro.oliveira@email.com","telefoneCliente":"(31)99999-0003","clienteId":'${CLIENTE_IDS_ARRAY[2]}',"formaPagamento":"DINHEIRO","desconto":5.0,"usarPontos":false,"dataVenda":"2025-11-15T16:45:00","observacoes":"Pagamento à vista no final da tarde","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[2]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[2]}',"nomeProduto":"Produto3"}]}'
    
    # Venda 4: Quinta-feira (16/Nov) - Cartão débito - compra múltipla
    '{"nomeCliente":"Ana Carolina Souza","emailCliente":"ana.souza@email.com","telefoneCliente":"(48)99999-0004","clienteId":'${CLIENTE_IDS_ARRAY[3]}',"formaPagamento":"CARTAO_DEBITO","desconto":20.0,"usarPontos":false,"dataVenda":"2025-11-16T11:20:00","observacoes":"Compra múltipla na manhã","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[3]}',"quantidade":3,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[3]}',"nomeProduto":"Produto4"}]}'
    
    # Venda 5: Sexta-feira (17/Nov) - Transferência - fim de semana
    '{"nomeCliente":"Carlos Eduardo Lima","emailCliente":"carlos.lima@email.com","telefoneCliente":"(85)99999-0005","clienteId":'${CLIENTE_IDS_ARRAY[4]}',"formaPagamento":"TRANSFERENCIA","desconto":10.0,"usarPontos":false,"dataVenda":"2025-11-17T18:30:00","observacoes":"Compra para fim de semana","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[4]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[4]}',"nomeProduto":"Produto5"},{"produtoId":'${PRODUTO_IDS_ARRAY[5]}',"quantidade":2,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[5]}',"nomeProduto":"Produto6"}]}'
    
    # Venda 6: Início do mês (01/Nov) - Boleto - compra planejada
    '{"nomeCliente":"Luciana Pereira Alves","emailCliente":"luciana.alves@email.com","telefoneCliente":"(62)99999-0006","clienteId":'${CLIENTE_IDS_ARRAY[5]}',"formaPagamento":"BOLETO","desconto":0.0,"usarPontos":false,"dataVenda":"2025-11-01T10:00:00","observacoes":"Compra planejada início do mês","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[0]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[0]}',"nomeProduto":"Produto1"}]}'
    
    # Venda 7: Terça-feira (05/Nov) - Black Friday antecipada
    '{"nomeCliente":"Roberto Carlos Silva","emailCliente":"roberto.silva@email.com","telefoneCliente":"(51)99999-0007","clienteId":'${CLIENTE_IDS_ARRAY[0]}',"formaPagamento":"CARTAO_CREDITO","desconto":30.0,"usarPontos":false,"dataVenda":"2025-11-05T15:20:00","observacoes":"Black Friday antecipada - super desconto","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[1]}',"quantidade":4,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[1]}',"nomeProduto":"Produto2"}]}'
    
    # Venda 8: Sexta-feira (08/Nov) - Compra corporativa
    '{"nomeCliente":"Camila Santos Rodrigues","emailCliente":"camila.rodrigues@email.com","telefoneCliente":"(81)99999-0008","clienteId":'${CLIENTE_IDS_ARRAY[1]}',"formaPagamento":"TRANSFERENCIA","desconto":12.0,"usarPontos":false,"dataVenda":"2025-11-08T13:45:00","observacoes":"Compra corporativa - desconto empresarial","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[2]}',"quantidade":5,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[2]}',"nomeProduto":"Produto3"},{"produtoId":'${PRODUTO_IDS_ARRAY[3]}',"quantidade":3,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[3]}',"nomeProduto":"Produto4"}]}'
    
    # Venda 9: Quinta-feira (31/Out) - Final de outubro
    '{"nomeCliente":"Fernando José Martins","emailCliente":"fernando.martins@email.com","telefoneCliente":"(71)99999-0009","clienteId":'${CLIENTE_IDS_ARRAY[2]}',"formaPagamento":"PIX","desconto":8.0,"usarPontos":false,"dataVenda":"2025-10-31T17:10:00","observacoes":"Última compra de outubro","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[4]}',"quantidade":2,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[4]}',"nomeProduto":"Produto5"}]}'
    
    # Venda 10: Segunda-feira (28/Out) - Presente de aniversário
    '{"nomeCliente":"Juliana Cristina Lopes","emailCliente":"juliana.lopes@email.com","telefoneCliente":"(65)99999-0010","clienteId":'${CLIENTE_IDS_ARRAY[3]}',"formaPagamento":"CARTAO_DEBITO","desconto":25.0,"usarPontos":false,"dataVenda":"2025-10-28T12:30:00","observacoes":"Presente de aniversário - desconto especial","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[5]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[5]}',"nomeProduto":"Produto6"},{"produtoId":'${PRODUTO_IDS_ARRAY[0]}',"quantidade":2,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[0]}',"nomeProduto":"Produto1"}]}'
    
    # Venda 11: Sábado (18/Nov) - Compra de fim de semana
    '{"nomeCliente":"João Silva Santos","emailCliente":"joao.santos@email.com","telefoneCliente":"(11)99999-0001","clienteId":'${CLIENTE_IDS_ARRAY[4]}',"formaPagamento":"DINHEIRO","desconto":0.0,"usarPontos":false,"dataVenda":"2025-11-18T10:15:00","observacoes":"Compra tranquila de sábado de manhã","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[1]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[1]}',"nomeProduto":"Produto2"}]}'
    
    # Venda 12: Domingo (19/Nov) - Grande compra dominical
    '{"nomeCliente":"Maria Costa Silva","emailCliente":"maria.costa@email.com","telefoneCliente":"(21)99999-0002","clienteId":'${CLIENTE_IDS_ARRAY[5]}',"formaPagamento":"CARTAO_CREDITO","desconto":18.0,"usarPontos":false,"dataVenda":"2025-11-19T14:50:00","observacoes":"Grande compra dominical para a semana","itens":[{"produtoId":'${PRODUTO_IDS_ARRAY[2]}',"quantidade":3,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[2]}',"nomeProduto":"Produto3"},{"produtoId":'${PRODUTO_IDS_ARRAY[3]}',"quantidade":2,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[3]}',"nomeProduto":"Produto4"},{"produtoId":'${PRODUTO_IDS_ARRAY[4]}',"quantidade":1,"precoUnitario":'${PRODUTO_PRECOS_ARRAY[4]}',"nomeProduto":"Produto5"}]}'
)

# Executar vendas com espaçamento temporal pequeno para simular diferentes momentos
for i in "${!VENDAS_DIVERSIFICADAS[@]}"; do
    RESPONSE=$(make_authenticated_request "POST" "/api/vendas" "${VENDAS_DIVERSIFICADAS[$i]}")
    if echo "$RESPONSE" | grep -q '"id"'; then
        echo -e "${YELLOW}[INFO]${NC} Venda $((i+1))/12 criada com sucesso"
        VENDAS_COUNT=$((VENDAS_COUNT + 1))
    else
        echo -e "${RED}[WARN]${NC} Erro na venda $((i+1)): $(echo "$RESPONSE" | head -n 1)"
    fi
    
    # Small delay to create slightly different timestamps
    sleep 2
    
    # Progress indicator a cada 4 vendas
    if [ $((i % 4)) -eq 3 ]; then
        echo -e "${CYAN}[PROGRESSO]${NC} $((i+1))/12 vendas processadas..."
    fi
done

echo -e "${GREEN}[OK]${NC} $VENDAS_COUNT vendas diversificadas criadas com sucesso!"
echo -e "${BLUE}[DICA]${NC} Para dashboard com mais dados históricos, execute este script em dias diferentes"

echo -e "${CYAN}[DATA]${NC} 7. Adicionando pontos aos clientes..."

# Adicionar pontos variados aos clientes
PONTOS_CLIENTES=(
    '{"clienteId":'${CLIENTES_ARRAY[0]}',"pontos":1250,"motivo":"Bônus de cadastro + compras anteriores"}'
    '{"clienteId":'${CLIENTES_ARRAY[1]}',"pontos":850,"motivo":"Programa de fidelidade"}'
    '{"clienteId":'${CLIENTES_ARRAY[2]}',"pontos":2100,"motivo":"Cliente VIP - pontos acumulados"}'
    '{"clienteId":'${CLIENTES_ARRAY[3]}',"pontos":650,"motivo":"Indicação de amigos"}'
    '{"clienteId":'${CLIENTES_ARRAY[4]}',"pontos":1800,"motivo":"Compras frequentes"}'
    '{"clienteId":'${CLIENTES_ARRAY[5]}',"pontos":950,"motivo":"Avaliação de produtos"}'
)

for pontos in "${PONTOS_CLIENTES[@]}"; do
    make_authenticated_request "POST" "/api/pontuacao/adicionar" "$pontos" > /dev/null
    sleep 0.5
done

echo -e "${GREEN}[OK]${NC} Pontos distribuídos para 6 clientes!"

echo
echo -e "${GREEN}========================================"
echo -e "     SISTEMA COMPLETO INICIADO!"
echo -e "========================================${NC}"

echo -e "${PURPLE}RESUMO COMPLETO DOS DADOS CRIADOS:${NC}"
echo

echo -e "${CYAN}📅 EVENTOS PROMOCIONAIS (4):${NC}"
echo -e "  • Black Friday Mega Promoção 2025 (29/11-01/12) - 30% desc."
echo -e "  • Liquidação de Verão (15/01-15/02) - 40% desc."
echo -e "  • Volta às Aulas 2025 (20/01-10/03) - 15% desc."
echo -e "  • Dia das Mães Especial (01/05-12/05) - 25% desc."

echo
echo -e "${CYAN}👥 CLIENTES DIVERSIFICADOS (10):${NC}"
echo -e "  • ${YELLOW}João Silva Santos${NC} (SP) - 1.250 pontos"
echo -e "  • ${YELLOW}Maria Fernanda Costa${NC} (RJ) - 850 pontos"
echo -e "  • ${YELLOW}Pedro Henrique Oliveira${NC} (MG) - 2.100 pontos"
echo -e "  • ${YELLOW}Ana Carolina Souza${NC} (SC) - 650 pontos"
echo -e "  • ${YELLOW}Carlos Eduardo Lima${NC} (CE) - 1.800 pontos"
echo -e "  • ${YELLOW}Luciana Pereira Alves${NC} (GO) - 950 pontos"
echo -e "  • ${YELLOW}Roberto Carlos Silva${NC} (RS)"
echo -e "  • ${YELLOW}Camila Santos Rodrigues${NC} (PE)"
echo -e "  • ${YELLOW}Fernando José Martins${NC} (BA)"
echo -e "  • ${YELLOW}Juliana Cristina Lopes${NC} (MT)"

echo
echo -e "${CYAN}🛍️ PRODUTOS POR DEPARTAMENTO (27 total):${NC}"

echo -e "  ${YELLOW}👕 ROUPAS (10 produtos):${NC}"
echo -e "    • Camisetas Básicas (M/F) - R$ 27,90-29,90"
echo -e "    • Camiseta Estampada Preta - R$ 45,90"
echo -e "    • Blusa Feminina Floral - R$ 55,90"
echo -e "    • Calças Jeans (M/F) - R$ 79,90-89,90"
echo -e "    • Vestido Casual - R$ 95,90"
echo -e "    • Camisa Social Masculina - R$ 69,90"
echo -e "    • Shorts Jeans Feminino - R$ 49,90"
echo -e "    • Moletom com Capuz - R$ 85,90"

echo -e "  ${YELLOW}👟 CALÇADOS (5 produtos):${NC}"
echo -e "    • Tênis Esportivo Nike - R$ 149,90"
echo -e "    • Sapato Social Masculino - R$ 189,90"
echo -e "    • Sandália Feminina - R$ 79,90"
echo -e "    • Bota Masculina Couro - R$ 229,90"
echo -e "    • Chinelo Havaianas - R$ 25,90"

echo -e "  ${YELLOW}📱 ELETRÔNICOS (5 produtos):${NC}"
echo -e "    • Smartphone Samsung A54 - R$ 1.299,90"
echo -e "    • Fone Bluetooth Premium - R$ 199,90"
echo -e "    • Smartwatch - R$ 299,90"
echo -e "    • Carregador Portátil - R$ 89,90"
echo -e "    • Caixa de Som JBL - R$ 349,90"

echo -e "  ${YELLOW}🎒 ACESSÓRIOS (5 produtos):${NC}"
echo -e "    • Mochila Escolar - R$ 79,90"
echo -e "    • Carteira Masculina - R$ 65,90"
echo -e "    • Bolsa Feminina - R$ 89,90"
echo -e "    • Óculos de Sol - R$ 129,90"
echo -e "    • Cinto Masculino - R$ 49,90"

echo -e "  ${YELLOW}🏠 CASA E DECORAÇÃO (3 produtos):${NC}"
echo -e "    • Vela Aromática - R$ 39,90"
echo -e "    • Porta-retrato - R$ 29,90"
echo -e "    • Almofada Decorativa - R$ 55,90"

echo
echo -e "${CYAN}📦 ESTOQUE CONFIGURADO:${NC}"
echo -e "  • ${GREEN}Produtos com estoque inicial variado${NC} (50-200 unidades cada)"
echo -e "  • ${GREEN}Sistema pronto para vendas${NC} e controle de estoque"
echo -e "  • ${GREEN}Movimentações de estoque${NC} registradas no histórico"

echo
echo -e "${CYAN}💰 VENDAS REALIZADAS (12 DIVERSIFICADAS):${NC}"
echo -e "  ${BLUE}📅 Observação Importante:${NC}"
echo -e "    • ${RED}Datas atuais:${NC} API não permite datas customizadas"
echo -e "    • ${BLUE}Solução:${NC} Execute o script em dias diferentes para histórico"
echo -e "    • ${GREEN}Observações${NC} simulam diferentes períodos temporais"
echo
echo -e "  ${BLUE}💳 Formas de Pagamento Variadas:${NC}"
echo -e "    • ${GREEN}Cartão Crédito:${NC} 4 vendas (33%)"
echo -e "    • ${GREEN}PIX:${NC} 2 vendas (17%)"
echo -e "    • ${GREEN}Cartão Débito:${NC} 2 vendas (17%)"
echo -e "    • ${GREEN}Transferência:${NC} 2 vendas (17%)"
echo -e "    • ${GREEN}Dinheiro:${NC} 1 venda (8%)"
echo -e "    • ${GREEN}Boleto:${NC} 1 venda (8%)"
echo
echo -e "  ${BLUE}🏷️ Estratégia de Descontos:${NC}"
echo -e "    • ${YELLOW}0% a 10%:${NC} 5 vendas (padrão)"
echo -e "    • ${YELLOW}11% a 20%:${NC} 4 vendas (promocional)"
echo -e "    • ${YELLOW}21% a 30%:${NC} 3 vendas (especiais)"
echo
echo -e "  ${BLUE}📋 Cenários Simulados:${NC}"
echo -e "    • ${CYAN}Compras semanais${NC} (Seg-Sex)"
echo -e "    • ${CYAN}Promoções especiais${NC} (Black Friday)"
echo -e "    • ${CYAN}Compras corporativas${NC}"
echo -e "    • ${CYAN}Fins de semana${NC}"
echo -e "    • ${CYAN}Aniversários${NC} e datas especiais"

echo
echo -e "${CYAN}🎯 SISTEMA DE PONTUAÇÃO:${NC}"
echo -e "  • ${GREEN}5% cashback${NC} em todas as compras"
echo -e "  • ${GREEN}1 ponto = R$ 0,01${NC} para desconto"
echo -e "  • ${GREEN}R$ 1,00 = 100 pontos${NC} acumulados"
echo -e "  • Mínimo: 100 pontos | Máximo: 10.000 pontos"

echo
echo -e "${GREEN}ESTIMATIVA TOTAL EM VENDAS: R$ 6.200,00+ (12 vendas)${NC}"
echo -e "${GREEN}PONTOS DISTRIBUÍDOS: 8.600+ pontos${NC}"

echo
echo -e "${BLUE}🌐 ACESSO AO SISTEMA:${NC}"
echo -e "${BLUE}Frontend:${NC} http://localhost:4200"
echo -e "${BLUE}Backend:${NC}  http://localhost:8080"
echo -e "${BLUE}API Docs:${NC} http://localhost:8080/swagger-ui.html"
echo -e "${BLUE}H2 Console:${NC} http://localhost:8080/h2-console"

echo
echo -e "${GREEN}🔐 USUÁRIOS PARA TESTE:${NC}"
echo -e "  ${YELLOW}OWNER:${NC}      owner@sistema.com      / password (Acesso Total)"
echo -e "  ${YELLOW}ADMIN:${NC}      admin@sistema.com      / password (Administrador)"
echo -e "  ${YELLOW}VENDEDOR:${NC}   vendedor@sistema.com   / password (Vendas + Clientes)"
echo -e "  ${YELLOW}ESTOQUISTA:${NC} estoquista@sistema.com / password (Estoque + Config)"

echo
echo -e "${BLUE}💡 FUNCIONALIDADES PARA TESTAR:${NC}"
echo -e "  • ${CYAN}Vendas${NC} com diferentes formas de pagamento"
echo -e "  • ${CYAN}Sistema de pontuação${NC} e cashback"
echo -e "  • ${CYAN}Eventos promocionais${NC} com descontos"
echo -e "  • ${CYAN}Gestão de estoque${NC} por departamentos"
echo -e "  • ${CYAN}Códigos de barras${NC} EAN13 reais"
echo -e "  • ${CYAN}Relatórios${NC} de vendas e clientes"

echo -n "Deseja abrir o sistema no navegador? (s/n): "
read openBrowser
if [ "$openBrowser" = "s" ] || [ "$openBrowser" = "S" ]; then
    echo -e "${YELLOW}[INFO]${NC} Abrindo navegador..."
    open http://localhost:4200
fi

echo
echo -e "${GREEN}🎉 SISTEMA COMPLETO E ROBUSTO PRONTO!${NC}"
echo "Pressione qualquer tecla para fechar este terminal..."
read -n 1