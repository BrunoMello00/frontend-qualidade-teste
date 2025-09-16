import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray, AbstractControl } from '@angular/forms';
import { VendaService } from '../../services/venda.service';
import { ProdutoService } from '../../services/produto.service';
import { EventosService, Evento } from '../../services/eventos.service';
import { ClienteService, Cliente } from '../../services/cliente.service';

interface Produto {
  id: number;
  nome: string;
  preco: number;
  quantidade: number;
  categoria: string;
  quantidadeMinima: number;
}

interface ItemVenda {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  precoUnitario: number;
  desconto: number; // desconto específico para este item (valor em R$)
  percentualDesconto: number; // percentual de desconto para este item
  subtotal: number; // subtotal sem desconto
  totalComDesconto: number; // total após aplicar desconto
}

interface Venda {
  id?: number;
  dataVenda: Date;
  clienteId?: number; // ID do cliente (se cadastrado)
  clienteNome: string;
  clienteCpf: string; // CPF obrigatório (real ou fake)
  clienteEmail?: string;
  clienteTelefone?: string;
  evento?: string; // Novo campo para evento/razão
  itens: ItemVenda[];
  subtotal: number;
  desconto: number;
  percentualDesconto: number; // Novo campo para percentual de desconto
  total: number;
  formaPagamento: string;
  status: string;
  observacoes?: string;
}

@Component({
  selector: 'app-vendas',
  templateUrl: './vendas.component.html',
  styleUrls: ['./vendas.component.css']
})
export class VendasComponent implements OnInit {
  
  // Estados
  vendas: Venda[] = [];
  produtos: Produto[] = [];
  eventos: Evento[] = []; // Nova lista de eventos
  isLoading = false;
  showVendaForm = false;
  errorMessage = '';
  successMessage = '';

  // Estados para edição de venda
  vendaEdicao: Venda | null = null;
  itensEdicao: ItemVenda[] = [];
  novoProdutoId: number = 0;
  novaQuantidade: number = 1;

  // ===== SISTEMA DE AUTOCOMPLETE =====
  produtoSearchTerms: { [key: number]: string } = {};
  showDropdown: { [key: number]: boolean } = {};
  filteredProdutos: { [key: number]: Produto[] } = {};

  // ===== SISTEMA DE CLIENTES =====
  clienteAtual: Cliente | null = null;
  showCadastroCliente = false;
  cpfBusca = '';
  clienteForm!: FormGroup;
  buscandoCliente = false;
  clienteEncontrado = false;
  showOpcoesCliente = false; // Nova variável para controlar exibição das opções

  // Formulários
  vendaForm!: FormGroup;
  searchForm!: FormGroup;

  // Filtros
  dateFilter = '';
  statusFilter = '';
  searchTerm = '';
  clienteFilter = '';
  eventoFilter = ''; // Novo filtro por evento
  precoOrder = ''; // Filtro de ordem por preço (asc, desc)
  orderByPrice = ''; // Ordenação por preço para o template
  activeFilterBox = ''; // Para controlar qual box está ativo

  // Paginação
  currentPage = 1;
  itemsPerPage = 10;
  totalItems = 0;

  // Estatísticas
  stats = {
    vendasHoje: 0,
    faturamentoHoje: 0,
    vendasMes: 0,
    faturamentoMes: 0,
    ticketMedio: 0,
    vendasPendentes: 0
  };

  // Opções
  formasPagamento = [
    'DINHEIRO',
    'CARTAO_CREDITO',
    'CARTAO_DEBITO',
    'PIX',
    'TRANSFERENCIA',
    'BOLETO'
  ];

  statusVenda = [
    'PENDENTE',
    'CONFIRMADA',
    'ENTREGUE',
    'CANCELADA'
  ];

  constructor(
    private fb: FormBuilder,
    private vendaService: VendaService,
    private produtoService: ProdutoService,
    private eventosService: EventosService,
    private clienteService: ClienteService
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadVendas();
    this.loadProdutos();
    this.loadEventos(); // Carregar eventos
    this.loadStats();
  }

