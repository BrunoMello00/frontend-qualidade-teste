import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

// ===== ENUMS =====
export enum TipoCodigoBarras {
  EAN13 = 'EAN13',
  EAN8 = 'EAN8',
  UPCA = 'UPCA',
  UPCE = 'UPCE',
  CODE128 = 'CODE128',
  CODE39 = 'CODE39',
  PERSONALIZADO = 'PERSONALIZADO'
}

// ===== INTERFACES PRINCIPAIS =====
export interface TamanhoProduto {
  id: number;
  nome: string; // Nome do tamanho (P, M, G, etc)
  tamanho: string; // Campo que vem do backend
  quantidade: number; // Quantidade disponível em estoque
  estoque?: number; // Campo que vem do backend
  preco?: number; // Preço específico do tamanho (opcional)
  produtoId?: number; // ID do produto
  codigoBarras?: string;
  ativo?: boolean;
  vendidas?: number;
}

export interface ProdutoRequest {
  nome: string;
  descricao?: string;
  preco: number;
  categoria?: string;
  departamento?: string; // Alias para categoria
  estoqueMinimo?: number;
  codigoBarras?: string;
  tipoCodigoBarras?: TipoCodigoBarras;
  prefixoCodigo?: string;
  gerarCodigoAutomatico?: boolean;
  unidadeMedida?: string;
  margem?: number;  // Campo de margem de lucro
  fornecedor?: string;  // Campo de fornecedor
  codigoFornecedor?: string;  // Código do fornecedor
  custoUnitario?: number;  // Custo unitário do produto
  pontuacaoProduto?: number; // Pontuação que o produto concede ao cliente
  tamanhos?: any[]; // Lista de tamanhos do produto
}

export interface ProdutoResponse {
  id: number;
  nome: string;
  descricao?: string;
  preco: number;
  estoque: number; // 🔧 Campo de estoque
  quantidadeEstoque?: number; // Campo que vem do backend
  quantidade?: number; // Opcional agora
  departamento?: string; // 🔧 Departamento em vez de categoria
  estoqueMinimo: number;
  codigoBarras?: string;
  codigoResumido?: string;
  codigoInternoSequencial?: number;
  tipoCodigoBarras?: TipoCodigoBarras;
  prefixoCodigo?: string;
  unidadeMedida?: string;
  ativo: boolean;
  dataCadastro: string;
  dataAtualizacao?: string;
  estoqueBaixo: boolean;
  margem?: number;  // Campo de margem de lucro
  fornecedor?: string;  // Campo de fornecedor
  codigoFornecedor?: string;  // Código do fornecedor
  custoUnitario?: number;  // Custo unitário do produto
  pontuacaoProduto?: number; // Pontuação que o produto concede ao cliente
  tamanhos?: TamanhoProduto[]; // 🔧 Lista de tamanhos do produto
  bloqueado: boolean;
  bloqueadoPorUsuario?: string;
  bloqueioExpiraEm?: string;
  codigoValido: boolean;
  padraoBrasileiro: boolean;
  codigoFormatado?: string;
}

export interface EstoqueRequest {
  quantidade: number;
  observacao?: string;
}

export interface ProdutoResumo {
  id: number;
  nome: string;
  estoqueMinimo: number;
  preco: number;
  estoqueBaixo: boolean;
}

export interface EstatisticasResponse {
  totalProdutos: number;
  produtosAtivos: number;
  produtosEstoqueBaixo: number;
  valorTotalEstoque: number;
}

export interface CodigoBarrasRequest {
  tipoCodigoBarras: TipoCodigoBarras;
  prefixo?: string;
  gerarAutomaticamente?: boolean;
  codigoManual?: string;
}

export interface LockRequest {
  produtoId: number;
  tipoLock?: string;
  tempoExpiracaoMinutos?: number;
}

export interface LockResponse {
  sucesso: boolean;
  mensagem: string;
  bloqueadoPorUsuario?: string;
  bloqueioExpiraEm?: string;
  podeEditar: boolean;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  size: number;
  number: number;
}

export interface FiltroProdutos {
  nome?: string;
  departamento?: string; // 🔧 Mudança de categoria para departamento
  estoqueBaixo?: boolean;
  ativo?: boolean;
  codigoBarras?: string;
  precoMin?: number;
  precoMax?: number;
  estoqueMin?: number;
  estoqueMax?: number;
}

