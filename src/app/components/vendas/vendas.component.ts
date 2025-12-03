import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { VendasService, VendaRequest, VendaResponse, EstatisticasVendasResponse, StatusVenda, FormaPagamento } from '../../services/vendas.service';
import { ProdutoService, ProdutoResponse, TamanhoProduto } from '../../services/produto.service';
import { ClienteService, Cliente } from '../../services/cliente.service';
import { EventosService, Evento } from '../../services/eventos.service';
import { AuthService } from '../../services/auth.service';
import { DevolucaoService, DevolucaoRequest, ItemDevolucaoRequest } from '../../services/devolucao.service';



@Component({
  selector: 'app-vendas',
  templateUrl: './vendas.component.html',
  styleUrls: ['./vendas.component.css']
})
export class VendasComponent implements OnInit {
  vendaForm!: FormGroup;
  clienteForm!: FormGroup;
  vendas: VendaResponse[] = [];
  produtos: ProdutoResponse[] = [];

  isLoading = false;
  showVendaForm = false;
  showCadastroCliente = false;
  buscandoCliente = false;
  clienteAtual: Cliente | null = null;
  clienteEncontrado = false;
  showOpcoesCliente = false;

  // Propriedades para devolução/troca
  showModalDevolucao = false;
  showModalTroca = false;
  vendaSelecionada: VendaResponse | null = null;
  devolucaoForm!: FormGroup;
  itensSelecionadosDevolucao: { [itemId: number]: boolean } = {};
  quantidadesDevolucao: { [itemId: number]: number } = {};
  statusQualidadeItens: { [itemId: number]: string } = {};
  descontoItens: { [itemId: number]: number } = {};

  // Arrays para armazenar totais calculados e evitar loops infinitos
  private totaisCalculados: number[] = [];
  private subtotaisCalculados: number[] = [];

  // mensagens
  errorMessage = '';
  successMessage = '';

  // auto-complete helpers
  produtoSearchTerms: { [key: number]: string } = {};
  showDropdown: { [key: number]: boolean } = {};
  filteredProdutos: { [key: number]: ProdutoResponse[] } = {};

  // filtros/pagination
  searchTerm = '';
  dateFilter = '';
  statusFilter = '';
  eventoFilter = '';
  orderByPrice = '';
  activeFilterBox = '';
  currentPage = 1;
  itemsPerPage = 10;

  totalItems = 0;

  // estatísticas (forma simples, aceita ausência de campos)
  stats: EstatisticasVendasResponse & {
    vendasHoje?: number;
    faturamentoHoje?: number;
    vendasMes?: number;
    faturamentoMes?: number;
  } = {
    totalVendas: 0,
    totalFaturamento: 0,
    ticketMedio: 0,
    vendasPendentes: 0,
    vendasConfirmadas: 0,
    vendasCanceladas: 0
  };

  formasPagamento = Object.values(FormaPagamento);
  statusVenda = Object.values(StatusVenda);

  cpfBusca = '';

  eventos: Evento[] = [];
  eventosAtivos: Evento[] = [];
  eventoSelecionado: Evento | null = null;

  // 🔧 Arrays de tamanhos para cada item
  tamanhosDisponiveis: { [itemIndex: number]: TamanhoProduto[] } = {};

  constructor(
    private fb: FormBuilder,
    private vendasService: VendasService,
    private produtoService: ProdutoService,
    private clienteService: ClienteService,
    private eventosService: EventosService,
    private cdr: ChangeDetectorRef,
    public authService: AuthService,
    private devolucaoService: DevolucaoService
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadProdutos();
    this.loadVendas();
    this.loadStats();
    this.carregarEventosAtivos();
  }