  initializeForms(): void {
    this.vendaForm = this.fb.group({
      clienteNome: ['', [Validators.required, Validators.minLength(2)]],
      clienteEmail: ['', [Validators.email]],
      clienteTelefone: [''],
      evento: [''], // Novo campo para evento/razão
      formaPagamento: ['DINHEIRO', Validators.required],
      pagamentoParcelado: [false], // Novo campo para indicar se é parcelado
      numeroParcelas: [1, [Validators.min(1), Validators.max(12)]], // Novo campo para número de parcelas
      jurosPercentual: ['', [Validators.min(0), Validators.max(100)]], // Novo campo para juros
      desconto: ['', [Validators.min(0)]],
      // Percentual de desconto começa vazio para melhor UX
      percentualDesconto: ['', [Validators.min(0), Validators.max(100)]], // Campo começa vazio
      observacoes: [''],
      itens: this.fb.array([])
    });

    this.searchForm = this.fb.group({
      termo: [''],
      dataInicio: [''],
      dataFim: [''],
      status: [''],
      cliente: [''],
      evento: [''] // Novo filtro por evento
    });

    // Formulário do cliente para cadastro durante a venda
    this.clienteForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required, Validators.pattern(/^\(\d{2}\) \d{4,5}-\d{4}$/)]],
      cpf: ['', [Validators.required, this.cpfValidator]],
      endereco: this.fb.group({
        cep: ['', [Validators.required, Validators.pattern(/^\d{5}-\d{3}$/)]],
        rua: ['', Validators.required],
        numero: ['', Validators.required],
        complemento: [''],
        bairro: ['', Validators.required],
        cidade: ['', Validators.required],
        estado: ['', Validators.required]
      })
    });

    // Adicionar primeiro item vazio
    this.addItem();
  }

  get itensFormArray(): FormArray {
    return this.vendaForm.get('itens') as FormArray;
  }

  createItemFormGroup(): FormGroup {
    return this.fb.group({
      produtoId: ['', Validators.required],
      // Prevenção de quantidades negativas ou zero: min(1)
      quantidade: ['', [Validators.required, Validators.min(1)]],
      // Prevenção de preços negativos ou zero: min(0.01)
      precoUnitario: ['', [Validators.required, Validators.min(0.01)]],
      subtotal: [{ value: '', disabled: true }],
      desconto: [{ value: '', disabled: true }],
      // Desconto não pode ser negativo ou maior que 100%
      percentualDesconto: ['', [Validators.min(0), Validators.max(100)]],
      totalComDesconto: [{ value: '', disabled: true }]
    });
  }

  addItem(): void {
    const newIndex = this.itensFormArray.length;
    this.itensFormArray.push(this.createItemFormGroup());
    
    // Inicializar valores do autocomplete para o novo item
    this.produtoSearchTerms[newIndex] = '';
    this.showDropdown[newIndex] = false;
    this.filteredProdutos[newIndex] = [...this.produtos];
  }

  removeItem(index: number): void {
    if (this.itensFormArray.length > 1) {
      this.itensFormArray.removeAt(index);
      
      // Limpar dados do autocomplete para o item removido
      delete this.produtoSearchTerms[index];
      delete this.showDropdown[index];
      delete this.filteredProdutos[index];
      
      // Reorganizar índices dos itens restantes
      const maxIndex = this.itensFormArray.length;
      for (let i = index; i < maxIndex; i++) {
        if (this.produtoSearchTerms[i + 1] !== undefined) {
          this.produtoSearchTerms[i] = this.produtoSearchTerms[i + 1];
          this.showDropdown[i] = this.showDropdown[i + 1];
          this.filteredProdutos[i] = this.filteredProdutos[i + 1];
          
          delete this.produtoSearchTerms[i + 1];
          delete this.showDropdown[i + 1];
          delete this.filteredProdutos[i + 1];
        }
      }
      
      this.calculateTotal();
    }
  }

  onProdutoChange(index: number): void {
    const item = this.itensFormArray.at(index);
    const produtoId = item.get('produtoId')?.value;
    
    if (produtoId) {
      const produto = this.produtos.find(p => p.id == produtoId);
      if (produto) {
        item.patchValue({
          precoUnitario: produto.preco
        });
        this.calculateItemSubtotal(index);
      }
    }
  }

  onQuantidadeChange(index: number): void {
    const item = this.itensFormArray.at(index);
    const quantidade = item.get('quantidade')?.value || 0;
    
    // Validação: quantidade deve ser pelo menos 1
    if (quantidade < 1) {
      item.patchValue({ quantidade: 1 });
      this.errorMessage = 'Quantidade deve ser pelo menos 1 unidade';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    
    this.calculateItemSubtotal(index);
  }

  onPrecoChange(index: number): void {
    const item = this.itensFormArray.at(index);
    const preco = item.get('precoUnitario')?.value || 0;
    
    // Validação: preço deve ser maior que R$ 0,00
    if (preco <= 0) {
      item.patchValue({ precoUnitario: 0.01 });
      this.errorMessage = 'Preço deve ser maior que R$ 0,00';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    
    this.calculateItemSubtotal(index);
  }

  // ===== MÉTODOS DE AUTOCOMPLETE =====
  
  onProdutoFocus(index: number): void {
    // Ao focar no campo, mostrar todas as opções disponíveis
    this.showDropdown[index] = true;
    this.filteredProdutos[index] = this.produtos; // Mostrar todos os produtos inicialmente
  }
  
  onProdutoSearch(index: number, event: Event): void {
    const target = event.target as HTMLInputElement;
    const searchTerm = target.value.toLowerCase();
    this.produtoSearchTerms[index] = searchTerm;
    this.showDropdown[index] = true;
    
    // Se não há termo de busca, mostrar todos os produtos
    if (!searchTerm || searchTerm.trim() === '') {
      this.filteredProdutos[index] = this.produtos;
    } else {
      // Filtrar produtos baseado no termo de busca (nome, categoria)
      this.filteredProdutos[index] = this.produtos.filter(produto =>
        produto.nome.toLowerCase().includes(searchTerm) ||
        produto.categoria.toLowerCase().includes(searchTerm) ||
        produto.id.toString().includes(searchTerm)
      );
    }
  }

  onProdutoBlur(index: number): void {
    // Delay para permitir clique no dropdown
    setTimeout(() => {
      this.showDropdown[index] = false;
    }, 200);
  }

  getFilteredProdutos(index: number): Produto[] {
    // Se não há produtos filtrados para este índice, retornar todos os produtos
    if (!this.filteredProdutos[index]) {
      this.filteredProdutos[index] = this.produtos;
    }
    return this.filteredProdutos[index];
  }

  getProdutoNome(index: number): string {
    const item = this.itensFormArray.at(index);
    const produtoId = item.get('produtoId')?.value;
    
    if (produtoId) {
      const produto = this.produtos.find(p => p.id == produtoId);
      return produto ? produto.nome : '';
    }
    
    return this.produtoSearchTerms[index] || '';
  }

  selectProduto(index: number, produto: Produto): void {
    const item = this.itensFormArray.at(index);
    
    // Definir o produto selecionado
    item.patchValue({
      produtoId: produto.id,
      precoUnitario: produto.preco
    });
    
    // Limpar busca e fechar dropdown
    this.produtoSearchTerms[index] = produto.nome;
    this.showDropdown[index] = false;
    
    // Calcular subtotal
    this.calculateItemSubtotal(index);
    
    // Chamar o método original de mudança de produto
    this.onProdutoChange(index);
  }

  calculateItemSubtotal(index: number): void {
    const item = this.itensFormArray.at(index);
    const quantidade = item.get('quantidade')?.value || 0;
    const precoUnitario = item.get('precoUnitario')?.value || 0;
    const subtotal = quantidade * precoUnitario;
    const percentualDesconto = item.get('percentualDesconto')?.value || 0;
    const desconto = (subtotal * percentualDesconto) / 100;
    const totalComDesconto = subtotal - desconto;
    
    item.patchValue({
      subtotal: subtotal,
      desconto: desconto,
      totalComDesconto: totalComDesconto
    });
    
    this.calculateTotal();
  }

  calculateTotal(): void {
    let subtotalOriginal = 0; // Valor sem descontos
    let subtotalComDescontoItens = 0; // Valor já com descontos por item
    
    this.itensFormArray.controls.forEach(item => {
      subtotalOriginal += item.get('subtotal')?.value || 0;
      subtotalComDescontoItens += item.get('totalComDesconto')?.value || 0;
    });
    
    // Debug - vamos verificar os valores
    console.log('DEBUG - Subtotal Original:', subtotalOriginal);
    console.log('DEBUG - Subtotal Com Desconto Itens:', subtotalComDescontoItens);
    
    // CORREÇÃO: Desconto geral da venda é aplicado sobre o valor já com descontos por item
    const descontoGeral = this.vendaForm.get('desconto')?.value || 0;
    const total = subtotalComDescontoItens - descontoGeral;
    
    console.log('DEBUG - Desconto Geral:', descontoGeral);
    console.log('DEBUG - Total Final:', total);
    
    // CORREÇÃO: Atualizar o formulário com os valores corretos
    this.vendaForm.patchValue({
      subtotal: subtotalOriginal, // Mantém o valor original para referência
      total: total // Total já corrigido
    }, { emitEvent: false }); // emitEvent: false evita loops infinitos
  }

  // Método para calcular desconto por percentual
  onPercentualDescontoChange(): void {
    const percentualValue = this.vendaForm.get('percentualDesconto')?.value;
    
    // Se campo estiver vazio, não aplicar desconto
    if (percentualValue === '' || percentualValue === null || percentualValue === undefined) {
      this.vendaForm.patchValue({
        desconto: 0
      });
      this.calculateTotal();
      return;
    }
    
    const percentual = parseFloat(percentualValue) || 0;
    
    // Validação: percentual deve estar entre 0 e 100
    if (percentual < 0) {
      this.vendaForm.patchValue({ percentualDesconto: '' });
      this.errorMessage = 'Desconto não pode ser negativo';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    if (percentual > 100) {
      this.vendaForm.patchValue({ percentualDesconto: 100 });
      this.errorMessage = 'Desconto não pode ser maior que 100%';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    
    // Aplica desconto sobre o subtotal que já tem descontos por item
    const subtotalComDescontoItens = this.getSubtotalComDescontoItens();
    const desconto = (subtotalComDescontoItens * percentual) / 100;
    
    this.vendaForm.patchValue({
      desconto: desconto
    });
    
    this.calculateTotal();
  }

  // ===== MÉTODOS DE PARCELAMENTO =====
  
  onFormaPagamentoChange(): void {
    const formaPagamento = this.vendaForm.get('formaPagamento')?.value;
    
    if (formaPagamento !== 'CARTAO_CREDITO') {
      // Se não for cartão de crédito, resetar campos de parcelamento
      this.vendaForm.patchValue({
        pagamentoParcelado: false,
        numeroParcelas: 1,
        jurosPercentual: 0
      });
    }
    
    this.calculateTotal();
  }

  onParcelamentoChange(): void {
    const isParcelado = this.vendaForm.get('pagamentoParcelado')?.value;
    
    if (!isParcelado) {
      // Se não for parcelado, resetar campos relacionados
      this.vendaForm.patchValue({
        numeroParcelas: 1,
        jurosPercentual: 0
      });
    } else {
      // Se for parcelado, definir valor padrão de 2 parcelas
      this.vendaForm.patchValue({
        numeroParcelas: 2
      });
    }
    
    this.calculateTotal();
  }

  onJurosChange(): void {
    this.calculateTotal();
  }

  isCartaoCredito(): boolean {
    return this.vendaForm.get('formaPagamento')?.value === 'CARTAO_CREDITO';
  }

  isParcelado(): boolean {
    return this.vendaForm.get('pagamentoParcelado')?.value === true;
  }

  getValorComJuros(): number {
    const total = this.getTotal();
    const jurosPercentual = this.vendaForm.get('jurosPercentual')?.value || 0;
    return total + (total * jurosPercentual / 100);
  }

  getValorParcela(): number {
    const numeroParcelas = this.vendaForm.get('numeroParcelas')?.value || 1;
    return this.getValorComJuros() / numeroParcelas;
  }

  getSubtotal(): number {
    let subtotal = 0;
    this.itensFormArray.controls.forEach(item => {
      subtotal += item.get('subtotal')?.value || 0;
    });
    return subtotal;
  }

  // Novo método para obter subtotal já com descontos por item
  getSubtotalComDescontoItens(): number {
    let subtotal = 0;
    this.itensFormArray.controls.forEach(item => {
      subtotal += item.get('totalComDesconto')?.value || 0;
    });
    return subtotal;
  }

  getTotal(): number {
    // CORREÇÃO: O total deve ser baseado no valor já com descontos por item
    const subtotalComDescontoItens = this.getSubtotalComDescontoItens();
    const desconto = this.vendaForm.get('desconto')?.value || 0;
    return Math.max(0, subtotalComDescontoItens - desconto);
  }

  getTotalFinal(): number {
    // Total final incluindo juros (se houver)
    const total = this.getTotal();
    const jurosPercentual = this.vendaForm.get('jurosPercentual')?.value || 0;
    return total + (total * jurosPercentual / 100);
  }

  // ===== VALIDADOR DE CPF =====
  cpfValidator(control: AbstractControl): {[key: string]: any} | null {
    const cpf = control.value;
    if (!cpf) return null;
    
    // Remove pontos e hífens
    const numbers = cpf.replace(/\D/g, '');
    
    // Verifica se tem 11 dígitos
    if (numbers.length !== 11) return { cpfInvalido: true };
    
    // Verifica se não são todos números iguais
    if (/^(\d)\1{10}$/.test(numbers)) return { cpfInvalido: true };
    
    // Valida primeiro dígito verificador
    let sum = 0;
    for (let i = 0; i < 9; i++) {
      sum += parseInt(numbers[i]) * (10 - i);
    }
    let digit1 = 11 - (sum % 11);
    if (digit1 >= 10) digit1 = 0;
    
    if (digit1 !== parseInt(numbers[9])) return { cpfInvalido: true };
    
    // Valida segundo dígito verificador
    sum = 0;
    for (let i = 0; i < 10; i++) {
      sum += parseInt(numbers[i]) * (11 - i);
    }
    let digit2 = 11 - (sum % 11);
    if (digit2 >= 10) digit2 = 0;
    
    if (digit2 !== parseInt(numbers[10])) return { cpfInvalido: true };
    
    return null;
  }

  // ===== MÉTODOS DE CARREGAMENTO =====

  async loadVendas(): Promise<void> {
    try {
      this.isLoading = true;
      
      // TODO: Carregar vendas reais do backend
      this.vendaService.obterVendasRecentes(50).subscribe({
        next: (vendas) => {
          // Converter vendas do service para o formato local
          this.vendas = (vendas || []).map((venda: any) => ({
            id: venda.id,
            // Converter strings para Date quando necessário
            dataVenda: (typeof venda.dataVenda === 'string') ? new Date(venda.dataVenda) : (venda.dataVenda || new Date()),
            clienteId: (venda as any).clienteId || null,
            clienteNome: venda.clienteNome || 'Cliente Avulso',
            clienteCpf: venda.clienteCpf || '000.000.000-00',
            clienteEmail: venda.clienteEmail,
            clienteTelefone: venda.clienteTelefone,
            evento: venda.evento || '',
            itens: (venda.itens || []).map((item: any) => ({
              produtoId: item.produtoId,
              produtoNome: (item.produto && item.produto.nome) || item.nomeProduto || 'Produto',
              quantidade: item.quantidade,
              precoUnitario: item.precoUnitario,
              desconto: item.descontoItem !== undefined ? item.descontoItem : (item.desconto || 0),
              percentualDesconto: item.percentualDesconto !== undefined ? item.percentualDesconto : 0,
              subtotal: (item.quantidade || 0) * (item.precoUnitario || 0),
              totalComDesconto: ((item.quantidade || 0) * (item.precoUnitario || 0)) - (item.descontoItem !== undefined ? item.descontoItem : (item.desconto || 0))
            })),
            // suportar tanto 'valorTotal' (mock) quanto 'total' (service)
            subtotal: (venda as any).valorTotal || venda.subtotal || 0,
            desconto: venda.desconto || 0,
            percentualDesconto: venda.percentualDesconto || 0,
            total: (venda as any).valorTotal || venda.total || 0,
            formaPagamento: venda.formaPagamento || 'Dinheiro',
            status: venda.status || 'Finalizada',
            observacoes: venda.observacoes
          }));
          this.totalItems = this.vendas.length;
          console.log('✅ Vendas carregadas:', this.vendas.length);
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('❌ Erro ao carregar vendas:', error);
          this.vendas = [];
          this.totalItems = 0;
          this.errorMessage = 'Erro ao carregar vendas';
          this.isLoading = false;
        }
      });
      
    } catch (error) {
      this.errorMessage = 'Erro ao carregar vendas';
      console.error('Erro:', error);
    } finally {
      this.isLoading = false;
    }
  }

  async loadProdutos(): Promise<void> {
    try {
      // TODO: Carregar produtos reais do backend
      this.produtoService.listarProdutos().subscribe({
        next: (produtos) => {
          // Converter produtos do service para o formato local
          this.produtos = (produtos || []).map(produto => ({
            id: produto.id || 0,
            nome: produto.nome,
            preco: produto.preco,
            quantidade: produto.quantidade || produto.estoque || 0,
            categoria: produto.categoria || 'Sem categoria',
            quantidadeMinima: produto.quantidadeMinima || produto.estoqueMinimo || 0
          }));
          console.log('✅ Produtos carregados:', this.produtos.length);
        },
        error: (error: any) => {
          console.error('❌ Erro ao carregar produtos:', error);
          this.produtos = [];
        }
      });
    } catch (error) {
      console.error('Erro ao carregar produtos:', error);
    }
  }

  async loadEventos(): Promise<void> {
    try {
      this.eventosService.listarEventos().subscribe({
        next: (eventos) => {
          this.eventos = eventos || [];
          console.log('✅ Eventos carregados:', this.eventos.length);
        },
        error: (error: any) => {
          console.error('❌ Erro ao carregar eventos:', error);
          this.eventos = [];
        }
      });
    } catch (error) {
      console.error('Erro ao carregar eventos:', error);
    }
  }

  async loadStats(): Promise<void> {
    try {
      // TODO: Carregar estatísticas reais do backend
      this.vendaService.obterEstatisticasVendas().subscribe({
        next: (stats) => {
          this.stats = {
            vendasHoje: stats.vendasDia || 0,
            faturamentoHoje: stats.faturamentoDia || 0,
            vendasMes: stats.vendasMes || 0,
            faturamentoMes: stats.faturamentoMes || 0,
            ticketMedio: stats.ticketMedio || 0,
            vendasPendentes: 0 // TODO: implementar no backend
          };
          console.log('✅ Estatísticas carregadas:', this.stats);
        },
        error: (error: any) => {
          console.error('❌ Erro ao carregar estatísticas:', error);
          this.stats = {
            vendasHoje: 0,
            faturamentoHoje: 0,
            vendasMes: 0,
            faturamentoMes: 0,
            ticketMedio: 0,
            vendasPendentes: 0
          };
        }
      });
    } catch (error) {
      console.error('Erro ao carregar estatísticas:', error);
    }
  }

  async onSubmitVenda(): Promise<void> {
    if (this.vendaForm.valid && this.itensFormArray.length > 0) {
      try {
        this.isLoading = true;
        
        const vendaData = {
          ...this.vendaForm.value,
          dataVenda: new Date(),
          subtotal: this.getSubtotal(),
          total: this.getTotal(),
          status: 'PENDENTE',
          clienteId: this.clienteAtual?.id || null,
          clienteCpf: this.clienteAtual?.cpf || this.cpfBusca || null
        };
        
        // Salvar venda no backend
        this.vendaService.criarVenda(vendaData).subscribe({
          next: (vendaCriada) => {
            this.successMessage = 'Venda registrada com sucesso!';
            
            // Se cliente foi identificado (não é CPF fake), registrar a compra para fidelidade
            if (this.clienteAtual && this.clienteAtual.cpf !== '000.000.000-00') {
              this.successMessage += ` Pontos adicionados para ${this.clienteAtual.nome}.`;
              // TODO: Implementar sistema de fidelidade no backend real
              console.log('⚠️ Sistema de fidelidade precisa ser implementado no backend');
            }
            
            this.resetVendaForm();
            this.loadVendas();
            this.loadStats();
            this.isLoading = false;
          },
          error: (error: any) => {
            console.error('❌ Erro ao registrar venda:', error);
            this.errorMessage = 'Erro ao registrar venda';
            this.isLoading = false;
          }
        });
        
      } catch (error) {
        this.errorMessage = 'Erro ao registrar venda';
        console.error('Erro:', error);
      } finally {
        this.isLoading = false;
      }
    } else {
      this.markFormGroupTouched(this.vendaForm);
    }
  }

  resetVendaForm(): void {
    this.vendaForm.reset();
    this.vendaForm.patchValue({
      formaPagamento: 'DINHEIRO',
      desconto: 0
    });
    
    // Limpar dados do cliente
    this.limparCliente();
    
    // Limpar array de itens
    while (this.itensFormArray.length > 1) {
      this.itensFormArray.removeAt(1);
    }
    
    // Reset primeiro item
    this.itensFormArray.at(0).reset();
    this.showVendaForm = false;
  }

  toggleVendaForm(): void {
    this.showVendaForm = !this.showVendaForm;
    if (!this.showVendaForm) {
      this.resetVendaForm();
    }
  }

  getFilteredVendas(): Venda[] {
    let filtered = [...this.vendas];

    if (this.searchTerm) {
      filtered = filtered.filter(venda => 
        venda.clienteNome.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        venda.id?.toString().includes(this.searchTerm)
      );
    }

    if (this.statusFilter) {
      filtered = filtered.filter(venda => venda.status === this.statusFilter);
    }

    if (this.eventoFilter) {
      filtered = filtered.filter(venda => venda.evento === this.eventoFilter);
    }

    if (this.dateFilter) {
      const today = new Date();
      filtered = filtered.filter(venda => {
        const vendaDate = new Date(venda.dataVenda);
        switch (this.dateFilter) {
          case 'hoje':
            return vendaDate.toDateString() === today.toDateString();
          case 'semana':
            const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
            return vendaDate >= weekAgo;
          case 'mes':
            return vendaDate.getMonth() === today.getMonth() && 
                   vendaDate.getFullYear() === today.getFullYear();
          default:
            return true;
        }
      });
    }

    // Ordenação por preço
    if (this.orderByPrice) {
      filtered.sort((a, b) => {
        if (this.orderByPrice === 'asc') {
          return a.total - b.total;
        } else if (this.orderByPrice === 'desc') {
          return b.total - a.total;
        }
        return 0;
      });
    }

    return filtered;
  }

  limparFiltros(): void {
    this.searchTerm = '';
    this.dateFilter = '';
    this.statusFilter = '';
    this.eventoFilter = '';
    this.orderByPrice = '';
    this.activeFilterBox = '';
    this.currentPage = 1;
  }

  getPaginatedVendas(): Venda[] {
    const filtered = this.getFilteredVendas();
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return filtered.slice(startIndex, startIndex + this.itemsPerPage);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredVendas().length / this.itemsPerPage);
  }

  goToPage(page: number): void {
    this.currentPage = page;
  }

  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarData(data: Date): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(data));
  }

  // ===== MÉTODOS DE CLIENTE =====
  
  onCpfFocus(): void {
    // Mostrar opções de cliente ao focar no campo CPF
    this.showOpcoesCliente = true;
  }
  
  async buscarClientePorCpf(): Promise<void> {
    if (!this.cpfBusca || this.cpfBusca.trim() === '') {
      this.clienteAtual = null;
      this.clienteEncontrado = false;
      return;
    }

    // Permitir CPF fake para vendas sem cadastro
    if (this.cpfBusca === '000.000.000-00') {
      this.clienteAtual = {
        id: 0,
        nome: 'Cliente não identificado',
        email: '',
        telefone: '',
        cpf: '000.000.000-00',
        endereco: '',
        dataCadastro: new Date().toISOString(),
        ativo: true,
        pontos: 0
      };
      this.clienteEncontrado = true;
      this.preencherDadosCliente();
      return;
    }

    this.buscandoCliente = true;
    try {
      this.clienteService.buscarPorCpf(this.cpfBusca).subscribe({
        next: (cliente) => {
          if (cliente) {
            this.clienteAtual = cliente;
            this.clienteEncontrado = true;
            this.preencherDadosCliente();
            this.successMessage = `Cliente encontrado: ${cliente.nome}`;
          } else {
            this.clienteAtual = null;
            this.clienteEncontrado = false;
            this.errorMessage = 'Cliente não encontrado. Deseja cadastrar um novo cliente?';
          }
          this.buscandoCliente = false;
        },
        error: (error: any) => {
          this.errorMessage = 'Erro ao buscar cliente.';
          this.clienteAtual = null;
          this.clienteEncontrado = false;
          this.buscandoCliente = false;
        }
      });
    } catch (error) {
      this.errorMessage = 'Erro ao buscar cliente.';
      this.clienteAtual = null;
      this.clienteEncontrado = false;
      this.buscandoCliente = false;
    }
  }

  preencherDadosCliente(): void {
    if (this.clienteAtual) {
      this.vendaForm.patchValue({
        clienteNome: this.clienteAtual.nome,
        clienteEmail: this.clienteAtual.email,
        clienteTelefone: this.clienteAtual.telefone
      });
    }
  }

  abrirCadastroCliente(): void {
    this.showCadastroCliente = true;
    this.showOpcoesCliente = false; // Esconder opções ao abrir cadastro
    // Pré-preencher CPF se foi buscado
    if (this.cpfBusca && this.cpfBusca !== '000.000.000-00') {
      this.clienteForm.patchValue({
        cpf: this.cpfBusca
      });
    }
  }

  fecharCadastroCliente(): void {
    this.showCadastroCliente = false;
    this.clienteForm.reset();
  }

  async salvarNovoCliente(): Promise<void> {
    if (this.clienteForm.valid) {
      try {
        this.clienteService.criarCliente(this.clienteForm.value).subscribe({
          next: (novoCliente) => {
            this.clienteAtual = novoCliente;
            this.clienteEncontrado = true;
            this.cpfBusca = novoCliente.cpf || '';
            this.preencherDadosCliente();
            this.fecharCadastroCliente();
            this.successMessage = `Cliente ${novoCliente.nome} cadastrado com sucesso!`;
          },
          error: (error: any) => {
            this.errorMessage = 'Erro ao cadastrar cliente.';
          }
        });
      } catch (error) {
        this.errorMessage = 'Erro ao cadastrar cliente.';
      }
    }
  }

  usarCpfFake(): void {
    this.cpfBusca = '000.000.000-00';
    this.showOpcoesCliente = false; // Esconder opções ao usar CPF fake
    this.buscarClientePorCpf();
  }

  limparCliente(): void {
    this.clienteAtual = null;
    this.clienteEncontrado = false;
    this.cpfBusca = '';
    this.showOpcoesCliente = false; // Resetar opções ao limpar cliente
    this.vendaForm.patchValue({
      clienteNome: '',
      clienteEmail: '',
      clienteTelefone: ''
    });
  }

  formatarCpf(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 11) {
      value = value.replace(/(\d{3})(\d)/, '$1.$2');
      value = value.replace(/(\d{3})(\d)/, '$1.$2');
      value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    }
    
    event.target.value = value;
    this.cpfBusca = value;
  }

  formatarTelefone(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 11) {
      if (value.length <= 10) {
        value = value.replace(/(\d{2})(\d)/, '($1) $2');
        value = value.replace(/(\d{4})(\d)/, '$1-$2');
      } else {
        value = value.replace(/(\d{2})(\d)/, '($1) $2');
        value = value.replace(/(\d{5})(\d)/, '$1-$2');
      }
    }
    
    event.target.value = value;
  }

  formatarCep(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 8) {
      value = value.replace(/(\d{5})(\d)/, '$1-$2');
    }
    
    event.target.value = value;
  }

  // ===== FIM MÉTODOS DE CLIENTE =====

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDENTE':
        return 'bg-warning text-dark';
      case 'CONFIRMADA':
        return 'bg-info text-white';
      case 'ENTREGUE':
        return 'bg-success text-white';
      case 'CANCELADA':
        return 'bg-danger text-white';
      default:
        return 'bg-secondary text-white';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'PENDENTE':
        return 'Pendente';
      case 'CONFIRMADA':
        return 'Confirmada';
      case 'ENTREGUE':
        return 'Entregue';
      case 'CANCELADA':
        return 'Cancelada';
      default:
        return status;
    }
  }

  getFormaPagamentoText(forma: string): string {
    switch (forma) {
      case 'DINHEIRO':
        return 'Dinheiro';
      case 'CARTAO_CREDITO':
        return 'Cartão de Crédito';
      case 'CARTAO_DEBITO':
        return 'Cartão de Débito';
      case 'PIX':
        return 'PIX';
      case 'TRANSFERENCIA':
        return 'Transferência';
      case 'BOLETO':
        return 'Boleto';
      default:
        return forma;
    }
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  getErrorMessage(fieldName: string): string {
    const control = this.vendaForm.get(fieldName);
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return 'Este campo é obrigatório';
      }
      if (control.errors['minlength']) {
        return `Mínimo de ${control.errors['minlength'].requiredLength} caracteres`;
      }
      if (control.errors['email']) {
        return 'E-mail inválido';
      }
      if (control.errors['min']) {
        if (fieldName === 'quantidade') {
          return 'Quantidade deve ser pelo menos 1 unidade';
        }
        if (fieldName === 'precoUnitario') {
          return 'Preço deve ser maior que R$ 0,00';
        }
        if (fieldName === 'desconto' || fieldName === 'percentualDesconto') {
          return 'Valor não pode ser negativo';
        }
        return `Valor mínimo: ${control.errors['min'].min}`;
      }
      if (control.errors['max']) {
        if (fieldName === 'percentualDesconto') {
          return 'Desconto não pode ser maior que 100%';
        }
        return `Valor máximo: ${control.errors['max'].max}`;
      }
    }
    return '';
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  // Métodos para ações da lista de vendas
  visualizarVenda(venda: Venda): void {
    const modal = this.createVendaModal(venda, 'visualizar');
    document.body.appendChild(modal);
    
    // Aplicar estilos do Bootstrap modal
    modal.style.display = 'block';
    modal.classList.add('show');
    document.body.classList.add('modal-open');
    
    // Criar backdrop
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop fade show';
    document.body.appendChild(backdrop);
    
    // Evento para fechar modal
    const closeModal = () => {
      modal.remove();
      backdrop.remove();
      document.body.classList.remove('modal-open');
    };
    
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal();
    });
    
    modal.querySelector('.btn-close')?.addEventListener('click', closeModal);
    modal.querySelector('.btn-secondary')?.addEventListener('click', closeModal);
  }

  editarVenda(venda: Venda): void {
    const modal = this.createVendaModal(venda, 'editar');
    document.body.appendChild(modal);
    
    // Aplicar estilos do Bootstrap modal
    modal.style.display = 'block';
    modal.classList.add('show');
    document.body.classList.add('modal-open');
    
    // Criar backdrop
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop fade show';
    document.body.appendChild(backdrop);
    
    // Evento para fechar modal
    const closeModal = () => {
      modal.remove();
      backdrop.remove();
      document.body.classList.remove('modal-open');
      
      // Limpar dados de edição
      this.vendaEdicao = null;
      this.itensEdicao = [];
      
      // Limpar instância global
      delete (window as any).vendaComponentInstance;
    };
    
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal();
    });
    
    modal.querySelector('.btn-close')?.addEventListener('click', closeModal);
    modal.querySelector('#btnFechar')?.addEventListener('click', closeModal);
  }

  // Método para alterar status de venda
  async alterarStatusVenda(venda: Venda, novoStatus: string, observacoes?: string): Promise<void> {
    try {
      if (!venda.id) {
        this.errorMessage = 'ID da venda não encontrado.';
        return;
      }

      const vendaAtualizada = await this.vendaService.atualizarStatusVenda(venda.id, novoStatus, observacoes).toPromise();
      
      if (vendaAtualizada) {
        // Atualizar a venda na lista
        const index = this.vendas.findIndex(v => v.id === venda.id);
        if (index !== -1) {
          this.vendas[index] = { ...this.vendas[index], status: novoStatus as any };
        }
        
        this.successMessage = `Status da venda alterado para ${this.getStatusText(novoStatus)} com sucesso!`;
        setTimeout(() => this.clearMessages(), 5000);
      }
    } catch (error) {
      console.error('Erro ao alterar status da venda:', error);
      this.errorMessage = 'Erro ao alterar status da venda.';
      setTimeout(() => this.clearMessages(), 5000);
    }
  }

  // Método para exibir modal de alteração de status
  mostrarModalAlterarStatus(venda: Venda): void {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-arrow-repeat me-2"></i>
              Alterar Status da Venda #${venda.id}
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-semibold">Status Atual:</label>
              <p class="form-control-plaintext">
                <span class="badge ${this.getStatusBadgeClass(venda.status)}">
                  ${this.getStatusText(venda.status)}
                </span>
              </p>
            </div>
            
            <div class="mb-3">
              <label for="novoStatus" class="form-label fw-semibold">Novo Status:</label>
              <select class="form-select" id="novoStatus">
                ${this.statusVenda.map(status => `
                  <option value="${status}" ${status === venda.status ? 'selected' : ''}>
                    ${this.getStatusText(status)}
                  </option>
                `).join('')}
              </select>
            </div>
            
            <div class="mb-3">
              <label for="observacoesStatus" class="form-label fw-semibold">Observações (opcional):</label>
              <textarea class="form-control" id="observacoesStatus" rows="3" 
                        placeholder="Informe o motivo da alteração do status..."></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="button" class="btn btn-primary" id="btnConfirmarStatus">
              <i class="bi bi-check-circle me-1"></i>Alterar Status
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Configurar eventos
    const btnConfirmar = modal.querySelector('#btnConfirmarStatus') as HTMLButtonElement;
    const selectStatus = modal.querySelector('#novoStatus') as HTMLSelectElement;
    const textareaObservacoes = modal.querySelector('#observacoesStatus') as HTMLTextAreaElement;

    btnConfirmar.addEventListener('click', async () => {
      const novoStatus = selectStatus.value;
      const observacoes = textareaObservacoes.value.trim();

      if (novoStatus === venda.status) {
        alert('O status selecionado é o mesmo status atual da venda.');
        return;
      }

      btnConfirmar.disabled = true;
      btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Alterando...';

      await this.alterarStatusVenda(venda, novoStatus, observacoes);

      // Fechar modal
      modal.remove();
      document.querySelector('.modal-backdrop')?.remove();
      document.body.classList.remove('modal-open');
    });

    // Mostrar modal
    const bsModal = new (window as any).bootstrap.Modal(modal);
    bsModal.show();

    // Limpar modal ao fechar
    modal.addEventListener('hidden.bs.modal', () => {
      modal.remove();
    });
  }

  imprimirVenda(venda: Venda): void {
    const printWindow = window.open('', '_blank');
    
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>Comprovante de Venda #${venda.id}</title>
            <style>
              body { 
                font-family: Arial, sans-serif; 
                margin: 0; 
                padding: 20px; 
                color: #333;
              }
              .header { 
                text-align: center; 
                margin-bottom: 30px; 
                border-bottom: 2px solid #ff6b35;
                padding-bottom: 20px;
              }
              .logo { 
                font-size: 28px; 
                font-weight: bold; 
                color: #ff6b35; 
                margin-bottom: 10px;
              }
              .company-info { 
                color: #666; 
                font-size: 14px;
                margin-bottom: 5px;
              }
              .venda-info { 
                display: flex; 
                justify-content: space-between; 
                margin: 20px 0; 
                background: #f8f9fa;
                padding: 15px;
                border-radius: 5px;
              }
              .info-group { 
                flex: 1; 
              }
              .info-label { 
                font-weight: bold; 
                color: #ff6b35; 
                font-size: 12px;
                text-transform: uppercase;
                margin-bottom: 5px;
              }
              .info-value { 
                font-size: 14px; 
                margin-bottom: 10px;
              }
              table { 
                width: 100%; 
                border-collapse: collapse; 
                margin: 20px 0; 
              }
              th, td { 
                border: 1px solid #ddd; 
                padding: 10px; 
                text-align: left; 
              }
              th { 
                background-color: #ff6b35; 
                color: white; 
                font-weight: bold; 
              }
              .text-right { text-align: right; }
              .total-section { 
                margin-top: 20px; 
                text-align: right;
                background: #f8f9fa;
                padding: 15px;
                border-radius: 5px;
              }
              .total-line { 
                display: flex; 
                justify-content: space-between; 
                margin: 5px 0; 
                padding: 5px 0;
              }
              .total-final { 
                font-size: 18px; 
                font-weight: bold; 
                color: #ff6b35;
                border-top: 2px solid #ff6b35;
                padding-top: 10px;
                margin-top: 10px;
              }
              .footer { 
                margin-top: 40px; 
                text-align: center; 
                color: #666; 
                font-size: 12px;
                border-top: 1px solid #ddd;
                padding-top: 20px;
              }
              @media print { 
                .no-print { display: none; }
                body { margin: 0; padding: 15px; }
              }
            </style>
          </head>
          <body>
            <div class="header">
              <div class="logo">Sistema de Estoque e Vendas</div>
              <div class="company-info">CNPJ: 12.345.678/0001-90</div>
              <div class="company-info">Endereço: Rua das Vendas, 123 - Centro - São Paulo/SP</div>
              <div class="company-info">Telefone: (11) 3333-4444 | Email: contato@sistema.com</div>
            </div>
            
            <div class="venda-info">
              <div class="info-group">
                <div class="info-label">Número da Venda</div>
                <div class="info-value">#${venda.id}</div>
                
                <div class="info-label">Data/Hora</div>
                <div class="info-value">${this.formatarData(venda.dataVenda)}</div>
                
                <div class="info-label">Status</div>
                <div class="info-value">${this.getStatusText(venda.status)}</div>
              </div>
              
              <div class="info-group">
                <div class="info-label">Cliente</div>
                <div class="info-value">${venda.clienteNome}</div>
                
                ${venda.clienteEmail ? `
                  <div class="info-label">E-mail</div>
                  <div class="info-value">${venda.clienteEmail}</div>
                ` : ''}
                
                ${venda.clienteTelefone ? `
                  <div class="info-label">Telefone</div>
                  <div class="info-value">${venda.clienteTelefone}</div>
                ` : ''}
              </div>
              
              <div class="info-group">
                <div class="info-label">Forma de Pagamento</div>
                <div class="info-value">${this.getFormaPagamentoText(venda.formaPagamento)}</div>
                
                <div class="info-label">Vendedor</div>
                <div class="info-value">Administrador</div>
              </div>
            </div>
            
            <table>
              <thead>
                <tr>
                  <th style="width: 50%">Produto</th>
                  <th style="width: 15%; text-align: center">Qtd</th>
                  <th style="width: 17.5%; text-align: right">Preço Unit.</th>
                  <th style="width: 17.5%; text-align: right">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                ${venda.itens.map(item => `
                  <tr>
                    <td>${item.produtoNome}</td>
                    <td style="text-align: center">${item.quantidade}</td>
                    <td style="text-align: right">${this.formatarMoeda(item.precoUnitario)}</td>
                    <td style="text-align: right">${this.formatarMoeda(item.subtotal)}</td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
            
            <div class="total-section">
              <div class="total-line">
                <span>Subtotal:</span>
                <span>${this.formatarMoeda(venda.subtotal)}</span>
              </div>
              ${venda.desconto > 0 ? `
                <div class="total-line">
                  <span>Desconto:</span>
                  <span>- ${this.formatarMoeda(venda.desconto)}</span>
                </div>
              ` : ''}
              <div class="total-line total-final">
                <span>TOTAL:</span>
                <span>${this.formatarMoeda(venda.total)}</span>
              </div>
            </div>
            
            ${venda.observacoes ? `
              <div style="margin-top: 20px;">
                <div class="info-label">Observações:</div>
                <div style="padding: 10px; background: #f8f9fa; border-radius: 5px; margin-top: 5px;">
                  ${venda.observacoes}
                </div>
              </div>
            ` : ''}
            
            <div class="footer">
              <p><strong>Obrigado pela preferência!</strong></p>
              <p>Comprovante emitido em ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')}</p>
              <p>Sistema de Estoque e Vendas - Versão 1.0</p>
            </div>
          </body>
        </html>
      `);
      
      printWindow.document.close();
      
      // Aguardar o carregamento antes de imprimir
      setTimeout(() => {
        printWindow.print();
        printWindow.close();
      }, 500);
    }
  }

  private createVendaModal(venda: Venda, modo: 'visualizar' | 'editar'): HTMLElement {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.setAttribute('tabindex', '-1');
    
    const isEdicao = modo === 'editar';
    
    // Se for edição, inicializar dados de edição
    if (isEdicao) {
      this.vendaEdicao = { ...venda };
      this.itensEdicao = [...venda.itens];
    }
    
    modal.innerHTML = `
      <div class="modal-dialog modal-xl">
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-${isEdicao ? 'pencil' : 'eye'} me-2"></i>
              ${isEdicao ? 'Editar' : 'Visualizar'} Venda #${venda.id}
            </h5>
            <button type="button" class="btn-close btn-close-white"></button>
          </div>
          
          <div class="modal-body" style="max-height: 70vh; overflow-y: auto;">
            <!-- Informações do Cliente -->
            <div class="row mb-3">
              <div class="col-12">
                <h6 class="text-primary border-bottom pb-2">
                  <i class="bi bi-person me-2"></i>Dados do Cliente
                </h6>
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">Nome:</label>
                ${isEdicao ? `
                  <input type="text" class="form-control" id="clienteNome" value="${venda.clienteNome}">
                ` : `
                  <p class="form-control-plaintext">${venda.clienteNome}</p>
                `}
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">E-mail:</label>
                ${isEdicao ? `
                  <input type="email" class="form-control" id="clienteEmail" value="${venda.clienteEmail || ''}">
                ` : `
                  <p class="form-control-plaintext">${venda.clienteEmail || 'Não informado'}</p>
                `}
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">Telefone:</label>
                ${isEdicao ? `
                  <input type="tel" class="form-control" id="clienteTelefone" value="${venda.clienteTelefone || ''}">
                ` : `
                  <p class="form-control-plaintext">${venda.clienteTelefone || 'Não informado'}</p>
                `}
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">Status:</label>
                ${isEdicao ? `
                  <select class="form-select" id="status">
                    ${this.statusVenda.map(status => `
                      <option value="${status}" ${venda.status === status ? 'selected' : ''}>
                        ${this.getStatusText(status)}
                      </option>
                    `).join('')}
                  </select>
                ` : `
                  <p class="form-control-plaintext">
                    <span class="badge ${this.getStatusBadgeClass(venda.status)}">
                      ${this.getStatusText(venda.status)}
                    </span>
                  </p>
                `}
              </div>
            </div>
            
            <!-- Itens da Venda -->
            <div class="row mb-3">
              <div class="col-12">
                <h6 class="text-primary border-bottom pb-2">
                  <i class="bi bi-basket me-2"></i>Itens da Venda
                  ${isEdicao ? `
                    <button type="button" class="btn btn-success btn-sm float-end" id="btnAdicionarItem">
                      <i class="bi bi-plus-circle me-1"></i>Adicionar Item
                    </button>
                  ` : ''}
                </h6>
                
                ${isEdicao ? `
                  <!-- Área para adicionar novo item -->
                  <div class="card card-body bg-light mb-3" id="areaAdicionarItem" style="display: none;">
                    <div class="row align-items-end">
                      <div class="col-md-5">
                        <label class="form-label fw-semibold">Produto:</label>
                        <select class="form-select" id="novoProdutoSelect">
                          <option value="">Selecione um produto</option>
                          ${this.produtos.map(produto => `
                            <option value="${produto.id}" data-preco="${produto.preco}" data-estoque="${produto.quantidade}">
                              ${produto.nome} - ${this.formatarMoeda(produto.preco)} (Estoque: ${produto.quantidade})
                            </option>
                          `).join('')}
                        </select>
                      </div>
                      <div class="col-md-2">
                        <label class="form-label fw-semibold">Quantidade:</label>
                        <input type="number" class="form-control" id="novaQuantidadeInput" min="1" value="1">
                      </div>
                      <div class="col-md-3">
                        <label class="form-label fw-semibold">Preço Unit.:</label>
                        <input type="number" class="form-control" id="novoPrecoInput" step="0.01" min="0" readonly>
                      </div>
                      <div class="col-md-2">
                        <button type="button" class="btn btn-primary btn-sm w-100" id="confirmarAdicionarItem">
                          <i class="bi bi-check"></i> Confirmar
                        </button>
                      </div>
                    </div>
                  </div>
                ` : ''}
                
                <div class="table-responsive">
                  <table class="table table-sm" id="tabelaItens">
                    <thead class="table-light">
                      <tr>
                        <th>Produto</th>
                        <th class="text-center">Qtd</th>
                        <th class="text-end">Preço Unit.</th>
                        <th class="text-end">Subtotal</th>
                        ${isEdicao ? '<th class="text-center">Ações</th>' : ''}
                      </tr>
                    </thead>
                    <tbody id="tbodyItens">
                      ${this.renderizarItensTabela(venda.itens, isEdicao)}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
            
            <!-- Resumo Financeiro -->
            <div class="row mb-3">
              <div class="col-12">
                <h6 class="text-primary border-bottom pb-2">
                  <i class="bi bi-calculator me-2"></i>Resumo Financeiro
                </h6>
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">Forma de Pagamento:</label>
                ${isEdicao ? `
                  <select class="form-select" id="formaPagamento">
                    ${this.formasPagamento.map(forma => `
                      <option value="${forma}" ${venda.formaPagamento === forma ? 'selected' : ''}>
                        ${this.getFormaPagamentoText(forma)}
                      </option>
                    `).join('')}
                  </select>
                ` : `
                  <p class="form-control-plaintext">${this.getFormaPagamentoText(venda.formaPagamento)}</p>
                `}
              </div>
              
              <div class="col-md-6">
                <label class="form-label fw-semibold">Data/Hora:</label>
                <p class="form-control-plaintext">${this.formatarData(venda.dataVenda)}</p>
              </div>
              
              ${isEdicao ? `
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Desconto (%):</label>
                  <input type="number" class="form-control" id="percentualDesconto" 
                         min="0" max="100" step="0.01" value="${venda.percentualDesconto || 0}">
                </div>
              ` : ''}
              
              <div class="col-12">
                <div class="bg-light p-3 rounded" id="resumoFinanceiro">
                  ${this.renderizarResumoFinanceiro(venda)}
                </div>
              </div>
            </div>
            
            <!-- Observações -->
            ${venda.observacoes || isEdicao ? `
              <div class="row">
                <div class="col-12">
                  <label class="form-label fw-semibold">Observações:</label>
                  ${isEdicao ? `
                    <textarea class="form-control" id="observacoes" rows="3">${venda.observacoes || ''}</textarea>
                  ` : `
                    <p class="form-control-plaintext">${venda.observacoes || 'Nenhuma observação'}</p>
                  `}
                </div>
              </div>
            ` : ''}
          </div>
          
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" id="btnFechar">
              <i class="bi bi-x-circle me-1"></i>Fechar
            </button>
            ${isEdicao ? `
              <button type="button" class="btn btn-primary" id="btnSalvar">
                <i class="bi bi-check-circle me-1"></i>Salvar Alterações
              </button>
            ` : ''}
          </div>
        </div>
      </div>
    `;
    
    // Configurar eventos se for edição
    if (isEdicao) {
      this.configurarEventosEdicao(modal);
    }
    
    return modal;
  }

  // Métodos auxiliares para edição de vendas
  private renderizarItensTabela(itens: ItemVenda[], isEdicao: boolean): string {
    return itens.map((item, index) => `
      <tr>
        <td>${item.produtoNome}</td>
        <td class="text-center">
          ${isEdicao ? `
            <input type="number" class="form-control form-control-sm text-center" 
                   value="${item.quantidade}" min="1" 
                   onchange="window.vendaComponentInstance.atualizarQuantidadeItem(${index}, this.value)"
                   style="width: 80px; margin: 0 auto;">
          ` : item.quantidade}
        </td>
        <td class="text-end">${this.formatarMoeda(item.precoUnitario)}</td>
        <td class="text-end fw-semibold">${this.formatarMoeda(item.subtotal)}</td>
        ${isEdicao ? `
          <td class="text-center">
            <button type="button" class="btn btn-danger btn-sm" 
                    onclick="window.vendaComponentInstance.removerItem(${index})"
                    title="Remover item">
              <i class="bi bi-trash"></i>
            </button>
          </td>
        ` : ''}
      </tr>
    `).join('');
  }

  private renderizarResumoFinanceiro(venda: Venda): string {
    const subtotal = this.vendaEdicao ? this.calcularSubtotalEdicao() : venda.subtotal;
    const percentualDesconto = this.vendaEdicao?.percentualDesconto || 0;
    const desconto = (subtotal * percentualDesconto) / 100;
    const total = subtotal - desconto;

    return `
      <div class="d-flex justify-content-between mb-2">
        <span>Subtotal:</span>
        <span class="fw-semibold" id="subtotalValue">${this.formatarMoeda(subtotal)}</span>
      </div>
      ${desconto > 0 ? `
        <div class="d-flex justify-content-between mb-2">
          <span>Desconto (${percentualDesconto}%):</span>
          <span class="text-danger" id="descontoValue">- ${this.formatarMoeda(desconto)}</span>
        </div>
      ` : ''}
      <div class="d-flex justify-content-between border-top pt-2">
        <span class="fw-bold fs-5">Total:</span>
        <span class="fw-bold fs-5 text-success" id="totalValue">${this.formatarMoeda(total)}</span>
      </div>
    `;
  }

  private configurarEventosEdicao(modal: HTMLElement): void {
    // Expor instância global para callbacks inline
    (window as any).vendaComponentInstance = this;

    // Evento para mostrar/ocultar área de adicionar item
    const btnAdicionarItem = modal.querySelector('#btnAdicionarItem');
    const areaAdicionarItem = modal.querySelector('#areaAdicionarItem') as HTMLElement;
    
    btnAdicionarItem?.addEventListener('click', () => {
      areaAdicionarItem.style.display = areaAdicionarItem.style.display === 'none' ? 'block' : 'none';
    });

    // Evento para atualizar preço quando selecionar produto
    const produtoSelect = modal.querySelector('#novoProdutoSelect') as HTMLSelectElement;
    const precoInput = modal.querySelector('#novoPrecoInput') as HTMLInputElement;
    
    produtoSelect?.addEventListener('change', () => {
      const selectedOption = produtoSelect.selectedOptions[0];
      if (selectedOption) {
        const preco = selectedOption.getAttribute('data-preco');
        precoInput.value = preco || '0';
      }
    });

    // Evento para confirmar adição de item
    const btnConfirmarAdicionar = modal.querySelector('#confirmarAdicionarItem');
    btnConfirmarAdicionar?.addEventListener('click', () => {
      this.adicionarNovoItem(modal);
    });

    // Evento para atualizar desconto
    const percentualDescontoInput = modal.querySelector('#percentualDesconto') as HTMLInputElement;
    percentualDescontoInput?.addEventListener('input', () => {
      this.atualizarResumoFinanceiro(modal);
    });

    // Evento para salvar alterações
    const btnSalvar = modal.querySelector('#btnSalvar');
    btnSalvar?.addEventListener('click', () => {
      this.salvarEdicaoVendaCompleta(modal);
    });
  }

  private calcularSubtotalEdicao(): number {
    if (!this.itensEdicao || this.itensEdicao.length === 0) {
      return 0;
    }
    return this.itensEdicao.reduce((total, item) => total + item.subtotal, 0);
  }

  private atualizarResumoFinanceiro(modal: HTMLElement): void {
    if (!this.vendaEdicao) return;

    const percentualDescontoInput = modal.querySelector('#percentualDesconto') as HTMLInputElement;
    const percentualDesconto = parseFloat(percentualDescontoInput?.value || '0');
    
    this.vendaEdicao.percentualDesconto = percentualDesconto;
    
    const subtotal = this.calcularSubtotalEdicao();
    const desconto = (subtotal * percentualDesconto) / 100;
    const total = subtotal - desconto;

    // Atualizar valores na tela
    const subtotalElement = modal.querySelector('#subtotalValue');
    const descontoElement = modal.querySelector('#descontoValue');
    const totalElement = modal.querySelector('#totalValue');

    if (subtotalElement) subtotalElement.textContent = this.formatarMoeda(subtotal);
    if (totalElement) totalElement.textContent = this.formatarMoeda(total);
    
    if (desconto > 0 && descontoElement) {
      descontoElement.textContent = `- ${this.formatarMoeda(desconto)}`;
    }
  }

  atualizarQuantidadeItem(index: number, novaQuantidade: string): void {
    const quantidade = parseInt(novaQuantidade);
    if (quantidade > 0 && this.itensEdicao[index]) {
      this.itensEdicao[index].quantidade = quantidade;
      this.itensEdicao[index].subtotal = quantidade * this.itensEdicao[index].precoUnitario;
      
      // Atualizar a tabela e resumo financeiro
      this.atualizarTabelaItens();
      this.atualizarResumoFinanceiroModal();
    }
  }

  removerItem(index: number): void {
    if (this.itensEdicao.length > 1) {
      this.itensEdicao.splice(index, 1);
      this.atualizarTabelaItens();
      this.atualizarResumoFinanceiroModal();
    } else {
      this.errorMessage = 'Uma venda deve ter pelo menos um item.';
      setTimeout(() => this.clearMessages(), 3000);
    }
  }

  private adicionarNovoItem(modal: HTMLElement): void {
    const produtoSelect = modal.querySelector('#novoProdutoSelect') as HTMLSelectElement;
    const quantidadeInput = modal.querySelector('#novaQuantidadeInput') as HTMLInputElement;
    const precoInput = modal.querySelector('#novoPrecoInput') as HTMLInputElement;

    const produtoId = parseInt(produtoSelect.value);
    const quantidade = parseInt(quantidadeInput.value);
    const preco = parseFloat(precoInput.value);

    if (!produtoId || quantidade <= 0 || preco <= 0) {
      this.errorMessage = 'Preencha todos os campos corretamente.';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }

    // Verificar se o produto já existe na venda
    const itemExistente = this.itensEdicao.find(item => item.produtoId === produtoId);
    if (itemExistente) {
      itemExistente.quantidade += quantidade;
      itemExistente.subtotal = itemExistente.quantidade * itemExistente.precoUnitario;
    } else {
      const produto = this.produtos.find(p => p.id === produtoId);
      if (produto) {
        const novoItem: ItemVenda = {
          produtoId: produtoId,
          produtoNome: produto.nome,
          quantidade: quantidade,
          precoUnitario: preco,
          desconto: 0,
          percentualDesconto: 0,
          subtotal: quantidade * preco,
          totalComDesconto: quantidade * preco
        };
        this.itensEdicao.push(novoItem);
      }
    }

    // Limpar formulário de adição
    produtoSelect.value = '';
    quantidadeInput.value = '1';
    precoInput.value = '';

    // Ocultar área de adição
    const areaAdicionarItem = modal.querySelector('#areaAdicionarItem') as HTMLElement;
    areaAdicionarItem.style.display = 'none';

    // Atualizar tabela e resumo
    this.atualizarTabelaItens();
    this.atualizarResumoFinanceiroModal();
  }

  private atualizarTabelaItens(): void {
    const tbody = document.querySelector('#tbodyItens');
    if (tbody && this.itensEdicao) {
      tbody.innerHTML = this.renderizarItensTabela(this.itensEdicao, true);
    }
  }

  private atualizarResumoFinanceiroModal(): void {
    const resumoElement = document.querySelector('#resumoFinanceiro');
    if (resumoElement && this.vendaEdicao) {
      resumoElement.innerHTML = this.renderizarResumoFinanceiro(this.vendaEdicao);
    }
  }

  private salvarEdicaoVendaCompleta(modal: HTMLElement): void {
    try {
      if (!this.vendaEdicao) return;

      // Coletar dados do modal
      const clienteNome = (modal.querySelector('#clienteNome') as HTMLInputElement)?.value;
      const clienteEmail = (modal.querySelector('#clienteEmail') as HTMLInputElement)?.value;
      const clienteTelefone = (modal.querySelector('#clienteTelefone') as HTMLInputElement)?.value;
      const status = (modal.querySelector('#status') as HTMLSelectElement)?.value;
      const formaPagamento = (modal.querySelector('#formaPagamento') as HTMLSelectElement)?.value;
      const observacoes = (modal.querySelector('#observacoes') as HTMLTextAreaElement)?.value;
      const percentualDesconto = parseFloat((modal.querySelector('#percentualDesconto') as HTMLInputElement)?.value || '0');

      // Calcular valores finais
      const subtotal = this.calcularSubtotalEdicao();
      const desconto = (subtotal * percentualDesconto) / 100;
      const total = subtotal - desconto;

      // Atualizar venda na lista
      const vendaIndex = this.vendas.findIndex(v => v.id === this.vendaEdicao!.id);
      if (vendaIndex !== -1) {
        this.vendas[vendaIndex] = {
          ...this.vendas[vendaIndex],
          clienteNome: clienteNome || this.vendaEdicao.clienteNome,
          clienteEmail: clienteEmail || this.vendaEdicao.clienteEmail,
          clienteTelefone: clienteTelefone || this.vendaEdicao.clienteTelefone,
          status: status || this.vendaEdicao.status,
          formaPagamento: formaPagamento || this.vendaEdicao.formaPagamento,
          observacoes: observacoes || this.vendaEdicao.observacoes,
          percentualDesconto: percentualDesconto,
          itens: [...this.itensEdicao],
          subtotal: subtotal,
          desconto: desconto,
          total: total
        };
        
        this.successMessage = 'Venda atualizada com sucesso!';
        
        // Fechar modal
        modal.remove();
        document.querySelector('.modal-backdrop')?.remove();
        document.body.classList.remove('modal-open');
        
        // Limpar dados de edição
        this.vendaEdicao = null;
        this.itensEdicao = [];
        
        // Limpar mensagem após 5 segundos
        setTimeout(() => {
          this.clearMessages();
        }, 5000);
      }
    } catch (error) {
      console.error('Erro ao salvar edição:', error);
      this.errorMessage = 'Erro ao salvar alterações da venda.';
    }
  }

  // Métodos para filtros por boxes
  aplicarFiltroVendasHoje(): void {
    if (this.activeFilterBox === 'hoje') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.dateFilter = '';
    } else {
      // Ativa o filtro para vendas de hoje
      this.activeFilterBox = 'hoje';
      this.dateFilter = 'hoje';
    }
  }

  aplicarFiltroVendasMes(): void {
    if (this.activeFilterBox === 'mes') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.dateFilter = '';
    } else {
      // Ativa o filtro para vendas do mês
      this.activeFilterBox = 'mes';
      this.dateFilter = 'mes';
    }
  }

  aplicarFiltroPendentes(): void {
    if (this.activeFilterBox === 'pendentes') {
      // Se já está ativo, desativa o filtro
      this.activeFilterBox = '';
      this.statusFilter = '';
    } else {
      // Ativa o filtro para vendas pendentes
      this.activeFilterBox = 'pendentes';
      this.statusFilter = 'Pendente';
    }
  }

  // Método auxiliar para verificar se um box está ativo
  isBoxActive(filterType: string): boolean {
    return this.activeFilterBox === filterType;
  }

  // Método para limpar todos os filtros
  limparTodosFiltros(): void {
    this.searchTerm = '';
    this.statusFilter = '';
    this.dateFilter = '';
    this.clienteFilter = '';
    this.eventoFilter = '';
    this.precoOrder = '';
    this.activeFilterBox = '';
  }

  // Novo método para filtrar por evento
  aplicarFiltroEvento(evento: string): void {
    this.eventoFilter = evento;
    this.activeFilterBox = `evento-${evento}`;
  }

  // Novo método para ordenar por preço
  aplicarOrdemPreco(ordem: string): void {
    this.precoOrder = ordem;
    this.activeFilterBox = `preco-${ordem}`;
  }

  // Método para obter lista de eventos únicos
  getEventosUnicos(): string[] {
    const eventos = this.vendas
      .map(venda => venda.evento)
      .filter(evento => evento && evento.trim() !== '')
      .filter((evento, index, arr) => arr.indexOf(evento) === index);
    return eventos as string[];
  }

  // Método para aplicar filtro por evento específico
  filtrarPorEvento(evento: string): void {
    this.eventoFilter = evento;
    this.loadVendas(); // Recarregar com filtro
  }

  // Métodos para desconto por produto
  aplicarDescontoProduto(item: ItemVenda, percentual: number): void {
    item.percentualDesconto = percentual;
    item.desconto = (item.subtotal * percentual) / 100;
    item.totalComDesconto = item.subtotal - item.desconto;
    this.calcularTotais();
  }

  aplicarDescontoValorProduto(item: ItemVenda, valor: number): void {
    item.desconto = valor;
    item.percentualDesconto = item.subtotal > 0 ? (valor / item.subtotal) * 100 : 0;
    item.totalComDesconto = item.subtotal - item.desconto;
    this.calcularTotais();
  }

  calcularTotais(): void {
    const subtotal = this.itensEdicao.reduce((sum, item) => sum + item.subtotal, 0);
    const totalDesconto = this.itensEdicao.reduce((sum, item) => sum + item.desconto, 0);
    const total = this.itensEdicao.reduce((sum, item) => sum + item.totalComDesconto, 0);

    // Atualizar campos do formulário se estiver editando
    if (this.vendaEdicao) {
      this.vendaForm.patchValue({
        subtotal: subtotal,
        desconto: totalDesconto,
        total: total
      });
    }
  }

  // Métodos para desconto por item no formulário
  aplicarDescontoPorcentual(index: number, event: any): void {
    const percentual = parseFloat(event.target.value) || 0;
    
    // Validação: percentual deve estar entre 0 e 100
    if (percentual < 0) {
      event.target.value = 0;
      this.errorMessage = 'Desconto não pode ser negativo';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    if (percentual > 100) {
      event.target.value = 100;
      this.errorMessage = 'Desconto não pode ser maior que 100%';
      setTimeout(() => this.clearMessages(), 3000);
      return;
    }
    
    const item = this.itensFormArray.at(index);
    if (item) {
      const subtotal = item.get('subtotal')?.value || 0;
      const desconto = (subtotal * percentual) / 100;
      
      item.patchValue({
        percentualDesconto: percentual,
        desconto: desconto,
        totalComDesconto: subtotal - desconto
      });
      
      this.calcularTotaisFormulario();
    }
  }

  calcularTotalItem(index: number): number {
    const item = this.itensFormArray.at(index);
    if (item) {
      // CORREÇÃO: usar o campo totalComDesconto que já tem o desconto aplicado
      return item.get('totalComDesconto')?.value || 0;
    }
    return 0;
  }

  calcularTotaisFormulario(): void {
    const itens = this.itensFormArray.controls;
    const subtotal = itens.reduce((sum, item) => sum + (item.get('subtotal')?.value || 0), 0);
    const totalDesconto = itens.reduce((sum, item) => sum + (item.get('desconto')?.value || 0), 0);
    const total = itens.reduce((sum, item) => sum + ((item.get('subtotal')?.value || 0) - (item.get('desconto')?.value || 0)), 0);

    this.vendaForm.patchValue({
      subtotal: subtotal,
      desconto: totalDesconto,
      total: total
    });
  }

  // Buscar produto por código de barras
  buscarPorCodigoBarras(codigo: string): void {
    if (!codigo || codigo.trim() === '') return;

    // TODO: Implementar busca por código de barras no backend
    console.log('⚠️ Busca por código de barras precisa ser implementada no backend');
    this.errorMessage = `Funcionalidade de código de barras em desenvolvimento`;
    setTimeout(() => this.errorMessage = '', 3000);
    
    
  }

  // Adicionar produto automaticamente via código de barras
  adicionarProdutoAutomatico(produto: any): void {
    const novoItem = this.createItemFormGroup();
    novoItem.patchValue({
      produtoId: produto.id,
      quantidade: 1,
      precoUnitario: produto.preco,
      subtotal: produto.preco,
      totalComDesconto: produto.preco
    });
    
    this.itensFormArray.push(novoItem);
    this.calculateTotal();
  }

}
