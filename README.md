# Sistema de Gestão - Qualidade e Teste de Software

Projeto full-stack com frontend Angular e backend Java desenvolvido para demonstrar práticas de qualidade e teste de software.

## � Início Rápido

### **Comando Principal (Recomendado)**
```bash
npm run dev:fullstack
```
Inicia **ambos** os servidores simultaneamente:
- 🟦 **Backend** (Spring Boot): http://localhost:8080
- 🟢 **Frontend** (Angular): http://localhost:4200

### **Setup Inicial (Windows)**
```cmd
setup.bat
```

### **Pré-requisitos**
- Node.js 18+ e npm
- Java 11+ (JDK)  
- Maven 3.6+

## �📁 Estrutura do Projeto

```
/
├── src/                          # Frontend Angular
│   ├── app/components/          # Componentes da interface
│   ├── app/services/            # Serviços Angular
│   └── environments/            # Configurações
├── backend-java/                # Backend Java Spring Boot
│   ├── src/main/java/com/qualidade/teste/
│   │   ├── model/              # Modelos de dados
│   │   ├── service/            # 3 classes principais (foco do projeto)
│   │   └── controller/         # Controllers REST
│   ├── src/test/java/          # Testes unitários JUnit 5
│   └── pom.xml                 # Configuração Maven
└── README.md                   # Este arquivo
```

## 🎯 Classes Principais (Foco Acadêmico)

### **3 Serviços com Lógica Complexa (Não-CRUD)**

**Localização:** `./backend-java/src/main/java/com/qualidade/teste/service/`

#### 1. **ServicoCalculoDesconto.java**
- Cálculo de descontos por tipo de cliente (Bronze, Ouro, Premium)
- Sistema de fidelidade e pontuação
- Aplicação de cupons promocionais
- Descontos sazonais e limitações por produto

#### 2. **ServicoValidacaoEstoque.java**
- Validação de disponibilidade para vendas
- Sistema de reservas temporárias
- Controle de movimentações de estoque
- Alertas de estoque baixo

#### 3. **ServicoRelatorioVendas.java**
- Geração de relatórios por período
- Análise de comportamento de clientes
- Cálculos estatísticos e previsões
- Identificação de produtos com baixa performance

## 🧪 Estratégia de Testes

### **Testes Unitários Implementados**
- **Framework:** JUnit 5 + Mockito + AssertJ
- **Exemplo:** ServicoCalculoDescontoTest (7 casos)
- **Cobertura:** JaCoCo para métricas
- **Status:** 100% dos testes passando

### **Casos de Teste Manual**
- **Ferramenta:** TestLink + Planilhas
- **Cenários:** Validação de regras de negócio
- **Documentação:** Casos detalhados no Plano de Teste

## 🛠️ Tecnologias Utilizadas

### **Frontend**
- Angular 17 + TypeScript
- Bootstrap + Chart.js
- RxJS para programação reativa

### **Backend**
- Java 11 + Maven + Spring Boot
- JUnit 5 + Mockito + AssertJ
- JaCoCo para cobertura de código

## � Comandos Disponíveis

### **Desenvolvimento**
```bash
npm run dev:fullstack     # Backend + Frontend simultâneos ⭐
npm run dev:fullstack:settings2 # Com configurações alternativas
npm run backend:run       # Apenas backend (porta 8080)
npm run backend:run:settings2 # Backend com settings2.xml (porta 8081)
npm start                 # Apenas frontend (porta 4200)
```

### **Testes**
```bash
npm run backend:test      # Testes unitários
npm run backend:test:settings2 # Testes com settings2.xml
npm run backend:test-report # Testes + relatório cobertura
npm run full:test         # Todos os testes
```

### **Build**
```bash
npm run backend:compile   # Compilar Java
npm run full:build        # Build completo produção
```

## 🌐 URLs dos Serviços

### **Backend APIs**
- **Status:** http://localhost:8080/api/status
- **Health:** http://localhost:8080/api/health

### **Frontend**
- **App:** http://localhost:4200

### **Resposta Esperada da API Status:**
```json
{
  "status": "ok",
  "message": "Backend Qualidade e Teste funcionando",
  "services": {
    "desconto": "Serviço de Cálculo de Descontos",
    "estoque": "Serviço de Validação de Estoque", 
    "relatorios": "Serviço de Relatórios de Vendas"
  }
}
```

## 🎓 Objetivos Acadêmicos

### **Demonstração de Conhecimentos:**
1. **Classes Testáveis:** Lógica de negócio complexa
2. **Casos de Teste:** Cobertura abrangente e organizados
3. **Ferramentas:** Frameworks profissionais (JUnit, Maven)
4. **Qualidade:** Métricas e documentação completa

### **Características das Classes:**
- ✅ **Não-CRUD:** Implementam regras de negócio complexas
- ✅ **Testáveis:** Métodos independentes e validações
- ✅ **Documentadas:** JavaDoc e comentários explicativos
- ✅ **Mockadas:** Dados simulam cenários reais

## 📋 Entregáveis

### **Código-fonte**
- ✅ 3 classes Java com lógica complexa
- ✅ Testes unitários abrangentes
- ✅ Estrutura Maven configurada

### **Documentação**
- ✅ Plano de Teste estruturado
- ✅ README técnico completo
- ✅ Casos de teste manuais
- ✅ JavaDoc nas classes

### **Qualidade**
- ✅ Cobertura de código 80%+
- ✅ 100% testes passando
- ✅ Métricas documentadas
- ✅ Automação com scripts

## 🐛 Troubleshooting

### **Erro "Port already in use"**
```bash
npx kill-port 4200 8080
```

### **Backend não inicia**
```bash
java -version               # Verificar Java
npm run backend:compile     # Recompilar
```

### **Frontend não inicia**
```bash
npm ci                      # Limpar cache
ng serve                    # Tentar direto
```



## 🏆 Comandos Rápidos

| Ação | Comando |
|------|---------|
| **Subir tudo** | `npm run dev:fullstack` |
| **Testar backend** | `npm run backend:test` |
| **Ver cobertura** | `npm run backend:test-report` |
| **Build produção** | `npm run full:build` |

---

✨ **Sistema integrado e documentado para demonstração de práticas de qualidade e teste de software!**