  initializeForms(): void {
    this.vendaForm = this.fb.group({
      clienteNome: ['', [Validators.required, Validators.minLength(2)]],
      clienteEmail: ['', [Validators.email]],
      clienteTelefone: [''],
      evento: [''],
      eventoId: [''],
      formaPagamento: [FormaPagamento.DINHEIRO, Validators.required],
      pagamentoParcelado: [false],
      numeroParcelas: [1, [Validators.min(1), Validators.max(12)]],
      jurosPercentual: [0],
      percentualDesconto: [0],
      desconto: [0],
      observacoes: [''],
      itens: this.fb.array([this.createItemFormGroup()])
    });

    this.clienteForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      cpf: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required]],
      endereco: this.fb.group({
        cep: ['', Validators.required],
        rua: ['', Validators.required],
        numero: ['', Validators.required],
        complemento: [''],
        bairro: ['', Validators.required],
        cidade: ['', Validators.required],
        estado: ['', Validators.required]
      })
    });

    this.devolucaoForm = this.fb.group({
      tipoDevolucao: ['', Validators.required],
      motivo: ['', Validators.required],
      observacoes: ['']
    });
  }

  createItemFormGroup(): FormGroup {
    return this.fb.group({
      produtoId: ['', Validators.required],
      produtoNome: [''], // 🆕 Campo para exibir o nome do produto
      quantidade: [1, [Validators.required, Validators.min(1)]],
      precoUnitario: [0, [Validators.required, Validators.min(0.01)]],
      subtotal: [{ value: 0, disabled: true }],
      percentualDesconto: [0],
      totalComDesconto: [{ value: 0, disabled: true }],
      tamanhoSelecionado: [''], // 🆕 Campo para tamanho selecionado
      tamanhosDisponiveis: [[]], // 🆕 Array de tamanhos disponíveis
      temTamanhos: [false] // 🆕 Flag para indicar se produto tem tamanhos
    });
  }

  get itensFormArray(): FormArray {
    return this.vendaForm.get('itens') as FormArray;
  }

  addItem(): void {
    const newIndex = this.itensFormArray.length;
    this.itensFormArray.push(this.createItemFormGroup());
    
    // 🔧 COMPORTAMENTO COMO SELECT NATIVO: Novo item sempre fechado
    this.produtoSearchTerms[newIndex] = '';
    this.showDropdown[newIndex] = false; // SEMPRE FECHADO como select nativo
    this.filteredProdutos[newIndex] = [...this.produtos];
    
    // Inicializar arrays de totais para o novo item
    this.totaisCalculados[newIndex] = 0;
    this.subtotaisCalculados[newIndex] = 0;
    
    console.log(`🔧 Novo item adicionado (como select nativo) - index: ${newIndex}, dropdown FECHADO`);
  }

  removeItem(index: number): void {
    if (this.itensFormArray.length > 1) {
      this.itensFormArray.removeAt(index);
      delete this.produtoSearchTerms[index];
      delete this.showDropdown[index];
      delete this.filteredProdutos[index];
      
      // Remover totais dos arrays
      this.totaisCalculados.splice(index, 1);
      this.subtotaisCalculados.splice(index, 1);
    }
  }

  loadProdutos(): void {
    this.produtoService.listarProdutos().subscribe({
      next: (resp) => {
        console.log('🛍️ Produtos carregados:', resp); // Debug
        console.log('🛍️ Produtos recebidos:', resp.content?.length || 0);
        const todosProdutos = (resp.content || []) as ProdutoResponse[];
        
        // 🔍 Debug - verificar se nomes estão chegando
        todosProdutos.slice(0, 3).forEach((prod, idx) => {
          console.log(`🛍️ Produto ${idx + 1}:`, {
            id: prod.id,
            nome: prod.nome,
            departamento: prod.departamento,
            preco: prod.preco,
            estoque: prod.quantidadeEstoque || prod.estoque
          });
        });
        
        // 🔍 Filtrar apenas produtos com estoque disponível
        this.produtos = todosProdutos.filter(produto => {
          if (produto.tamanhos && produto.tamanhos.length > 0) {
            return produto.tamanhos.some(tamanho => (tamanho.estoque || 0) > 0);
          }
          const estoque = produto.estoque || produto.quantidadeEstoque || 0;
          return estoque > 0;
        });
        
        console.log(`🔍 Produtos filtrados: ${this.produtos.length} de ${todosProdutos.length} produtos têm estoque`);
        console.log('🔍 Primeiros produtos filtrados:', this.produtos.slice(0, 3).map(p => ({ nome: p.nome, id: p.id })));
        this.filteredProdutos = {};
        for (let i = 0; i < this.itensFormArray.length; i++) {
          this.filteredProdutos[i] = [...this.produtos];
        }
      },
      error: (err) => console.error('Erro ao carregar produtos:', err)
    });
  }

  loadVendas(page: number = 0, size: number = 50): void {
    this.isLoading = true;
    this.vendasService.listarVendas({}, page, size).subscribe({
      next: (resp) => {
        this.vendas = resp.content || [];
        this.totalItems = resp.totalElements || this.vendas.length;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar vendas:', err);
        this.isLoading = false;
      }
    });
  }

  loadStats(): void {
    this.vendasService.obterEstatisticasVendas().subscribe({
      next: (s) => {
        this.stats = { 
          ...s, 
          vendasHoje: (s as any).vendasHoje ?? s.totalVendas ?? 0,
          faturamentoHoje: (s as any).faturamentoHoje ?? s.totalFaturamento ?? 0,
          vendasMes: (s as any).vendasMes ?? s.totalVendas ?? 0,
          faturamentoMes: (s as any).faturamentoMes ?? s.totalFaturamento ?? 0
        };
      },
      error: (err) => console.error('Erro ao carregar estatísticas:', err)
    });
  }

  formatarCpf(event: any): void {
    let value = (event.target.value || '').replace(/\D/g, '');
    if (value.length > 11) value = value.slice(0, 11);
    value = value.replace(/(\d{3})(\d)/, '$1.$2');
    value = value.replace(/(\d{3})(\d)/, '$1.$2');
    value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    event.target.value = value;
    this.cpfBusca = value;
  }

  onCpfFocus(): void { this.showOpcoesCliente = true; }

  usarCpfFake(): void { this.cpfBusca = '000.000.000-00'; this.buscarClientePorCpf(); }

  formatarTelefone(event: any): void {
    let value = (event.target.value || '').replace(/\D/g, '');
    if (value.length <= 10) value = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    else value = value.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3');
    event.target.value = value;
  }

  formatarCep(event: any): void {
    let value = (event.target.value || '').replace(/\D/g, '');
    if (value.length > 8) value = value.slice(0, 8);
    value = value.replace(/(\d{5})(\d{1,3})/, '$1-$2');
    event.target.value = value;
  }

  getErrorMessage(fieldName: string): string {
    const control = this.vendaForm.get(fieldName);
    if (!control) return '';
    if (control.hasError('required')) return 'Campo obrigatório.';
    if (control.hasError('min')) return 'Valor abaixo do mínimo.';
    if (control.hasError('email')) return 'Email inválido.';
    return '';
  }

  getFilteredProdutos(index: number): ProdutoResponse[] { return this.filteredProdutos[index] || []; }

  getFormaPagamentoText(forma: any): string { return this.vendasService.getTextoFormaPagamento(forma); }
  getStatusText(status: any): string { return this.vendasService.getTextoStatus(status); }
  getStatusBadgeClass(status: any): string { return this.vendasService.getClasseStatus(status); }

  onParcelamentoChange(): void { /* recalcula valores das parcelas */ }
  onJurosChange(): void { /* recalcula juros */ }
  onPercentualDescontoChange(): void { 
    console.log('📈 onPercentualDescontoChange chamado - DESCONTO GERAL');
    
    // O desconto % final é um DESCONTO GERAL (não por item)
    // Recalcular descontos cumulativos com debounce maior
    setTimeout(() => {
      this.recalcularDescontosCumulativos();
    }, 100);
  }

  getCategoriaClass(categoria: string): string {
    switch(categoria) {
      case 'DIAMANTE': return 'primary';
      case 'OURO': return 'warning';
      case 'PRATA': return 'secondary';
      case 'BRONZE': return 'dark';
      default: return 'dark';
    }
  }

  calculateItemSubtotal(index: number): void { this.calcularTotalItem(index); }

  onProdutoSearch(index: number, event: Event): void {
    const target = event.target as HTMLInputElement;
    const term = target.value.toLowerCase();
    console.log(`🔧 onProdutoSearch called - index: ${index}, term: "${term}"`);
    
    // 🔧 Atualizar o FormControl do produto nome
    const item = this.itensFormArray.at(index) as FormGroup;
    item.patchValue({ produtoNome: target.value });
    
    // 🔧 COMPORTAMENTO COMO SELECT NATIVO: Só abre com digitação real
    this.produtoSearchTerms[index] = term;
    
    if (!term.trim()) {
      // Se campo limpo, fechar imediatamente
      this.showDropdown[index] = false;
      this.filteredProdutos[index] = [...this.produtos];
      console.log(`🔧 Campo limpo - dropdown FECHADO`);
      return;
    }
    
    // Filtrar produtos baseado no termo
    this.filteredProdutos[index] = this.produtos.filter(p =>
      (p.nome || '').toLowerCase().includes(term) || (p.departamento || '').toLowerCase().includes(term)
    );
    
    // 🔧 COMPORTAMENTO COMO SELECT: Só abre quando há digitação ATIVA (> 1 caractere)
    if (this.filteredProdutos[index].length > 0 && term.length >= 2) {
      this.showDropdown[index] = true;
      console.log(`🔧 Digitação ativa: "${term}" - ${this.filteredProdutos[index].length} resultados - dropdown ABERTO`);
    } else {
      this.showDropdown[index] = false;
      console.log(`🔧 Termo muito curto ou sem resultados - dropdown FECHADO`);
    }
  }

  onProdutoClick(index: number): void {
    console.log(`🔧 onProdutoClick called - index: ${index}`);
    // 🔧 COMPORTAMENTO COMO SELECT NATIVO: Abrir dropdown quando clicado
    
    if (this.produtos.length > 0) {
      // Se dropdown está fechado, abrir com todos os produtos
      if (!this.showDropdown[index]) {
        this.showDropdown[index] = true;
        this.filteredProdutos[index] = [...this.produtos];
        console.log(`🔧 Dropdown ABERTO por clique - todos os produtos exibidos - index: ${index}`);
      } else {
        // Se dropdown está aberto, fechar
        this.showDropdown[index] = false;
        console.log(`🔧 Dropdown FECHADO por clique - index: ${index}`);
      }
    }
  }

  onProdutoFocus(index: number): void {
    console.log(`🔧 onProdutoFocus called - index: ${index}`);
    // 🔧 COMPORTAMENTO COMO SELECT NATIVO: NÃO abrir no focus, apenas preparar dados
    // O dropdown só abrirá quando o usuário realmente clicar (via onProdutoClick)
    
    const searchTerm = this.produtoSearchTerms[index] || '';
    if (this.produtos.length > 0) {
      // Apenas preparar dados filtrados, mas NÃO abrir dropdown
      this.filteredProdutos[index] = searchTerm ? 
        this.produtos.filter(p => 
          (p.nome || '').toLowerCase().includes(searchTerm.toLowerCase()) || 
          (p.departamento || '').toLowerCase().includes(searchTerm.toLowerCase())
        ) : this.produtos;
      console.log(`🔧 Dados preparados no focus - dropdown permanece FECHADO - index: ${index}`);
    }
  }

  onProdutoBlur(index: number): void {
    // 🔧 CORREÇÃO: Tempo otimizado para permitir cliques mas fechar rapidamente
    setTimeout(() => {
      // Verificar se o usuário não está clicando em um item do dropdown
      const activeElement = document.activeElement;
      const isClickingDropdown = activeElement && activeElement.closest('.dropdown-menu');
      
      if (!isClickingDropdown) {
        this.showDropdown[index] = false;
        console.log(`🔧 Dropdown fechado por blur - index: ${index}`);
      }
    }, 150);
  }

  selectProduto(index: number, produto: ProdutoResponse): void {
    console.log(`🔧 selectProduto called - index: ${index}, produto:`, produto);
    const item = this.itensFormArray.at(index) as FormGroup;
    
    console.log(`🔧 FormGroup before update:`, item.value);
    console.log(`🔧 FormGroup controls:`, Object.keys(item.controls));
    
    // 🔧 Verificar se produto tem estoque disponível (apenas para produtos sem tamanhos)
    if (!produto.tamanhos || produto.tamanhos.length === 0) {
      if (produto.estoque <= 0) {
        console.log(`🔧 Produto sem tamanhos e sem estoque, retornando...`);
        this.errorMessage = `Produto "${produto.nome}" não possui estoque disponível.`;
        return;
      }
    }
    
    console.log(`🔧 Produto selecionado, continuando... estoque: ${produto.estoque}`);
    
    // 🔧 Atualizar o FormControl com ID, nome e preço
    try {
      item.patchValue({ 
        produtoId: produto.id, 
        produtoNome: produto.nome,
        precoUnitario: produto.preco
      });
      console.log(`🔧 patchValue executado com sucesso`);
    } catch (error) {
      console.error(`🔧 Erro no patchValue:`, error);
    }
    
    console.log(`🔧 FormGroup after update:`, item.value);
    console.log(`🔧 produtoNome control value:`, item.get('produtoNome')?.value);
    console.log(`🔧 precoUnitario control value:`, item.get('precoUnitario')?.value);
    
    // 🔧 COMPORTAMENTO COMO SELECT NATIVO: Fechamento imediato após seleção
    this.showDropdown[index] = false;
    
    // Atualizar termo de busca com o nome do produto selecionado
    this.produtoSearchTerms[index] = produto.nome;
    
    console.log(`🔧 PRODUTO SELECIONADO (como select nativo) - Dropdown fechado para: ${produto.nome}`);
    
    // 🔧 GARANTIA de fechamento como select nativo
    setTimeout(() => this.showDropdown[index] = false, 0);
    setTimeout(() => this.showDropdown[index] = false, 50);
    setTimeout(() => this.showDropdown[index] = false, 100);
    
    // 🔧 Usar setTimeout para todas as operações após patchValue para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      if (produto.tamanhos && produto.tamanhos.length > 0) {
        console.log(`🔧 Produto tem tamanhos, carregando...`);
        this.carregarTamanhosDisponiveis(index, produto.id!);
      } else {
        console.log(`🔧 Produto sem tamanhos, validando estoque...`);
        this.validarEstoqueProduto(index, produto);
      }
      
      console.log(`🔧 Calculando total do item...`);
      this.calcularTotalItem(index);
      
      // 🎯 RECALCULAR DESCONTOS uma única vez após adicionar produto
      setTimeout(() => {
        this.calcularTotalGeral();
        
        // Verificar se existe evento ou desconto percentual
        const eventoId = this.vendaForm.get('evento')?.value;
        const percentualDesconto = this.vendaForm.get('percentualDesconto')?.value || 0;
        
        if ((eventoId && this.eventoSelecionado && this.eventoSelecionado.descontoPercentual && this.eventoSelecionado.descontoPercentual > 0) || percentualDesconto > 0) {
          this.recalcularDescontosCumulativos();
        }
      }, 100);
      
      console.log(`🔧 selectProduto finalizado`);
    }, 0);
  }

  /**
   * 🆕 Carrega tamanhos disponíveis (com estoque > 0) para um produto
   */
  carregarTamanhosDisponiveis(index: number, produtoId: number): void {
    console.log(`🔧 carregarTamanhosDisponiveis chamado - index: ${index}, produtoId: ${produtoId}`);
    
    this.produtoService.buscarTamanhosComEstoque(produtoId).subscribe({
      next: (tamanhos) => {
        console.log(`🔧 Tamanhos recebidos:`, tamanhos);
        const item = this.itensFormArray.at(index) as FormGroup;
        
        if (tamanhos.length === 0) {
          console.log(`🔧 Nenhum tamanho com estoque disponível`);
          this.errorMessage = `Produto não possui tamanhos com estoque disponível.`;
          // 🚫 NÃO resetar o produto - apenas mostrar erro
          return;
        }
        
        console.log(`🔧 Atualizando FormGroup com ${tamanhos.length} tamanhos`);
        // 🔄 Limpar erro anterior
        this.errorMessage = '';
        
        item.patchValue({
          tamanhosDisponiveis: tamanhos,
          temTamanhos: true
        });
        
        if (tamanhos.length === 1) {
          console.log(`🔧 Selecionando automaticamente o tamanho: ${tamanhos[0].tamanho}`);
          item.patchValue({ tamanhoSelecionado: tamanhos[0].id });
          this.validarEstoqueTamanho(index, tamanhos[0]);
        }
        
        console.log(`🔧 carregarTamanhosDisponiveis concluído`);
      },
      error: (err) => {
        console.error('🚨 Erro ao carregar tamanhos:', err);
        this.errorMessage = 'Erro ao carregar tamanhos do produto.';
      }
    });
  }

  /**
   * 🆕 Valida estoque de produto sem tamanhos
   */
  validarEstoqueProduto(index: number, produto: ProdutoResponse): void {
    const item = this.itensFormArray.at(index);
    const quantidade = item.get('quantidade')?.value || 1;
    
    if (produto.estoque < quantidade) {
      this.errorMessage = `Estoque insuficiente. Disponível: ${produto.estoque}, Solicitado: ${quantidade}`;
      item.get('quantidade')?.setValue(produto.estoque);
    }
  }

  /**
   * 🆕 Valida estoque disponível para um tamanho específico
   */
  validarEstoqueTamanho(index: number, tamanho: TamanhoProduto): void {
    console.log(`🔧 validarEstoqueTamanho chamado - index: ${index}, tamanho:`, tamanho);
    
    const item = this.itensFormArray.at(index);
    const quantidade = item.get('quantidade')?.value || 1;
    const estoqueDisponivel = tamanho.estoque || tamanho.quantidade;
    
    console.log(`🔧 Validando estoque - quantidade: ${quantidade}, disponível: ${estoqueDisponivel}`);
    
    if (estoqueDisponivel < quantidade) {
      console.log(`🚨 Estoque insuficiente!`);
      this.errorMessage = `Estoque insuficiente para tamanho ${tamanho.nome}. Disponível: ${estoqueDisponivel}, Solicitado: ${quantidade}`;
      item.get('quantidade')?.setValue(estoqueDisponivel);
    }
    
    if (tamanho.preco) {
      console.log(`🔧 Atualizando preço para: ${tamanho.preco}`);
      item.patchValue({ precoUnitario: tamanho.preco });
    }
    
    console.log(`🔧 validarEstoqueTamanho concluído`);
  }

  /**
   * 🆕 Chamado quando usuário seleciona um tamanho
   */
  onTamanhoChange(index: number): void {
    const item = this.itensFormArray.at(index) as FormGroup;
    const tamanhoId = item.get('tamanhoSelecionado')?.value;
    const tamanhos = this.getTamanhosDisponiveis(index);
    
    const tamanho = tamanhos.find((t: TamanhoProduto) => t.id === Number(tamanhoId));
    if (tamanho) {
      this.validarEstoqueTamanho(index, tamanho);
      this.calcularTotalItem(index);
    }
  }

  /**
   * 🆕 Retorna tamanhos disponíveis para um item específico
   */
  getTamanhosDisponiveis(index: number): TamanhoProduto[] {
    const item = this.itensFormArray.at(index);
    return item.get('tamanhosDisponiveis')?.value || [];
  }

  /**
   * 🆕 Verifica se um produto tem tamanhos
   */
  produtoTemTamanhos(index: number): boolean {
    const item = this.itensFormArray.at(index);
    const temTamanhos = item.get('temTamanhos')?.value || false;
    const tamanhos = this.getTamanhosDisponiveis(index);
    return temTamanhos && tamanhos.length > 0;
  }

  /**
   * 🆕 Verifica se é necessário selecionar um tamanho
   */
  precisaSelecionarTamanho(index: number): boolean {
    if (!this.produtoTemTamanhos(index)) {
      return false;
    }
    
    const item = this.itensFormArray.at(index);
    const tamanhoSelecionado = item.get('tamanhoSelecionado')?.value;
    const tamanhos = this.getTamanhosDisponiveis(index);
    
    return tamanhos.length > 1 && !tamanhoSelecionado;
  }

  /**
   * 🆕 Verifica se o item está pronto para venda
   */
  itemProntoParaVenda(index: number): boolean {
    const item = this.itensFormArray.at(index);
    const produtoId = item.get('produtoId')?.value;
    
    if (!produtoId) {
      return false;
    }
    
    if (this.produtoTemTamanhos(index)) {
      return !this.precisaSelecionarTamanho(index);
    }
    
    return true;
  }

  calcularTotalItem(index: number): number {
    const item = this.itensFormArray.at(index);
    const quantidade = item.get('quantidade')?.value || 0;
    const preco = item.get('precoUnitario')?.value || 0;
    const subtotal = quantidade * preco;
    const perc = item.get('percentualDesconto')?.value || 0;
    const desconto = (subtotal * (perc || 0)) / 100;
    const total = subtotal - desconto;
    
    // Só logar quando houver mudanças significativas para evitar spam
    const currentTotal = item.get('totalComDesconto')?.value;
    if (Math.abs(currentTotal - total) > 0.01) {
      console.log(`💰 calcularTotalItem[${index}] - Qtd: ${quantidade}, Preço: ${preco}, Subtotal: ${subtotal}, Total: ${total}`);
    }
    
    // Atualizar valores diretamente sem setTimeout para evitar problemas de timing
    item.get('subtotal')?.setValue(subtotal, { emitEvent: false });
    item.get('totalComDesconto')?.setValue(total, { emitEvent: false });
    
    // Atualizar arrays de totais calculados
    this.totaisCalculados[index] = total;
    this.subtotaisCalculados[index] = subtotal;
    
    return total;
  }

  // Método seguro para obter total calculado sem loops
  getTotalCalculado(index: number): number {
    if (this.totaisCalculados[index] !== undefined) {
      return this.totaisCalculados[index];
    }
    return this.calcularTotalItem(index);
  }

  // Métodos seguros para template (evitam loops infinitos)
  getTotalDescontoIndividualSeguro(): number {
    try {
      return this.calcularDescontoTotalIndividual();
    } catch (error) {
      console.warn('Erro ao calcular desconto individual:', error);
      return 0;
    }
  }

  getDescontoEventoSeguro(): number {
    try {
      return this.calcularDescontoEvento();
    } catch (error) {
      console.warn('Erro ao calcular desconto evento:', error);
      return 0;
    }
  }

  getTotalDescontoCumulativoSeguro(): number {
    try {
      return this.calcularDescontoTotalCumulativo();
    } catch (error) {
      console.warn('Erro ao calcular desconto cumulativo:', error);
      return 0;
    }
  }

  getDescontoGeralPercentualSeguro(): number {
    try {
      return this.calcularDescontoGeralPercentual();
    } catch (error) {
      console.warn('Erro ao calcular desconto geral percentual:', error);
      return 0;
    }
  }

  // 🆕 Método para calcular o TOTAL de descontos para EXIBIÇÃO (inclui todos os tipos)
  getTotalDescontosParaExibicao(): number {
    try {
      const descontoItens = this.getTotalDescontoIndividualSeguro(); // Desconto por item
      const descontoEvento = this.getDescontoEventoSeguro(); // Desconto do evento
      const descontoGeral = this.getDescontoGeralPercentualSeguro(); // Desconto geral/percentual
      
      const totalDescontos = descontoItens + descontoEvento + descontoGeral;
      
      console.log('🎯 TOTAL DESCONTOS PARA EXIBIÇÃO:', {
        descontoItens: descontoItens.toFixed(2),
        descontoEvento: descontoEvento.toFixed(2), 
        descontoGeral: descontoGeral.toFixed(2),
        totalDescontos: totalDescontos.toFixed(2)
      });
      
      return totalDescontos;
    } catch (error) {
      console.warn('Erro ao calcular total de descontos para exibição:', error);
      return 0;
    }
  }
  
  onQuantidadeChange(index: number): void { 
    console.log(`📊 onQuantidadeChange[${index}]`);
    this.calcularTotalItem(index);
    // Recalcular descontos cumulativos após mudança na quantidade
    setTimeout(() => this.recalcularDescontosCumulativos(), 50);
  }
  
  aplicarDescontoPorcentual(index: number, event: Event): void { 
    console.log(`📊 aplicarDescontoPorcentual[${index}] - DESCONTO POR ITEM`);
    
    // Recalcular apenas este item
    this.calcularTotalItem(index);
    
    // Recalcular descontos cumulativos após mudança no desconto por item
    setTimeout(() => this.recalcularDescontosCumulativos(), 50);
  }

  // 🆕 Método para recalcular todos os valores de forma consistente
  recalcularTodosOsValores(): void {
    console.log('🔄 Recalculando todos os valores...');
    
    // Recalcular todos os itens (SEM chamar calcularTotalGeral internamente)
    for (let i = 0; i < this.itensFormArray.length; i++) {
      this.calcularTotalItem(i);
    }
    
    // Recalcular descontos cumulativos após recalcular todos os itens
    setTimeout(() => {
      const temDesconto = (this.eventoSelecionado && this.eventoSelecionado.descontoPercentual && this.eventoSelecionado.descontoPercentual > 0) || 
                         (this.vendaForm.get('percentualDesconto')?.value || 0) > 0;
      
      if (temDesconto) {
        this.recalcularDescontosCumulativos();
      } else {
        this.calcularTotalGeral();
      }
    }, 30);
  }

  // 🆕 Método para aplicar desconto geral por porcentagem (CUMULATIVO)
  aplicarDescontoGeralPorcentual(): void {
    console.log('📈 aplicarDescontoGeralPorcentual chamado');
    
    // Usar o método corrigido de cálculo cumulativo
    this.recalcularDescontosCumulativos();
  }

  // 🆕 Método para recalcular total geral - CORRIGIDO para não aplicar desconto duplo
  calcularTotalGeral(): void {
    const subtotalComDescontoItens = this.getSubtotalComDescontoItens(); // Já inclui descontos individuais
    const descontoAdicional = this.vendaForm.get('desconto')?.value || 0; // Apenas descontos adicionais (evento/geral)
    const total = Math.max(0, subtotalComDescontoItens - descontoAdicional);
    
    console.log('💰 calcularTotalGeral CORRIGIDO - Subtotal (c/ desc. itens):', subtotalComDescontoItens.toFixed(2), 
                'Desconto adicional:', descontoAdicional.toFixed(2), 'Total final:', total.toFixed(2));
    
    // Atualizar o total no form se necessário
    if (!this.vendaForm.get('total')) {
      console.log('💰 Campo total não existe no form');
    }
    
    this.vendaForm.get('desconto')?.updateValueAndValidity();
  }
  
  // 🆕 Método para recalcular todos os descontos de forma coordenada
  recalcularDescontosCumulativos(): void {
    console.log('🔄 Recalculando descontos cumulativos...');
    
    const descontoTotalCumulativo = this.calcularDescontoTotalCumulativo();
    
    console.log('📈 Definindo desconto total cumulativo:', descontoTotalCumulativo);
    this.vendaForm.get('desconto')?.setValue(descontoTotalCumulativo, { emitEvent: false });
    
    this.calcularTotalGeral();
  }

  getSubtotal(): number { return this.itensFormArray.controls.reduce((sum, c) => sum + (c.get('subtotal')?.value || 0), 0); }
  getSubtotalComDescontoItens(): number { return this.itensFormArray.controls.reduce((sum, c) => sum + (c.get('totalComDesconto')?.value || 0), 0); }
  getTotal(): number { const subtotal = this.getSubtotalComDescontoItens(); const descontoGeral = this.vendaForm.get('desconto')?.value || 0; return Math.max(0, subtotal - descontoGeral); }
  
  // 🆕 Método para calcular desconto total cumulativo - CORRIGIDO para não aplicar desconto duplo
  calcularDescontoTotalCumulativo(): number {
    // 🔧 GARANTIR que os totais dos itens estão atualizados ANTES do cálculo
    this.itensFormArray.controls.forEach((control, index) => {
      this.calcularTotalItem(index);
    });
    
    const subtotalOriginal = this.getSubtotal(); // Subtotal SEM nenhum desconto
    const subtotalComDescontoItens = this.getSubtotalComDescontoItens(); // Subtotal COM desconto individual já aplicado
    
    // 🎯 CORREÇÃO: Se os itens já têm desconto aplicado, NÃO aplicar desconto adicional de itens
    // Os descontos individuais já estão incorporados no subtotalComDescontoItens
    
    // 1. Desconto de evento (aplica sobre subtotal original - antes de qualquer desconto)
    let descontoEventoValor = 0;
    if (this.eventoSelecionado && this.eventoSelecionado.descontoPercentual && this.eventoSelecionado.descontoPercentual > 0) {
      descontoEventoValor = (subtotalOriginal * this.eventoSelecionado.descontoPercentual) / 100;
    }
    
    // 2. Desconto geral/percentual (aplica sobre subtotal com desconto individual já aplicado)
    let descontoGeralValor = 0;
    const percentualDesconto = this.vendaForm.get('percentualDesconto')?.value || 0;
    if (percentualDesconto > 0) {
      descontoGeralValor = (subtotalComDescontoItens * percentualDesconto) / 100;
    }
    
    // 🎯 TOTAL: Apenas descontos adicionais (evento + geral), NÃO incluir desconto de itens
    const descontoTotalCumulativo = descontoEventoValor + descontoGeralValor;
    
    // 🔧 LOGS INFORMATIVOS - mostra cálculo correto
    if (subtotalOriginal > 0) {
      const descontoIndividualJaAplicado = subtotalOriginal - subtotalComDescontoItens;
      console.log('🧮 DESCONTO CORRIGIDO - Original:', subtotalOriginal.toFixed(2), 
                  'Já aplicado nos itens:', descontoIndividualJaAplicado.toFixed(2), 
                  'Desconto adicional:', descontoTotalCumulativo.toFixed(2));
    }
    
    return descontoTotalCumulativo;
  }
  getValorComJuros(): number { const total = this.getTotal(); const juros = this.vendaForm.get('jurosPercentual')?.value || 0; return total + (total * juros / 100); }
  getValorParcela(): number { const numeroParcelas = this.vendaForm.get('numeroParcelas')?.value || 1; return this.getValorComJuros() / numeroParcelas; }
  getTotalFinal(): number { return this.getValorComJuros(); }
  
  // ===== MÉTODOS PARA EXIBIÇÃO VISUAL DOS DESCONTOS =====
  
  /**
   * Calcula desconto total dos itens individuais
   */
  calcularDescontoTotalIndividual(): number {
    return this.itensFormArray.controls.reduce((total, control) => {
      const subtotal = control.get('subtotal')?.value || 0;
      const totalComDesconto = control.get('totalComDesconto')?.value || 0;
      return total + (subtotal - totalComDesconto);
    }, 0);
  }
  
  /**
   * Calcula valor do desconto de evento
   */
  calcularDescontoEvento(): number {
    if (!this.eventoSelecionado || !this.eventoSelecionado.descontoPercentual || this.eventoSelecionado.descontoPercentual <= 0) {
      return 0;
    }
    const subtotalOriginal = this.getSubtotal();
    return (subtotalOriginal * this.eventoSelecionado.descontoPercentual) / 100;
  }
  
  /**
   * Calcula valor do desconto geral percentual
   */
  calcularDescontoGeralPercentual(): number {
    const percentualDesconto = this.vendaForm.get('percentualDesconto')?.value || 0;
    if (percentualDesconto <= 0) {
      return 0;
    }
    const subtotalComDescontoItens = this.getSubtotalComDescontoItens();
    return (subtotalComDescontoItens * percentualDesconto) / 100;
  }

  // filtros / paginação simples no frontend
  getFilteredVendas(): VendaResponse[] {
    let filtered = [...this.vendas];
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(v => (v.nomeCliente || '').toLowerCase().includes(term) || (v.id?.toString() || '').includes(term));
    }
    if (this.statusFilter) filtered = filtered.filter(v => v.status === this.statusFilter);
    if (this.dateFilter) {
      const today = new Date();
      filtered = filtered.filter(v => {
        const d = new Date(v.dataVenda as any);
        if (this.dateFilter === 'hoje') return d.toDateString() === today.toDateString();
        if (this.dateFilter === 'semana') return d >= new Date(today.getTime() - 7 * 24 * 3600 * 1000);
        if (this.dateFilter === 'mes') return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear();
        return true;
      });
    }
    if (this.orderByPrice === 'asc') filtered.sort((a, b) => (a.valorTotal || 0) - (b.valorTotal || 0));
    if (this.orderByPrice === 'desc') filtered.sort((a, b) => (b.valorTotal || 0) - (a.valorTotal || 0));
    return filtered;
  }

  getPaginatedVendas(): VendaResponse[] { const filtered = this.getFilteredVendas(); const start = (this.currentPage - 1) * this.itemsPerPage; return filtered.slice(start, start + this.itemsPerPage); }
  getTotalPages(): number { return Math.ceil(this.getFilteredVendas().length / this.itemsPerPage) || 1; }
  goToPage(page: number): void { if (page < 1) page = 1; if (page > this.getTotalPages()) page = this.getTotalPages(); this.currentPage = page; }

  // ações do template
  toggleVendaForm(): void { 
    this.showVendaForm = !this.showVendaForm; 
    if (!this.showVendaForm) {
      this.resetVendaForm(); 
    } else {
      // 🔧 CORREÇÃO: Limpar arrays de controle do dropdown ao abrir nova venda
      this.limparControlsDropdown();
    }
  }

  resetVendaForm(): void { 
    this.vendaForm.reset({ 
      formaPagamento: FormaPagamento.DINHEIRO, 
      pagamentoParcelado: false, 
      numeroParcelas: 1, 
      jurosPercentual: 0, 
      desconto: 0 
    }); 
    
    // Limpar itens do FormArray
    while (this.itensFormArray.length > 1) {
      this.itensFormArray.removeAt(1);
    }
    this.itensFormArray.at(0).reset({ 
      quantidade: 1, 
      precoUnitario: 0, 
      subtotal: 0, 
      percentualDesconto: 0, 
      totalComDesconto: 0 
    }); 
    
    this.showVendaForm = false;
    
    // 🔧 CORREÇÃO: Limpar arrays de controle do dropdown
    this.limparControlsDropdown();
  }

  /**
   * 🆕 CORREÇÃO: Método para limpar completamente os arrays de controle do dropdown
   */
  private limparControlsDropdown(): void {
    console.log('🔧 Limpando controles do dropdown...');
    
    // 🔧 LIMPEZA TOTAL E FORÇADA
    this.produtoSearchTerms = {};
    this.showDropdown = {};
    this.filteredProdutos = {};
    
    // Inicializar para todos os itens do FormArray
    const numItens = Math.max(this.itensFormArray.length, 1);
    for (let i = 0; i < numItens; i++) {
      this.produtoSearchTerms[i] = '';
      this.showDropdown[i] = false; // 🔧 SEMPRE FECHADO - GARANTIA MÁXIMA
      this.filteredProdutos[i] = [...this.produtos];
      
      // 🔧 GARANTIA EXTRA - forçar fechamento com timeout
      setTimeout(() => {
        this.showDropdown[i] = false;
      }, 0);
    }
    
    console.log(`🔧 Arrays de controle COMPLETAMENTE limpos para ${numItens} itens - TODOS FORÇADOS A FECHAR`);
  }

  onFormaPagamentoChange(): void { const forma = this.vendaForm.get('formaPagamento')?.value; if (forma !== FormaPagamento.CARTAO_CREDITO) this.vendaForm.patchValue({ pagamentoParcelado: false, numeroParcelas: 1, jurosPercentual: 0 }); }
  isCartaoCredito(): boolean { return this.vendaForm.get('formaPagamento')?.value === FormaPagamento.CARTAO_CREDITO; }
  isParcelado(): boolean { return this.vendaForm.get('pagamentoParcelado')?.value === true; }

  formatarMoeda(valor: number | null | undefined): string { return this.vendasService.formatarMoeda(valor as any); }
  formatarData(data: Date | string | undefined): string { return this.vendasService.formatarData(data as any); }

  // busca por código de barras
  buscarPorCodigoBarras(codigo: string): void {
    if (!codigo) return;
    this.produtoService.buscarPorCodigoBarras(codigo).subscribe({
      next: (p) => { this.addItem(); const idx = this.itensFormArray.length - 1; this.selectProduto(idx, p); },
      error: (err) => this.errorMessage = 'Produto não encontrado por código de barras.'
    });
  }

  // cliente
  buscarClientePorCpf(): void {
    const cpf = this.cpfBusca?.replace(/\D/g, '') || '';
    if (!cpf) { this.clienteAtual = null; this.clienteEncontrado = false; return; }
    this.buscandoCliente = true;
    this.clienteService.buscarPorCpf(this.cpfBusca).subscribe({
      next: (c) => { if (c) { this.clienteAtual = c as Cliente; this.clienteEncontrado = true; this.preencherDadosCliente(); this.successMessage = `Cliente encontrado: ${c.nome}`; } else { this.clienteAtual = null; this.clienteEncontrado = false; } this.buscandoCliente = false; },
      error: (err) => { this.errorMessage = 'Erro ao buscar cliente.'; this.buscandoCliente = false; }
    });
  }

  preencherDadosCliente(): void { if (!this.clienteAtual) return; this.vendaForm.patchValue({ clienteNome: this.clienteAtual.nome, clienteEmail: this.clienteAtual.email, clienteTelefone: this.clienteAtual.telefone }); }

  abrirCadastroCliente(): void { this.showCadastroCliente = true; }
  fecharCadastroCliente(): void { this.showCadastroCliente = false; }

  salvarNovoCliente(): void { if (!this.clienteForm.valid) return; this.clienteService.criarCliente(this.clienteForm.value).subscribe({ next: (c) => { this.clienteAtual = c; this.clienteEncontrado = true; this.successMessage = `Cliente ${c.nome} cadastrado!`; this.fecharCadastroCliente(); }, error: (err) => this.errorMessage = 'Erro ao cadastrar cliente.' }); }

  limparCliente(): void { this.clienteAtual = null; this.clienteEncontrado = false; this.cpfBusca = ''; this.vendaForm.patchValue({ clienteNome: '', clienteEmail: '', clienteTelefone: '' }); }

  // ações da venda
  onSubmitVenda(): void {
    if (this.vendaForm.invalid || this.itensFormArray.length === 0) { this.markFormGroupTouched(this.vendaForm); return; }
    
    const itens = this.itensFormArray.controls.map(c => {
      const produto = this.produtos.find(p => p.id === c.get('produtoId')?.value);
      const subtotal = c.get('subtotal')?.value || 0;
      const percentualDesconto = c.get('percentualDesconto')?.value || 0;
      const descontoItemValor = (subtotal * percentualDesconto) / 100;
      
      console.log(`📈 Item ${produto?.nome}: Subtotal=${subtotal}, %Desconto=${percentualDesconto}, DescontoValor=${descontoItemValor}`);
      
      return {
        produtoId: c.get('produtoId')?.value,
        quantidade: c.get('quantidade')?.value,
        precoUnitario: c.get('precoUnitario')?.value,
        nomeProduto: produto?.nome || '', // 🔧 Campo obrigatório
        descontoItem: descontoItemValor, // 🔧 Valor correto do desconto do item
        tamanho: undefined // Pode ser implementado depois para tamanhos específicos
      };
    });
    
    // 📈 Cálculos de debug
    const subtotalTotal = this.getSubtotalComDescontoItens();
    const descontoGeral = this.vendaForm.get('desconto')?.value || 0;
    const totalFinal = this.getTotal();
    
    console.log('📈 DEBUG VENDA:');
    console.log('  - Subtotal (com desconto itens):', subtotalTotal);
    console.log('  - Desconto geral:', descontoGeral);
    console.log('  - Total final:', totalFinal);
    console.log('  - Itens:', itens);
    
    const request: VendaRequest = {
      nomeCliente: this.vendaForm.get('clienteNome')?.value,
      emailCliente: this.vendaForm.get('clienteEmail')?.value,
      telefoneCliente: this.vendaForm.get('clienteTelefone')?.value,
      formaPagamento: this.vendaForm.get('formaPagamento')?.value,
      desconto: descontoGeral, // 🔧 Usar valor calculado
      observacoes: this.vendaForm.get('observacoes')?.value,
      eventoId: this.eventoSelecionado?.id, // 🔧 Incluir evento se selecionado
      itens
    };
    
    console.log('🔧 FRONTEND - Enviando venda com forma de pagamento:', request.formaPagamento);
    console.log('🔧 FRONTEND - Dados completos da venda:', request);
    
    this.isLoading = true;
    this.vendasService.criarVenda(request).subscribe({ next: (v) => { this.successMessage = 'Venda registrada com sucesso!'; this.resetVendaForm(); this.loadVendas(); this.loadStats(); this.isLoading = false; }, error: (err) => { this.errorMessage = 'Erro ao registrar venda.'; this.isLoading = false; } });
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      if (control instanceof FormGroup || control instanceof FormArray) {
        this.markFormGroupTouched(control as FormGroup);
      } else {
        control?.markAsTouched();
      }
    });
  }

  clearMessages(): void { this.errorMessage = ''; this.successMessage = ''; }

  // util
  getProdutoNome(index: number): string { 
    const item = this.itensFormArray.at(index); 
    const id = item.get('produtoId')?.value; 
    const p = this.produtos.find(x => x.id === id); 
    const result = p ? p.nome : (this.produtoSearchTerms[index] || '');
    console.log(`🔧 getProdutoNome[${index}] - produtoId: ${id}, found: ${p?.nome}, searchTerm: ${this.produtoSearchTerms[index]}, result: "${result}"`);
    return result;
  }

  aplicarFiltroVendasHoje(): void { this.activeFilterBox = this.activeFilterBox === 'hoje' ? '' : 'hoje'; this.dateFilter = this.activeFilterBox === 'hoje' ? 'hoje' : ''; }
  aplicarFiltroVendasMes(): void { this.activeFilterBox = this.activeFilterBox === 'mes' ? '' : 'mes'; this.dateFilter = this.activeFilterBox === 'mes' ? 'mes' : ''; }
  aplicarFiltroPendentes(): void { this.activeFilterBox = this.activeFilterBox === 'pendentes' ? '' : 'pendentes'; this.statusFilter = this.activeFilterBox === 'pendentes' ? StatusVenda.PENDENTE : ''; }
  isBoxActive(box: string): boolean { return this.activeFilterBox === box; }
  limparTodosFiltros(): void { this.activeFilterBox = ''; this.limparFiltros(); }
  limparFiltros(): void { this.searchTerm = ''; this.dateFilter = ''; this.statusFilter = ''; this.orderByPrice = ''; this.currentPage = 1; }

  visualizarVenda(v: VendaResponse) { /* stub - implementar conforme necessidade */ }
  editarVenda(v: VendaResponse) { /* stub - implementar conforme necessidade */ }
  mostrarModalAlterarStatus(v: VendaResponse) { /* stub */ }
  imprimirVenda(v: VendaResponse) { /* stub */ }

  carregarEventosAtivos(): void {
    this.eventosService.listarEventosAtivos().subscribe({
      next: (eventos) => {
        this.eventosAtivos = eventos;
      },
      error: (err) => {
        console.error('Erro ao carregar eventos:', err);
        this.eventosAtivos = [];
      }
    });
  }

  onEventoChange(): void {
    const eventoId = this.vendaForm.get('evento')?.value; // Mudou de 'eventoId' para 'evento'
    console.log('🎯 Evento selecionado:', eventoId);
    
    this.eventoSelecionado = this.eventosAtivos.find(e => e.id == eventoId) || null;
    
    if (this.eventoSelecionado && this.eventoSelecionado.descontoPercentual && this.eventoSelecionado.descontoPercentual > 0) {
      console.log('💰 Aplicando desconto de:', this.eventoSelecionado.descontoPercentual + '%');
      
      // 🎯 VERIFICAR SE JÁ EXISTEM PRODUTOS para recalcular tudo
      const temProdutos = this.itensFormArray.controls.some(item => 
        item.get('produtoId')?.value && item.get('precoUnitario')?.value > 0
      );
      
      if (temProdutos) {
        console.log('🎯 Produtos já existem, recalculando com desconto cumulativo...');
        // Se já tem produtos, recalcular descontos cumulativos
        setTimeout(() => {
          this.recalcularDescontosCumulativos();
        }, 20);
      } else {
        console.log('🎯 Nenhum produto ainda, desconto será aplicado quando produtos forem adicionados');
        // Se não tem produtos ainda, apenas calcular total
        setTimeout(() => {
          this.calcularTotalGeral();
        }, 50);
      }
    } else {
      console.log('🚫 Removendo desconto do evento');
      this.vendaForm.patchValue({ desconto: 0 }, { emitEvent: false });
      setTimeout(() => {
        this.calcularTotalGeral();
      }, 10);
    }
  }

  aplicarDescontoEvento(): void {
    console.log('🎆 aplicarDescontoEvento chamado');
    if (this.eventoSelecionado) {
      const subtotal = this.getSubtotal();
      const descontoEvento = (subtotal * (this.eventoSelecionado?.descontoPercentual || 0)) / 100;
      
      // Verificar se há desconto percentual final para ser cumulativo
      const percentualDesconto = this.vendaForm.get('percentualDesconto')?.value || 0;
      let descontoPercentualFinal = 0;
      if (percentualDesconto > 0) {
        const subtotalComDescontoItens = this.getSubtotalComDescontoItens();
        descontoPercentualFinal = (subtotalComDescontoItens * percentualDesconto) / 100;
      }
      
      // SOMAR os descontos para serem cumulativos
      const descontoTotalCumulativo = descontoEvento + descontoPercentualFinal;
      
      console.log('💰 Subtotal:', subtotal);
      console.log('🎆 Desconto evento:', descontoEvento);
      console.log('📈 Desconto percentual final:', descontoPercentualFinal);
      console.log('💰 Desconto total cumulativo:', descontoTotalCumulativo);
      
      this.vendaForm.patchValue({ desconto: descontoTotalCumulativo }, { emitEvent: false });
      
      // Calcular total geral imediatamente (sem setTimeout para evitar loops)
      this.calcularTotalGeral();
      console.log('💰 Total geral atualizado após desconto cumulativo');
    }
  }

  getEventosUnicos(): string[] { return Array.from(new Set(this.vendas.map(v => (v as any).eventoNome).filter(Boolean))); }

  // ===== MÉTODOS PARA DEVOLUÇÃO E TROCA =====

  abrirModalDevolucao(venda: VendaResponse): void {
    if (venda.status !== 'CONFIRMADA' && venda.status !== 'ENTREGUE') {
      this.errorMessage = 'Só é possível devolver vendas confirmadas ou entregues.';
      return;
    }

    this.vendaSelecionada = venda;
    this.showModalDevolucao = true;
    this.resetarFormularioDevolucao();
  }

  abrirModalTroca(venda: VendaResponse): void {
    if (venda.status !== 'CONFIRMADA' && venda.status !== 'ENTREGUE') {
      this.errorMessage = 'Só é possível trocar vendas confirmadas ou entregues.';
      return;
    }

    this.vendaSelecionada = venda;
    this.showModalTroca = true;
  }

  fecharModalDevolucao(): void {
    this.showModalDevolucao = false;
    this.vendaSelecionada = null;
    this.resetarFormularioDevolucao();
  }

  fecharModalTroca(): void {
    this.showModalTroca = false;
    this.vendaSelecionada = null;
  }

  fecharTodosModais(): void {
    this.fecharCadastroCliente();
    this.fecharModalDevolucao();
    this.fecharModalTroca();
  }

  resetarFormularioDevolucao(): void {
    this.devolucaoForm.reset();
    this.itensSelecionadosDevolucao = {};
    this.quantidadesDevolucao = {};
    this.statusQualidadeItens = {};
    this.descontoItens = {};
  }

  // Métodos para gerenciar itens selecionados
  isItemSelecionado(itemId: number): boolean {
    return !!this.itensSelecionadosDevolucao[itemId];
  }

  toggleItemDevolucao(item: any, event: any): void {
    const isChecked = event.target.checked;
    this.itensSelecionadosDevolucao[item.id] = isChecked;
    
    if (isChecked) {
      this.quantidadesDevolucao[item.id] = 1;
      this.statusQualidadeItens[item.id] = 'NORMAL';
      this.descontoItens[item.id] = 0;
    } else {
      delete this.quantidadesDevolucao[item.id];
      delete this.statusQualidadeItens[item.id];
      delete this.descontoItens[item.id];
    }
  }

  getQuantidadeDevolucao(itemId: number): number {
    return this.quantidadesDevolucao[itemId] || 1;
  }

  setQuantidadeDevolucao(itemId: number, event: any): void {
    const quantidade = parseInt(event.target.value) || 1;
    this.quantidadesDevolucao[itemId] = quantidade;
  }

  getStatusQualidade(itemId: number): string {
    return this.statusQualidadeItens[itemId] || 'NORMAL';
  }

  setStatusQualidade(itemId: number, event: any): void {
    const status = event.target.value;
    this.statusQualidadeItens[itemId] = status;
    
    // Se mudou para defeituoso, define desconto padrão
    if (status === 'DEFEITUOSO' && !this.descontoItens[itemId]) {
      this.descontoItens[itemId] = 30;
    }
  }

  getDescontoItem(itemId: number): number {
    return this.descontoItens[itemId] || 0;
  }

  setDescontoItem(itemId: number, event: any): void {
    const desconto = parseFloat(event.target.value) || 0;
    this.descontoItens[itemId] = Math.min(Math.max(desconto, 0), 100);
  }

  temItensSelecionados(): boolean {
    return Object.values(this.itensSelecionadosDevolucao).some(selected => selected);
  }

  processarDevolucao(): void {
    if (this.devolucaoForm.invalid || !this.temItensSelecionados() || !this.vendaSelecionada) {
      this.errorMessage = 'Preencha todos os campos obrigatórios e selecione pelo menos um item.';
      return;
    }

    const itens: ItemDevolucaoRequest[] = [];
    
    // Montar array de itens para devolução
    for (const itemId of Object.keys(this.itensSelecionadosDevolucao)) {
      if (this.itensSelecionadosDevolucao[parseInt(itemId)]) {
        const item: ItemDevolucaoRequest = {
          itemVendaId: parseInt(itemId),
          quantidade: this.quantidadesDevolucao[parseInt(itemId)] || 1,
          statusQualidade: this.statusQualidadeItens[parseInt(itemId)] as any || 'NORMAL'
        };

        if (item.statusQualidade === 'DEFEITUOSO') {
          item.percentualDesconto = this.descontoItens[parseInt(itemId)] || 0;
        }

        itens.push(item);
      }
    }

    const devolucaoRequest: DevolucaoRequest = {
      vendaId: this.vendaSelecionada.id,
      tipoDevolucao: this.devolucaoForm.value.tipoDevolucao,
      motivo: this.devolucaoForm.value.motivo,
      observacoes: this.devolucaoForm.value.observacoes,
      itens: itens
    };

    this.isLoading = true;
    this.errorMessage = '';

    this.devolucaoService.processarDevolucao(devolucaoRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = `Devolução #${response.id} processada com sucesso! Valor devolvido: ${this.formatarMoeda(response.valorTotal)}`;
        this.fecharModalDevolucao();
        this.loadVendas(); // Recarregar a lista
        this.loadStats(); // Recarregar estatísticas
        
        setTimeout(() => {
          this.successMessage = '';
        }, 7000);
      },
      error: (error) => {
        this.isLoading = false;
        console.error('❌ Erro ao processar devolução:', error);
        
        if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else if (error.status === 403) {
          this.errorMessage = 'Você não tem permissão para processar devoluções.';
        } else if (error.status === 400) {
          this.errorMessage = 'Dados inválidos. Verifique as informações e tente novamente.';
        } else if (error.status === 404) {
          this.errorMessage = 'Venda não encontrada ou não pode ser devolvida.';
        } else {
          this.errorMessage = 'Erro interno do servidor. Tente novamente em alguns minutos.';
        }
        
        setTimeout(() => {
          this.errorMessage = '';
        }, 8000);
      }
    });

    console.log('🔄 Processando devolução:', devolucaoRequest);
  }

}
