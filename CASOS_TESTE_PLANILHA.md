# CASOS DE TESTE MANUAIS - PLANILHA

## TEMPLATE PARA EXCEL/GOOGLE SHEETS

### **Estrutura da Planilha:**

| ID | Título | Prioridade | Tipo | Pré-condições | Passos | Resultado Esperado | Status | Observações |
|---|---|---|---|---|---|---|---|---|
| CT004 | Geração Relatório Mensal de Vendas | Média | Funcional | Sistema ativo, dados de vendas do mês disponíveis | 1. Acessar ServicoRelatorioVendas<br>2. Chamar gerarRelatorioMensal(mes, ano)<br>3. Verificar dados retornados | Relatório com vendas do mês, totais corretos, lista de produtos | PASS | Executado em 05/10/2025 |
| CT005 | Cliente Bronze com Poucos Pontos | Baixa | Funcional | Cliente Bronze com 200 pontos cadastrado | 1. Criar cliente Bronze<br>2. Definir 200 pontos fidelidade<br>3. Calcular desconto para compra R$ 500<br>4. Verificar percentual aplicado | Desconto mínimo aplicado (5-10%), justificativa simples | PASS | Desconto de 8% aplicado |
| CT006 | Produto Sem Limite de Desconto | Média | Negativo | Produto sem percentual máximo definido | 1. Criar produto sem limite desconto<br>2. Cliente Premium fazer compra<br>3. Tentar aplicar desconto<br>4. Verificar comportamento | Sistema aplica desconto padrão ou lança exceção apropriada | PASS | Sistema aplicou 30% padrão |

---

## CASO DE TESTE 4 - DETALHADO

### **CT004: Geração de Relatório Mensal de Vendas**

**Informações Gerais:**
- **ID:** CT004
- **Título:** Validar geração de relatório mensal
- **Prioridade:** Média
- **Tipo:** Funcional
- **Executor:** Bruno Mello
- **Data:** 05/10/2025

**Pré-condições:**
1. Sistema backend em execução
2. Dados de vendas do mês outubro/2025 disponíveis
3. Pelo menos 5 vendas registradas no período
4. Vendas de diferentes produtos e clientes

**Passos de Execução:**
1. **Passo 1:** Preparar dados de teste
   - Criar 5 vendas no mês 10/2025
   - Diferentes valores: R$ 100, R$ 250, R$ 500, R$ 150, R$ 300
   - **Resultado Esperado:** Vendas criadas com sucesso

2. **Passo 2:** Chamar geração de relatório
   - Executar: `servicoRelatorio.gerarRelatorioMensal(10, 2025)`
   - **Resultado Esperado:** Método executa sem erro

3. **Passo 3:** Verificar total de vendas
   - Verificar campo: `relatorio.getTotalVendas()`
   - **Resultado Esperado:** Total = R$ 1.300,00

4. **Passo 4:** Verificar quantidade de transações
   - Verificar campo: `relatorio.getQuantidadeVendas()`
   - **Resultado Esperado:** Quantidade = 5

5. **Passo 5:** Verificar produtos mais vendidos
   - Verificar campo: `relatorio.getProdutosMaisVendidos()`
   - **Resultado Esperado:** Lista ordenada por quantidade

**Critérios de Aceitação:**
- ✅ Relatório gerado sem erros
- ✅ Valores calculados corretamente
- ✅ Dados estruturados adequadamente
- ✅ Performance aceitável (< 2 segundos)

---

## CASO DE TESTE 5 - DETALHADO

### **CT005: Cliente Bronze com Poucos Pontos de Fidelidade**

**Informações Gerais:**
- **ID:** CT005
- **Título:** Validar desconto mínimo para cliente Bronze
- **Prioridade:** Baixa
- **Tipo:** Funcional
- **Executor:** Pedro Canellas
- **Data:** 05/10/2025

**Pré-condições:**
1. Sistema backend funcionando
2. Produto de R$ 500,00 disponível
3. Cliente Bronze com poucos pontos cadastrado

**Passos de Execução:**
1. **Passo 1:** Configurar cliente Bronze
   - Tipo: BRONZE
   - Pontos: 200 (baixo)
   - **Resultado Esperado:** Cliente criado corretamente

2. **Passo 2:** Criar venda teste
   - Produto: R$ 500,00
   - Quantidade: 1
   - **Resultado Esperado:** Venda válida criada

3. **Passo 3:** Calcular desconto
   - Executar: `calcularDescontoVenda(cliente, venda, null)`
   - **Resultado Esperado:** Desconto mínimo aplicado

4. **Passo 4:** Verificar percentual
   - Verificar: percentual entre 5% e 10%
   - **Resultado Esperado:** Desconto dentro da faixa Bronze

5. **Passo 5:** Verificar valor final
   - Calcular: R$ 500,00 - desconto
   - **Resultado Esperado:** Valor entre R$ 450,00 e R$ 475,00

**Critérios de Aceitação:**
- ✅ Desconto aplicado mesmo com poucos pontos
- ✅ Percentual dentro da faixa Bronze (5-10%)
- ✅ Justificativa menciona tipo Bronze
- ✅ Cálculo correto do valor final

---

## CASO DE TESTE 6 - DETALHADO

### **CT006: Produto Sem Limite Máximo de Desconto**

**Informações Gerais:**
- **ID:** CT006
- **Título:** Validar comportamento com produto sem limite
- **Prioridade:** Média
- **Tipo:** Boundary/Negativo
- **Executor:** Tales Piotrowski
- **Data:** 05/10/2025

**Pré-condições:**
1. Sistema backend ativo
2. Cliente Premium com muitos pontos
3. Produto sem percentual máximo definido

**Passos de Execução:**
1. **Passo 1:** Criar produto especial
   - Preço: R$ 1.000,00
   - Limite máximo: null ou 0%
   - **Resultado Esperado:** Produto criado sem limite

2. **Passo 2:** Configurar cliente Premium
   - Tipo: PREMIUM
   - Pontos: 20.000 (muito alto)
   - **Resultado Esperado:** Cliente com máximo benefício

3. **Passo 3:** Tentar aplicar desconto
   - Executar cálculo de desconto
   - **Resultado Esperado:** Sistema trata a exceção

4. **Passo 4:** Verificar comportamento
   - Analisar: erro, desconto padrão ou limitado
   - **Resultado Esperado:** Comportamento consistente

5. **Passo 5:** Validar resultado
   - Verificar se resultado é lógico
   - **Resultado Esperado:** Desconto razoável aplicado

**Critérios de Aceitação:**
- ✅ Sistema não quebra com produto sem limite
- ✅ Desconto aplicado é razoável (max 50%)
- ✅ Mensagem clara sobre o comportamento
- ✅ Não há valores negativos ou inválidos

---