export interface Produto extends ProdutoResponse {
  codigo?: string;
  fornecedor?: string;
  temTamanhos?: boolean;
  tamanhos?: any[];
  dataCriacao?: Date;
}

export interface ProdutoEstatisticas {
  produtoId: number;
  quantidadeVendida: number;
  ultimaVenda?: Date;
  diasSemVender: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProdutoService {
  private readonly apiUrl = `${environment.apiUrl}/produtos`;
  
  private categoriasSubject = new BehaviorSubject<string[]>([]);
  public categorias$ = this.categoriasSubject.asObservable();

  // Mapeamento de códigos de departamento para nomes
  private departamentos: { [key: string]: string } = {
    '01': 'Alimentação',
    '02': 'Bebidas', 
    '03': 'Limpeza',
    '04': 'Higiene',
    '05': 'Eletrônicos',
    '06': 'Roupas',
    '07': 'Casa & Decoração',
    '08': 'Esportes',
    '09': 'Livros & Papelaria',
    '10': 'Outros'
  };

  private estatisticasSubject = new BehaviorSubject<EstatisticasResponse | null>(null);
  public estatisticas$ = this.estatisticasSubject.asObservable();

  constructor(private http: HttpClient) {
    this.carregarCategorias();
  }

  // ===== OPERAÇÕES CRUD BÁSICAS =====

  /**
   * Lista produtos com paginação e filtros
   */
  listarProdutos(
    filtros: FiltroProdutos = {}, 
    page: number = 0, 
    size: number = 10
  ): Observable<PaginatedResponse<ProdutoResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filtros.nome) params = params.set('nome', filtros.nome);
    if (filtros.departamento) params = params.set('departamento', filtros.departamento);

