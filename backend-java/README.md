# Backend Java - Projeto de Qualidade e Teste

Este diretório contém as 3 classes de backend em Java criadas para o projeto de Qualidade e Teste de Software.

## 📁 Estrutura do Projeto

```
backend-java/
├── src/
│   ├── main/java/com/qualidade/teste/
│   │   ├── model/
│   │   │   ├── Cliente.java
│   │   │   ├── Produto.java
│   │   │   ├── Venda.java
│   │   │   └── ItemVenda.java
│   │   └── service/
│   │       ├── ServicoCalculoDesconto.java      (Classe 1)
│   │       ├── ServicoValidacaoEstoque.java     (Classe 2)
│   │       └── ServicoRelatorioVendas.java      (Classe 3)
│   └── test/java/com/qualidade/teste/service/
│       └── ServicoCalculoDescontoTest.java
├── pom.xml
└── README.md (este arquivo)
```

## 🎯 Classes Principais para Teste

### 1. ServicoCalculoDesconto
**Localização:** `src/main/java/com/qualidade/teste/service/ServicoCalculoDesconto.java`

**Responsabilidade:** Calcular descontos baseados em regras de negócio complexas.

**Complexidades implementadas:**
- Desconto por tipo de cliente (Bronze, Prata, Ouro, Premium)
- Desconto por programa de fidelidade (baseado em pontos)
- Desconto por quantidade de itens na compra
- Desconto por categoria de produtos e promoções ativas
- Validação e aplicação de cupons de desconto
- Cálculo de desconto de aniversário
- Aplicação de limites máximos de desconto
- Validação de regras de negócio

**Métodos principais para teste:**
- `calcularDescontoVenda(Cliente, Venda, String)` - Método principal
- `calcularDescontoAniversario(Cliente, LocalDate)` - Lógica de aniversário
- `validarDescontoPermitido(BigDecimal, Cliente, Produto)` - Validações

### 2. ServicoValidacaoEstoque
**Localização:** `src/main/java/com/qualidade/teste/service/ServicoValidacaoEstoque.java`

**Responsabilidade:** Validações e operações de controle de estoque.

**Complexidades implementadas:**
- Validação de disponibilidade para vendas
- Sistema de reservas temporárias com expiração
- Controle de movimentações (entrada/saída)
- Alertas de estoque baixo com sugestões
- Limpeza automática de reservas expiradas
- Histórico de movimentações

**Métodos principais para teste:**
- `validarDisponibilidadeParaVenda(List<ItemVenda>)` - Validação principal
- `criarReservaTemporaria(List<ItemVenda>, int)` - Sistema de reservas
- `processarEntradaEstoque(Long, int, String)` - Entrada de mercadorias
- `processarSaidaEstoque(Long, int, String)` - Saída de mercadorias
- `obterProdutosComEstoqueBaixo()` - Análise de estoque

### 3. ServicoRelatorioVendas
**Localização:** `src/main/java/com/qualidade/teste/service/ServicoRelatorioVendas.java`

**Responsabilidade:** Gerar relatórios e análises de vendas.

**Complexidades implementadas:**
- Relatórios de vendas por período com métricas
- Análise de comportamento de clientes
- Identificação de produtos com baixa performance
- Cálculo de sazonalidade de vendas
- Previsão de vendas baseada em histórico
- Cálculos estatísticos e agregações
- Análise de tendências e padrões

**Métodos principais para teste:**
- `gerarRelatorioVendasPeriodo(LocalDate, LocalDate)` - Relatório principal
- `analisarComportamentoCliente(Long)` - Análise de cliente
- `identificarProdutosBaixaPerformance(int)` - Produtos com problemas
- `calcularSazonalidadeVendas()` - Análise sazonal
- `gerarPrevisaoVendas(int)` - Previsões futuras

## 🧪 Estratégias de Teste Sugeridas

### Casos de Teste por Classe

#### ServicoCalculoDesconto
- **Teste de Limites:** Valores extremos de desconto, quantidades, pontos de fidelidade
- **Teste de Regras:** Combinações de diferentes tipos de desconto
- **Teste de Validação:** Parâmetros inválidos, produtos inativos, sem estoque
- **Teste de Limites Máximos:** Aplicação correta de limites por produto/cliente
- **Teste de Cupons:** Validação de hierarquia de clientes para cupons

