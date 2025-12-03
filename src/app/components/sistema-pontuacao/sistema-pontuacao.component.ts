import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { 
  PontuacaoService, 
  ClientePontuacao, 
  Recompensa, 
  EstatisticasPontuacao,
  CategoriaConfig,
  TransacaoPontos,
  ResgatePontos
} from '../../services/pontuacao.service';

@Component({
  selector: 'app-sistema-pontuacao',
  templateUrl: './sistema-pontuacao.component.html',
  styleUrls: ['./sistema-pontuacao.component.css']
})
export class SistemaPontuacaoComponent implements OnInit {

  clientes: ClientePontuacao[] = [];
  recompensas: Recompensa[] = [];
  categorias: CategoriaConfig[] = [];
  historicoTransacoes: (TransacaoPontos & {
    id?: number;
    data?: Date;
    tipo?: string;
    status?: string;
    descricao?: string;
  })[] = [];
  stats: EstatisticasPontuacao = {
    totalClientes: 0,
    totalPontosAtivos: 0,
    mediaPontosCliente: 0,
    totalResgates: 0,
    totalRecompensasAtivas: 0
  };

  isLoading = false;
  isSaving = false;
  errorMessage = '';
  successMessage = '';
  activeTab = 'clientes';

  showModalPontos = false;
  clienteSelecionado: ClientePontuacao | null = null;
  pontosForm: FormGroup;

  showModalCategoria = false;
  showModalRecompensa = false;
  categoriaSelecionada: CategoriaConfig | null = null;
  categoriaForm: FormGroup;
  recompensaForm: FormGroup;

  searchTerm = '';
  selectedNivel = '';
  selectedCategoria = '';
  
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;

