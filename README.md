# Frontend Qualidade e Teste

Projeto de sistema de gestão com frontend Angular e backend Java para disciplina de Qualidade e Teste de Software.

## 📁 Estrutura do Projeto

```
/
├── src/                          # Frontend Angular
│   ├── app/
│   │   ├── components/
│   │   ├── services/
│   │   └── models/
│   └── environments/
├── backend-java/                 # Classes Java para Teste
│   ├── src/main/java/
│   │   └── com/qualidade/teste/
│   │       ├── model/            # Modelos de dados
│   │       └── service/          # Classes de serviço (3 classes principais)
│   ├── src/test/java/            # Testes unitários
│   ├── pom.xml                   # Configuração Maven
│   └── README.md                 # Documentação detalhada do backend
└── README.md                     # Este arquivo
```

## 🎯 Entrega 1 - Artefatos

### Classes de Backend Java (Complexas, não-CRUD)

Localização: `./backend-java/src/main/java/com/qualidade/teste/service/`

1. **ServicoCalculoDesconto.java**
   - Cálculo de descontos baseado em regras complexas
   - Validação de cupons e limites
   - Lógica de fidelidade e sazonalidade

2. **ServicoValidacaoEstoque.java**
   - Validação de disponibilidade para vendas
   - Sistema de reservas temporárias
   - Controle de movimentações de estoque

3. **ServicoRelatorioVendas.java**
   - Geração de relatórios e análises
   - Cálculos estatísticos e agregações
   - Previsões baseadas em histórico

### Características das Classes

- ✅ **Não são CRUD simples** - Implementam lógica de negócio complexa
- ✅ **Complexidade razoável** - Múltiplos caminhos de execução
- ✅ **Testabilidade** - Métodos independentes e validações
- ✅ **Dados mockados** - Simulam cenários reais de uso
- ✅ **Documentação** - JavaDoc e comentários explicativos

### Casos de Teste Unitários

Localização: `./backend-java/src/test/java/com/qualidade/teste/service/`

- **ServicoCalculoDescontoTest.java** - Exemplo completo de testes
- Cobertura de cenários: limites, validações, regras de negócio
- Uso de JUnit 5, AssertJ e Mockito
- Organização em classes aninhadas (@Nested)

## 🛠️ Tecnologias Utilizadas

### Frontend
- Angular 17
- TypeScript
- RxJS
- Bootstrap
- Chart.js

### Backend Java
- Java 11
- Maven
- JUnit 5
- Mockito
- AssertJ
- JaCoCo (cobertura)

## 🚀 Como Executar

### Frontend Angular
```bash
cd "d:\Faculdade\Qualidade e Teste\frontend"
npm install
npm start
# Acesse http://localhost:4200
```

### Backend Java - Testes
```bash
cd "d:\Faculdade\Qualidade e Teste\frontend\backend-java"
mvn clean test
mvn jacoco:report  # Gerar relatório de cobertura
```

## 📊 Plano de Teste

### Escopo dos Módulos Testados

1. **ServicoCalculoDesconto**
   - Cálculo de descontos por tipo de cliente
   - Aplicação de regras de fidelidade
   - Validação de cupons e limites máximos
   - Cenários de aniversário e sazonalidade

2. **ServicoValidacaoEstoque**
   - Validação de disponibilidade para vendas
   - Gerenciamento de reservas temporárias
   - Controle de movimentações de entrada/saída
   - Alertas de estoque baixo

3. **ServicoRelatorioVendas**
   - Geração de relatórios por período
   - Análise de comportamento de clientes
   - Identificação de produtos com baixa performance
   - Cálculos de sazonalidade e previsões

### Estratégias de Teste

- **Testes Unitários:** Validação de métodos individuais
- **Testes de Integração:** Interação entre componentes
- **Testes de Limites:** Valores extremos e edge cases
- **Testes de Validação:** Parâmetros inválidos e tratamento de erros
- **Testes de Regras de Negócio:** Cenários complexos reais

### Ferramentas de Teste

- **JUnit 5:** Framework de testes unitários
- **Mockito:** Criação de mocks para isolamento
- **AssertJ:** Assertions fluentes e expressivas
- **JaCoCo:** Cobertura de código e relatórios
- **Maven Surefire:** Execução automatizada de testes

### Métricas de Qualidade

- **Cobertura de Código:** Meta 80%+
- **Cobertura de Branches:** Meta 70%+
- **Casos de Teste:** 15-20 por classe principal
- **Complexidade:** Moderada para garantir testabilidade

## 📋 Artefatos Entregues

### Código-fonte Original
- Classes de serviço em Java com lógica complexa
- Dados mockados para simulação realista
- Estrutura de projeto Maven configurada

### Casos de Teste Unitários
- Exemplo completo para ServicoCalculoDesconto
- Cenários de teste abrangentes
- Organização clara e documentação

### Plano de Teste
- Escopo detalhado dos módulos testados
- Estratégias e ferramentas definidas
- Métricas de qualidade estabelecidas
- Documentação no README do backend

### Documentação
- README principal com visão geral
- README do backend com detalhes técnicos
- JavaDoc nas classes de serviço
- Comentários explicativos no código

## 🎓 Objetivos Acadêmicos

Este projeto foi desenvolvido para demonstrar:

1. **Criação de Classes Testáveis**
   - Lógica de negócio complexa
   - Separação de responsabilidades
   - Validações e tratamento de erros

2. **Elaboração de Casos de Teste**
   - Cobertura de cenários diversos
   - Testes de limites e validações
   - Organização e documentação

3. **Aplicação de Ferramentas**
   - Frameworks de teste modernos
   - Métricas de cobertura
   - Automatização com Maven

4. **Boas Práticas**
   - Código limpo e documentado
   - Estrutura de projeto organizada
   - Padronização e convenções

## 📞 Informações do Projeto

- **Disciplina:** Qualidade e Teste de Software
- **Entrega:** Etapa 1 (Peso 3)
- **Branch Principal:** develop
- **Repositório:** BrunoMello00/frontend-qualidade-teste

## 📝 Notas Importantes

- Todas as classes estão na branch principal (develop) conforme solicitado
- Os artefatos estão claramente organizados e documentados
- O README contém links diretos para todos os artefatos
- As classes possuem complexidade adequada para testes unitários
- Os dados mockados permitem cenários realistas de teste