#### ServicoValidacaoEstoque
- **Teste de Concorrência:** Múltiplas reservas simultâneas
- **Teste de Expiração:** Limpeza automática de reservas vencidas
- **Teste de Validação:** Quantidades negativas, produtos inexistentes
- **Teste de Estado:** Movimentações que deixam estoque negativo
- **Teste de Alertas:** Cálculo correto de produtos com estoque baixo

#### ServicoRelatorioVendas
- **Teste de Períodos:** Datas inválidas, períodos vazios, sobreposições
- **Teste de Cálculos:** Precisão de aggregações, médias, percentuais
- **Teste de Previsões:** Validação de algoritmos de previsão
- **Teste de Performance:** Relatórios com grandes volumes de dados
- **Teste de Sazonalidade:** Cálculos mensais, identificação de picos

## 🔧 Como Executar os Testes

### Pré-requisitos
- Java 11 ou superior
- Maven 3.6 ou superior

### Comandos Maven

```bash
# Compilar o projeto
mvn clean compile

# Executar todos os testes
mvn test

# Executar testes com relatório de cobertura
mvn clean test jacoco:report

# Executar apenas testes de uma classe específica
mvn test -Dtest=ServicoCalculoDescontoTest

# Gerar relatório de cobertura (estará em target/site/jacoco/index.html)
mvn jacoco:report
```

## 📊 Dados Mockados

Todas as classes utilizam dados mockados internamente para simular:
- **Clientes:** Diferentes tipos (Bronze, Prata, Ouro, Premium) com pontos de fidelidade
- **Produtos:** Diversas categorias com preços e estoques variados
- **Vendas:** Histórico de vendas distribuído ao longo do ano
- **Promoções:** Descontos por categoria e cupons de desconto
- **Configurações:** Regras de negócio e limites do sistema

## 🎓 Objetivos Educacionais

Estas classes foram projetadas especificamente para ensino de:

1. **Testes Unitários:** Cada método possui lógica testável independentemente
2. **Cobertura de Código:** Múltiplos caminhos de execução e branches
3. **Teste de Integração:** Interação entre diferentes componentes
4. **Teste de Regressão:** Garantia de que mudanças não quebram funcionalidades
5. **Boas Práticas:** Validação de entrada, tratamento de erros, documentação

## 📈 Métricas de Qualidade Esperadas

- **Cobertura de Código:** Meta de 80%+ cobertura de linhas
- **Cobertura de Branches:** Meta de 70%+ cobertura de branches
- **Complexidade Ciclomática:** Métodos com complexidade moderada para teste
- **Casos de Teste:** Mínimo 15-20 casos por classe principal
- **Assertions:** Múltiplas validações por teste

## 🔍 Exemplos de Cenários de Teste Complexos

### Cenário 1: Desconto Combinado
- Cliente Premium com 15.000 pontos
- Compra de 25 itens de roupas
- Cupom "BLACKFRIDAY" aplicado
- Verificar se todos os descontos são aplicados corretamente

### Cenário 2: Reserva com Conflito
- Produto com estoque = 5
- Duas reservas simultâneas de 3 unidades cada
- Verificar que apenas uma reserva é bem-sucedida

### Cenário 3: Relatório Sazonal
- Vendas distribuídas ao longo de 12 meses
- Verificar identificação correta de picos e vales
- Validar cálculo de fatores de sazonalidade

## 📝 Notas para Implementação

1. **Dados Mockados:** Os dados são inicializados no construtor de cada classe
2. **Threads:** As classes não são thread-safe por design (boa oportunidade para testes de concorrência)
3. **Validações:** Múltiplas validações de entrada para diferentes cenários de erro
4. **Configurabilidade:** Regras de negócio podem ser facilmente modificadas para diferentes cenários de teste

## 🚀 Próximos Passos

1. Implementar casos de teste para as outras duas classes de serviço
2. Adicionar testes de integração entre as classes
3. Implementar testes de performance para grandes volumes
4. Adicionar testes de concorrência para operações de estoque
5. Criar mocks para simular dependências externas