  constructor(
    private fb: FormBuilder,
    private pontuacaoService: PontuacaoService
  ) {
    this.pontosForm = this.fb.group({
      tipoOperacao: ['ADICIONAR', Validators.required],
      pontos: ['', [Validators.required, Validators.min(1)]],
      descricao: ['', Validators.required]
    });

    this.categoriaForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      descricao: [''],
      pontosMinimos: ['', [Validators.required, Validators.min(0)]],
      pontosMaximos: [''],
      pontosIniciais: ['', [Validators.required, Validators.min(0)]],
      cor: ['#007bff', Validators.required],
      ativo: [true]
    });

    this.recompensaForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      descricao: [''],
      pontosNecessarios: ['', [Validators.required, Validators.min(1)]],
      categoria: ['Geral'],
      valorDesconto: [''],
      percentualDesconto: [''],
      quantidadeDisponivel: [''],
      dataValidade: [''],
      ativo: [true]
    });
  }

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.isLoading = true;
    this.clearMessages();
    
    Promise.all([
      this.carregarClientes(),
      this.carregarRecompensas(),
      this.carregarCategorias(),
      this.carregarEstatisticas(),
      this.carregarHistoricoTransacoes()
    ]).then(() => {
      this.isLoading = false;
    }).catch(error => {
      console.error('Erro ao carregar dados:', error);
      this.errorMessage = 'Erro ao carregar dados do sistema de pontuação';
      this.isLoading = false;
    });
  }

  carregarClientes(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.pontuacaoService.listarClientesComPontuacao(this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.clientes = response.content || response;
            this.totalPages = response.totalPages || 1;
            resolve();
          },
          error: (error) => {
            console.error('Erro ao carregar clientes:', error);
            reject(error);
          }
        });
    });
  }

  carregarRecompensas(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.pontuacaoService.listarRecompensas()
        .subscribe({
          next: (recompensas) => {
            this.recompensas = recompensas;
            resolve();
          },
          error: (error) => {
            console.error('Erro ao carregar recompensas:', error);
            reject(error);
          }
        });
    });
  }

  carregarCategorias(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.pontuacaoService.listarCategorias()
        .subscribe({
          next: (categorias) => {
            this.categorias = categorias;
            resolve();
          },
          error: (error) => {
            console.error('Erro ao carregar categorias:', error);
            reject(error);
          }
        });
    });
  }

  carregarEstatisticas(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.pontuacaoService.obterEstatisticas()
        .subscribe({
          next: (stats) => {
            this.stats = stats;
            resolve();
          },
          error: (error) => {
            console.error('Erro ao carregar estatísticas:', error);
            reject(error);
          }
        });
    });
  }

  carregarHistoricoTransacoes(): Promise<void> {
    return new Promise((resolve) => {
      this.historicoTransacoes = [
        {
          id: 1,
          clienteId: 1,
          pontos: 50,
          motivo: 'Compra realizada',
          descricao: 'Compra no valor de R$ 250,00',
          data: new Date(),
          tipo: 'CREDITO',
          status: 'CONFIRMADO'
        },
        {
          id: 2,
          clienteId: 2,
          pontos: -30,
          motivo: 'Resgate de recompensa',
          descricao: 'Resgate de desconto 10%',
          data: new Date(Date.now() - 86400000),
          tipo: 'DEBITO',
          status: 'CONFIRMADO'
        }
      ];
      resolve();
    });
  }

  getFilteredClientes(): ClientePontuacao[] {
    let filtered = [...this.clientes];

    if (this.searchTerm) {
      filtered = filtered.filter(cliente => 
        cliente.nome.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        cliente.email.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    if (this.selectedNivel) {
      filtered = filtered.filter(cliente => cliente.categoriaAtual === this.selectedNivel);
    }

    return filtered.sort((a, b) => b.pontos - a.pontos);
  }

  getFilteredRecompensas(): Recompensa[] {
    let filtered = [...this.recompensas];

    if (this.selectedCategoria) {
      filtered = filtered.filter(recompensa => recompensa.categoria === this.selectedCategoria);
    }

    return filtered.sort((a, b) => a.pontosNecessarios - b.pontosNecessarios);
  }

  getNivelColor(categoria: string): string {
    const cliente = this.clientes.find(c => c.categoriaAtual === categoria);
    return cliente?.corCategoria || this.pontuacaoService.obterCorCategoria(categoria);
  }

  getProgressoNivel(cliente: ClientePontuacao): number {
    return cliente.progressoCategoria || 0;
  }

  formatarData(data: Date | string | undefined): string {
    if (!data) return 'N/A';
    const dateObj = typeof data === 'string' ? new Date(data) : data;
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(dateObj);
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  openModalPontos(cliente: ClientePontuacao): void {
    this.clienteSelecionado = cliente;
    this.showModalPontos = true;
    this.pontosForm.reset();
    this.pontosForm.patchValue({
      tipoOperacao: 'ADICIONAR'
    });
  }

  closeModalPontos(): void {
    this.showModalPontos = false;
    this.clienteSelecionado = null;
    this.pontosForm.reset();
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.pontosForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  calcularPontosFinal(): number {
    if (!this.clienteSelecionado || !this.pontosForm.get('pontos')?.value) {
      return this.clienteSelecionado?.pontos || 0;
    }

    const pontosOperacao = Number(this.pontosForm.get('pontos')?.value);
    const tipoOperacao = this.pontosForm.get('tipoOperacao')?.value;
    
    if (tipoOperacao === 'ADICIONAR') {
      return this.clienteSelecionado.pontos + pontosOperacao;
    } else {
      return Math.max(0, this.clienteSelecionado.pontos - pontosOperacao);
    }
  }

  onSubmitPontos(): void {
    if (this.pontosForm.invalid || !this.clienteSelecionado) {
      this.pontosForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.clearMessages();
    
    const pontosOperacao = Number(this.pontosForm.get('pontos')?.value);
    const tipoOperacao = this.pontosForm.get('tipoOperacao')?.value;
    const descricao = this.pontosForm.get('descricao')?.value;

    const transacao: TransacaoPontos = {
      clienteId: this.clienteSelecionado.id,
      pontos: tipoOperacao === 'ADICIONAR' ? pontosOperacao : -pontosOperacao,
      motivo: descricao,
      observacoes: `${tipoOperacao === 'ADICIONAR' ? 'Adição' : 'Remoção'} manual de pontos`
    };

    setTimeout(() => {
      if (tipoOperacao === 'ADICIONAR') {
        this.clienteSelecionado!.pontos += pontosOperacao;
        this.successMessage = `${pontosOperacao} pontos adicionados para ${this.clienteSelecionado!.nome}`;
      } else {
        this.clienteSelecionado!.pontos = Math.max(0, this.clienteSelecionado!.pontos - pontosOperacao);
        this.successMessage = `${pontosOperacao} pontos removidos de ${this.clienteSelecionado!.nome}`;
      }

      this.carregarEstatisticas();
      this.closeModalPontos();
      this.isSaving = false;
      
      setTimeout(() => this.successMessage = '', 5000);
    }, 1500);
  }

  adicionarPontos(cliente: ClientePontuacao): void {
    this.openModalPontos(cliente);
  }

  resgatarRecompensa(cliente: ClientePontuacao, recompensa: Recompensa): void {
    if (cliente.pontos >= recompensa.pontosNecessarios) {
      const resgate: ResgatePontos = {
        clienteId: cliente.id,
        recompensaId: recompensa.id!,
        observacoes: `Resgate da recompensa: ${recompensa.nome}`
      };

      this.successMessage = `Recompensa "${recompensa.nome}" resgatada por ${cliente.nome}`;
      setTimeout(() => this.successMessage = '', 3000);
    } else {
      this.errorMessage = `${cliente.nome} não possui pontos suficientes`;
      setTimeout(() => this.errorMessage = '', 3000);
    }
  }

  limparFiltros(): void {
    this.searchTerm = '';
    this.selectedNivel = '';
    this.selectedCategoria = '';
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.selectedNivel || this.selectedCategoria);
  }

  isFilterActive(field: string): boolean {
    switch (field) {
      case 'search': return !!this.searchTerm;
      case 'nivel': return !!this.selectedNivel;
      case 'categoria': return !!this.selectedCategoria;
      default: return false;
    }
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  formatarPontos(pontos: number): string {
    return this.pontuacaoService.formatarPontos(pontos);
  }

  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.carregarClientes();
    }
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.carregarClientes();
    }
  }

  irParaPagina(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.carregarClientes();
    }
  }

  get niveis(): string[] {
    return this.categorias.map(c => c.nome);
  }

  get categorias_recompensas(): string[] {
    return [...new Set(this.recompensas.map(r => r.categoria || 'Geral'))];
  }


  editarCategoria(categoria: CategoriaConfig): void {
    this.categoriaSelecionada = categoria;
    this.categoriaForm.patchValue({
      nome: categoria.nome,
      descricao: categoria.descricao || '',
      pontosMinimos: categoria.pontosMinimos,
      pontosMaximos: categoria.pontosMaximos || '',
      pontosIniciais: categoria.pontosIniciais,
      cor: categoria.cor,
      ativo: categoria.ativo
    });
    this.showModalCategoria = true;
  }

  confirmarRemocaoCategoria(categoria: CategoriaConfig): void {
    if (confirm(`Tem certeza que deseja remover a categoria "${categoria.nome}"?`)) {
      this.removerCategoria(categoria);
    }
  }

  removerCategoria(categoria: CategoriaConfig): void {
    if (!categoria.id) return;

    this.pontuacaoService.removerCategoria(categoria.id)
      .subscribe({
        next: () => {
          this.successMessage = `Categoria "${categoria.nome}" removida com sucesso`;
          this.carregarCategorias();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          console.error('Erro ao remover categoria:', error);
          this.errorMessage = 'Erro ao remover categoria. Verifique se não há clientes vinculados.';
          setTimeout(() => this.errorMessage = '', 5000);
        }
      });
  }

  onSubmitCategoria(): void {
    if (this.categoriaForm.invalid) {
      this.categoriaForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.clearMessages();

    const categoriaData: CategoriaConfig = {
      ...this.categoriaForm.value,
      pontosMaximos: this.categoriaForm.value.pontosMaximos || null
    };

    const operacao = this.categoriaSelecionada
      ? this.pontuacaoService.atualizarCategoria(this.categoriaSelecionada.id!, categoriaData)
      : this.pontuacaoService.criarCategoria(categoriaData);

    operacao.subscribe({
      next: (categoria) => {
        const acao = this.categoriaSelecionada ? 'atualizada' : 'criada';
        this.successMessage = `Categoria "${categoria.nome}" ${acao} com sucesso`;
        this.closeModalCategoria();
        this.carregarCategorias();
        this.isSaving = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        console.error('Erro ao salvar categoria:', error);
        this.errorMessage = 'Erro ao salvar categoria. Verifique os dados informados.';
        this.isSaving = false;
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  closeModalCategoria(): void {
    this.showModalCategoria = false;
    this.categoriaSelecionada = null;
    this.categoriaForm.reset({
      cor: '#007bff',
      ativo: true
    });
  }


  editarRecompensa(recompensa: Recompensa): void {
  }

  criarRecompensa(): void {
    this.showModalRecompensa = true;
    this.recompensaForm.reset({
      categoria: 'Geral',
      ativo: true
    });
  }

  getRecentTransacoes(): (TransacaoPontos & { id?: number; data?: Date; tipo?: string; status?: string; descricao?: string; })[] {
    return this.historicoTransacoes.slice(0, 10); // Últimas 10 transações
  }

  getClienteNome(clienteId: number): string {
    const cliente = this.clientes.find(c => c.id === clienteId);
    return cliente ? cliente.nome : 'Cliente não encontrado';
  }

  getHistoricoCliente(clienteId: number | undefined): (TransacaoPontos & { id?: number; data?: Date; tipo?: string; status?: string; descricao?: string; })[] {
    if (!clienteId) return [];
    return this.historicoTransacoes.filter((t: any) => t.clienteId === clienteId);
  }

  closeModalRecompensa(): void {
    this.showModalRecompensa = false;
    this.recompensaForm.reset({
      categoria: 'Geral',
      ativo: true
    });
  }
}