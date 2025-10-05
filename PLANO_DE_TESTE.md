# PLANO DE TESTE - Sistema de Gestão de Estoque e Vendas

## 1. ESCOPO DO SISTEMA

### 1.1 Descrição Geral
Sistema full-stack para gestão de estoque e vendas com frontend Angular e backend Java, desenvolvido para demonstrar práticas de qualidade e teste de software.

### 1.2 Módulos/Componentes a Serem Testados

#### **Módulo 1: Serviço de Cálculo de Desconto**
- **Classe:** `ServicoCalculoDesconto.java`
- **Responsabilidade:** Calcular descontos baseado em regras complexas de negócio
- **Funcionalidades:**
  - Cálculo por tipo de cliente (Bronze, Ouro, Premium)
  - Aplicação de regras de fidelidade
  - Validação e aplicação de cupons
  - Descontos sazonais e por aniversário
  - Limitação de desconto máximo por produto

#### **Módulo 2: Serviço de Validação de Estoque**
- **Classe:** `ServicoValidacaoEstoque.java`
- **Responsabilidade:** Controlar disponibilidade e movimentações de estoque
- **Funcionalidades:**
  - Validação de disponibilidade para vendas
  - Sistema de reservas temporárias
  - Controle de movimentações (entrada/saída)
  - Alertas de estoque baixo
  - Prevenção de venda sem estoque

#### **Módulo 3: Serviço de Relatórios de Vendas**
- **Classe:** `ServicoRelatorioVendas.java`
- **Responsabilidade:** Gerar análises e relatórios de vendas
- **Funcionalidades:**
  - Relatórios por período
  - Análise de comportamento de clientes
  - Identificação de produtos com baixa performance
  - Cálculos de sazonalidade
  - Previsões baseadas em histórico

### 1.3 Critérios de Inclusão/Exclusão

#### **Incluído no Escopo:**
- ✅ Lógica de negócio das classes de serviço
- ✅ Validações e regras de negócio
- ✅ Tratamento de erros e exceções
- ✅ Cenários de uso complexos
- ✅ Integração entre componentes

#### **Excluído do Escopo:**
- ❌ Interface de usuário (UI)
- ❌ Operações CRUD básicas
- ❌ Conexões com banco de dados
- ❌ Autenticação e autorização
- ❌ Performance e carga

## 2. OBJETIVOS DOS TESTES

### 2.1 Objetivos Gerais
- Validar a corretude da lógica de negócio
- Verificar o tratamento adequado de exceções
- Garantir a qualidade do código
- Demonstrar práticas de teste unitário

### 2.2 Métricas de Qualidade
- **Cobertura de Código:** Mínimo 80%
- **Cobertura de Branches:** Mínimo 70%
- **Casos de Teste:** 15-20 por classe
- **Taxa de Sucesso:** 100% dos testes aprovados

## 3. ESTRATÉGIAS DE TESTE

### 3.1 Tipos de Teste

#### **Testes Unitários**
- Teste de métodos individuais
- Isolamento através de mocks
- Validação de retornos esperados
- Verificação de exceções

#### **Testes de Integração**
- Interação entre componentes
- Fluxos de dados entre serviços
- Validação de contratos

#### **Testes de Limites**
- Valores extremos (máximo/mínimo)
- Dados inválidos
- Null/vazio
- Overflow/underflow

#### **Testes de Regras de Negócio**
- Cenários reais de uso
- Combinações complexas
- Casos especiais
- Edge cases

### 3.2 Técnicas de Teste
- **Particionamento de Equivalência**
- **Análise de Valor Limite**
- **Teste de Caixa Branca**
- **Teste de Caixa Preta**

## 4. FERRAMENTAS E TECNOLOGIAS

### 4.1 Ferramentas de Teste
- **JUnit 5:** Framework de testes unitários
- **Mockito:** Criação de objetos mock
- **AssertJ:** Assertions fluentes
- **JaCoCo:** Cobertura de código
- **Maven Surefire:** Execução automatizada

### 4.2 Ferramentas de Gestão
- **TestLink:** Gestão de casos de teste manuais
- **Git:** Controle de versão
- **IntelliJ IDEA:** IDE de desenvolvimento
- **Maven:** Gerenciamento de dependências

### 4.3 Ferramentas de Relatório
- **JaCoCo Report:** Relatórios de cobertura
- **Surefire Reports:** Resultados dos testes
- **TestLink Reports:** Relatórios de teste manual

## 5. CASOS DE TESTE MANUAL

### 5.1 Cenários de Teste Manual

#### **CT001: Cálculo de Desconto Cliente Premium**
- **Objetivo:** Validar desconto para cliente premium com alta fidelidade
- **Ferramenta:** TestLink
- **Prioridade:** Alta

#### **CT002: Validação de Estoque Insuficiente**
- **Objetivo:** Verificar comportamento quando produto não tem estoque
- **Ferramenta:** TestLink
- **Prioridade:** Alta

#### **CT003: Geração de Relatório Mensal**
- **Objetivo:** Validar relatório de vendas do mês
- **Ferramenta:** Documento/Planilha
- **Prioridade:** Média

## 6. CRONOGRAMA DE EXECUÇÃO

### 6.1 Fases do Teste
1. **Fase 1:** Preparação do ambiente (Concluída)
2. **Fase 2:** Execução de testes unitários (Concluída)
3. **Fase 3:** Execução de testes manuais (Em andamento)
4. **Fase 4:** Análise de resultados (Pendente)
5. **Fase 5:** Relatório final (Pendente)

## 7. ENTREGÁVEIS

### 7.1 Artefatos de Teste
- ✅ Código-fonte das classes Java
- ✅ Casos de teste unitários
- ✅ Plano de teste (este documento)
- 🔄 Casos de teste manual no TestLink
- 🔄 Relatórios de execução
- 🔄 Análise de cobertura

### 7.2 Documentação
- ✅ README do projeto
- ✅ JavaDoc das classes
- ✅ Comentários no código
- 🔄 Manual de execução dos testes

## 8. CRITÉRIOS DE ACEITAÇÃO

### 8.1 Critérios de Aprovação
- Todos os testes unitários passando (100%)
- Cobertura de código ≥ 80%
- Casos de teste manual executados
- Documentação completa
- Código seguindo padrões de qualidade

### 8.2 Critérios de Rejeição
- Falha em mais de 5% dos testes
- Cobertura de código < 70%
- Bugs críticos não tratados
- Documentação incompleta

---

**Responsável:** [Seu Nome]  
**Data:** 05/10/2025  
**Versão:** 1.0  
**Status:** Em Execução