    return this.http.get<PaginatedResponse<ProdutoResponse>>(this.apiUrl, { params })
      .pipe(
        map(response => ({
          ...response,
          content: response.content.map(produto => this.mapProdutoResponse(produto))
        }))
      );
  }

  /**
   * 🔧 Mapeia resposta do backend para interface frontend
   */
  private mapProdutoResponse(produto: any): ProdutoResponse {
    return {
      ...produto,
      // ✅ Prioriza quantidadeEstoque (que vem do getEstoqueTotal())
      estoque: produto.quantidadeEstoque || produto.estoque || 0,
      tamanhos: produto.tamanhos ? produto.tamanhos.map((tamanho: any) => ({
        ...tamanho,
        nome: tamanho.tamanho, // Mapear tamanho para nome
        quantidade: tamanho.estoque || 0 // Mapear estoque para quantidade
      })) : []
    };
  }

  /**
   * Busca produto por ID
   */
  buscarPorId(id: number): Observable<ProdutoResponse> {
    return this.http.get<any>(`${this.apiUrl}/${id}`)
      .pipe(map(produto => this.mapProdutoResponse(produto)));
  }

  /**
   * Cria novo produto
   */
  criarProduto(produto: ProdutoRequest): Observable<ProdutoResponse> {
    return this.http.post<ProdutoResponse>(this.apiUrl, produto);
  }

  /**
   * Atualiza produto existente
   */
  atualizarProduto(id: number, produto: ProdutoRequest, usuarioId?: number): Observable<ProdutoResponse> {
    let params = new HttpParams();
    if (usuarioId) params = params.set('usuarioId', usuarioId.toString());

    return this.http.put<ProdutoResponse>(`${this.apiUrl}/${id}`, produto, { params });
  }

  /**
   * Exclui produto (marca como inativo)
   */
  excluirProduto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
  /**
   * Reativa produto desabilitado
   */
  reativarProduto(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/reativar`, {});
  }
  
  /**
   * Lista produtos desabilitados (para administração)
   */
  listarProdutosDesabilitados(page: number = 0, size: number = 10): Observable<PaginatedResponse<ProdutoResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PaginatedResponse<ProdutoResponse>>(`${this.apiUrl}/desabilitados`, { params });
  }

  // ===== GESTÃO DE ESTOQUE =====

  /**
   * Lista produtos com estoque baixo
   */
  buscarProdutosEstoqueBaixo(limite: number = 5): Observable<ProdutoResumo[]> {
    const params = new HttpParams().set('limite', limite.toString());
    return this.http.get<ProdutoResumo[]>(`${this.apiUrl}/estoque-baixo`, { params });
  }

  /**
   * Atualiza estoque do produto
   */
  atualizarEstoque(id: number, estoqueRequest: EstoqueRequest): Observable<ProdutoResponse> {
    return this.http.patch<ProdutoResponse>(`${this.apiUrl}/${id}/estoque`, estoqueRequest);
  }

  /**
   * Busca tamanhos de um produto
   */
  buscarTamanhosPorProdutoId(produtoId: number): Observable<TamanhoProduto[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${produtoId}/tamanhos`)
      .pipe(
        map(tamanhos => tamanhos.map(tamanho => ({
          ...tamanho,
          nome: tamanho.tamanho, // Mapear tamanho para nome
          quantidade: tamanho.estoque || 0 // Mapear estoque para quantidade
        })))
      );
  }

  /**
   * Busca tamanhos com estoque disponível (> 0) para um produto
   */
  buscarTamanhosComEstoque(produtoId: number): Observable<TamanhoProduto[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${produtoId}/tamanhos/com-estoque`)
      .pipe(
        map(tamanhos => tamanhos.map(tamanho => ({
          ...tamanho,
          nome: tamanho.tamanho || tamanho.nome, // Mapear tamanho para nome
          quantidade: tamanho.estoque || tamanho.quantidade || 0 // Mapear estoque para quantidade
        })))
      );
  }

  // ===== CATEGORIAS =====

  /**
   * Lista todas as categorias
   */
  listarCategorias(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/departamentos`).pipe(
      map(codigos => {
        // Mapear códigos para nomes dos departamentos
        const nomes = codigos.map(codigo => this.departamentos[codigo] || codigo).filter(nome => nome);
        this.categoriasSubject.next(nomes);
        return nomes;
      })
    );
  }

  /**
   * Carrega departamentos para o cache
   */
  private carregarCategorias(): void {
    this.listarCategorias().subscribe({
      error: (error) => console.error('Erro ao carregar departamentos:', error)
    });
  }

  /**
   * Função para obter o nome do departamento a partir do código
   */
  getNomeDepartamento(codigo: string | undefined): string {
    if (!codigo) {
      return 'Sem departamento';
    }
    return this.departamentos[codigo] || codigo || 'Sem departamento';
  }

  // ===== CÓDIGOS DE BARRAS =====

  /**
   * Busca produto por código de barras
   */
  buscarPorCodigoBarras(codigo: string): Observable<ProdutoResponse> {
    return this.http.get<ProdutoResponse>(`${this.apiUrl}/codigo-barras/${codigo}`);
  }

  /**
   * Busca produto por código resumido
   */
  buscarPorCodigoResumido(codigo: string): Observable<ProdutoResponse> {
    return this.http.get<ProdutoResponse>(`${this.apiUrl}/codigo-resumido/${codigo}`);
  }

  /**
   * Lista tipos de código de barras suportados
   */
  listarTiposCodigoSuportados(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/tipos-codigo`);
  }

  /**
   * Gera código personalizado
   */
  gerarCodigoPersonalizado(request: CodigoBarrasRequest): Observable<{codigo: string, tipo: string, prefixo: string}> {
    return this.http.post<{codigo: string, tipo: string, prefixo: string}>(`${this.apiUrl}/gerar-codigo`, request);
  }

  // ===== SISTEMA DE LOCKS =====

  /**
   * Adquire lock exclusivo para editar produto
   */
  adquirirLock(id: number, usuarioId?: number): Observable<LockResponse> {
    let params = new HttpParams();
    if (usuarioId) params = params.set('usuarioId', usuarioId.toString());

    return this.http.post<LockResponse>(`${this.apiUrl}/${id}/lock`, {}, { params });
  }

  /**
   * Libera lock do produto
   */
  liberarLock(id: number, usuarioId?: number): Observable<LockResponse> {
    let params = new HttpParams();
    if (usuarioId) params = params.set('usuarioId', usuarioId.toString());

    return this.http.delete<LockResponse>(`${this.apiUrl}/${id}/lock`, { params });
  }

  /**
   * Verifica status do lock
   */
  verificarStatusLock(id: number, usuarioId?: number): Observable<LockResponse> {
    let params = new HttpParams();
    if (usuarioId) params = params.set('usuarioId', usuarioId.toString());

    return this.http.get<LockResponse>(`${this.apiUrl}/${id}/lock/status`, { params });
  }

  // ===== ESTATÍSTICAS =====

  /**
   * Obtém estatísticas gerais dos produtos
   */
  obterEstatisticas(): Observable<EstatisticasResponse> {
    const estatisticas$ = this.http.get<EstatisticasResponse>(`${this.apiUrl}/estatisticas`);
    
    estatisticas$.subscribe(stats => this.estatisticasSubject.next(stats));
    
    return estatisticas$;
  }

  // ===== MÉTODOS UTILITÁRIOS =====

  /**
   * Formatar moeda
   */
  formatarMoeda(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor || 0);
  }

  /**
   * Formatar data
   */
  formatarData(data: string): string {
    if (!data) return 'N/A';
    
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      }).format(new Date(data));
    } catch {
      return data;
    }
  }

  /**
   * Formatar data/hora
   */
  formatarDataHora(data: string): string {
    if (!data) return 'N/A';
    
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(data));
    } catch {
      return data;
    }
  }

  /**
   * Formatar número
   */
  formatarNumero(valor: number): string {
    return new Intl.NumberFormat('pt-BR').format(valor || 0);
  }

  /**
   * Validar código de barras básico
   */
  validarCodigoBarras(codigo: string): boolean {
    if (!codigo) return false;
    
    const codigoLimpo = codigo.replace(/\D/g, '');
    
    return [8, 12, 13, 14].includes(codigoLimpo.length);
  }

  /**
   * Obter cor do status do estoque - CAMPO REMOVIDO: baseado apenas em status geral
   */
  getCorStatusEstoque(produto: ProdutoResponse): string {
    return '#28a745'; // Verde - produto ativo
  }

  /**
   * Obter texto do status do estoque - CAMPO REMOVIDO: baseado apenas em status geral
   */
  getStatusEstoque(produto: ProdutoResponse): string {
    return 'Produto Ativo';
  }

  /**
   * Obter cor do tipo de código de barras
   */
  getCorTipoCodigoBarras(tipo: TipoCodigoBarras): string {
    switch (tipo) {
      case TipoCodigoBarras.EAN13: return '#0d6efd';
      case TipoCodigoBarras.EAN8: return '#20c997';
      case TipoCodigoBarras.UPCA: return '#fd7e14';
      case TipoCodigoBarras.UPCE: return '#6f42c1';
      case TipoCodigoBarras.CODE128: return '#dc3545';
      case TipoCodigoBarras.CODE39: return '#198754';
      case TipoCodigoBarras.PERSONALIZADO: return '#6c757d';
      default: return '#6c757d';
    }
  }

  /**
   * Calcular valor total do estoque
   */
  /**
   * CAMPO REMOVIDO: calcularValorTotalEstoque - estoque não é mais gerenciado no catálogo
   */
  calcularValorTotalEstoque(produtos: ProdutoResponse[]): number {
    return produtos.reduce((total, produto) => {
      return total + produto.preco;
    }, 0);
  }

  /**
   * Filtrar produtos
   */
  filtrarProdutos(produtos: ProdutoResponse[], filtros: FiltroProdutos): ProdutoResponse[] {
    return produtos.filter(produto => {
      if (filtros.nome && !produto.nome.toLowerCase().includes(filtros.nome.toLowerCase())) {
        return false;
      }
      
      if (filtros.departamento && produto.departamento !== filtros.departamento) {
        return false;
      }
      
      if (filtros.estoqueBaixo !== undefined && produto.estoqueBaixo !== filtros.estoqueBaixo) {
        return false;
      }
      
      if (filtros.ativo !== undefined && produto.ativo !== filtros.ativo) {
        return false;
      }
      
      if (filtros.codigoBarras && !produto.codigoBarras?.includes(filtros.codigoBarras)) {
        return false;
      }
      
      if (filtros.precoMin && produto.preco < filtros.precoMin) {
        return false;
      }
      
      if (filtros.precoMax && produto.preco > filtros.precoMax) {
        return false;
      }
      
      
      return true;
    });
  }

  /**
   * Limpar cache
   */
  limparCache(): void {
    this.categoriasSubject.next([]);
    this.estatisticasSubject.next(null);
  }

  // ===== MÉTODOS LEGACY (COMPATIBILIDADE) =====

  listarProdutos_Legacy(): Observable<Produto[]> {
    return this.listarProdutos().pipe(
      map(response => response.content.map(produto => ({
        ...produto,
        codigo: produto.codigoResumido,
        quantidade: 0, // Valor padrão para compatibilidade
        estoque: 0, // Valor padrão para compatibilidade
        quantidadeMinima: produto.estoqueMinimo,
        dataCriacao: new Date(produto.dataCadastro)
      })))
    );
  }

  criarProduto_Legacy(produto: Produto): Observable<Produto> {
    const produtoRequest: ProdutoRequest = {
      nome: produto.nome,
      descricao: produto.descricao,
      preco: produto.preco,
      departamento: produto.departamento,
      estoqueMinimo: produto.estoqueMinimo || 5, // Removido quantidadeMinima
      codigoBarras: produto.codigoBarras,
      unidadeMedida: produto.unidadeMedida
    };

    return this.criarProduto(produtoRequest).pipe(
      map(response => ({
        ...response,
        codigo: response.codigoResumido,
        quantidade: 0, // Valor padrão
        estoque: 0, // Valor padrão
        quantidadeMinima: response.estoqueMinimo,
        dataCriacao: new Date(response.dataCadastro)
      }))
    );
  }

  atualizarProduto_Legacy(id: number, produto: Produto): Observable<Produto> {
    const produtoRequest: ProdutoRequest = {
      nome: produto.nome,
      descricao: produto.descricao,
      preco: produto.preco,
      departamento: produto.departamento,
      estoqueMinimo: produto.estoqueMinimo || 5, // Removido quantidadeMinima
      codigoBarras: produto.codigoBarras,
      unidadeMedida: produto.unidadeMedida
    };

    return this.atualizarProduto(id, produtoRequest).pipe(
      map(response => ({
        ...response,
        codigo: response.codigoResumido,
        quantidade: 0, // Valor padrão
        estoque: 0, // Valor padrão
        quantidadeMinima: response.estoqueMinimo,
        dataCriacao: new Date(response.dataCadastro)
      }))
    );
  }

  deletarProduto(id: number): Observable<void> {
    return this.excluirProduto(id);
  }

  atualizarEstoque_Legacy(id: number, quantidade: number): Observable<Produto> {
    return this.atualizarEstoque(id, { quantidade }).pipe(
      map(response => ({
        ...response,
        codigo: response.codigoResumido,
        quantidade: 0, // Valor padrão
        estoque: 0, // Valor padrão
        quantidadeMinima: response.estoqueMinimo,
        dataCriacao: new Date(response.dataCadastro)
      }))
    );
  }

  buscarCategorias(): Observable<string[]> {
    return this.listarCategorias();
  }

  buscarProdutosBaixoEstoque(): Observable<Produto[]> {
    return this.buscarProdutosEstoqueBaixo().pipe(
      map(produtos => produtos.map(produto => ({
        ...produto,
        codigo: '',
        nome: produto.nome,
        preco: produto.preco,
        quantidade: 0, // Valor padrão
        estoque: 0, // Valor padrão
        estoqueMinimo: produto.estoqueMinimo,
        quantidadeMinima: produto.estoqueMinimo,
        ativo: true,
        dataCadastro: new Date().toISOString(),
        dataCriacao: new Date(),
        estoqueBaixo: produto.estoqueBaixo,
        bloqueado: false,
        codigoValido: true,
        padraoBrasileiro: false
      })))
    );
  }

  contarProdutos(): Observable<number> {
    return this.obterEstatisticas().pipe(
      map(stats => stats.totalProdutos)
    );
  }

  getProdutosAtivos(): Observable<Produto[]> {
    // ✅ Buscar todos os produtos ativos (size=1000 para garantir que pegue todos)
    return this.listarProdutos({ ativo: true }, 0, 1000).pipe(
      map(response => response.content.map(produto => ({
        ...produto,
        codigo: produto.codigoResumido,
        // ✅ CORREÇÃO: Usar estoque correto do produto (já mapeado)
        quantidade: produto.quantidadeEstoque || produto.estoque || 0,
        estoque: produto.quantidadeEstoque || produto.estoque || 0,
        quantidadeMinima: produto.estoqueMinimo,
        dataCriacao: new Date(produto.dataCadastro)
      })))
    );
  }

  listarProdutosBaixoEstoque(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/baixo-estoque`).pipe(
      map(response => response.content || response),
      catchError(error => {
        console.error('Erro ao listar produtos com baixo estoque:', error);
        return of([]);
      })
    );
  }
}
