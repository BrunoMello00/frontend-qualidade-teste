import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';
import { MockDataService } from './mock-data.service';

export interface Produto {
  id?: number;
  nome: string;
  descricao?: string;
  preco: number;
  categoria?: string;
  codigoBarras?: string;
  codigo?: string;
  quantidade?: number;
  estoque?: number;
  quantidadeEstoque?: number;
  estoqueMinimo?: number;
  quantidadeMinima?: number;
  fornecedor?: string;
  temTamanhos?: boolean;
  tamanhos?: any[];
  ativo?: boolean;
  dataCriacao?: Date;
  dataAtualizacao?: Date;
  dataCadastro?: string;
  // Propriedades adicionais do backend ProdutoDTO.ProdutoResponse
  codigoResumido?: string;
  codigoInternoSequencial?: number;
  tipoCodigoBarras?: string;
  prefixoCodigo?: string;
  unidadeMedida?: string;
  estoqueBaixo?: boolean;
  // Informações de lock
  bloqueado?: boolean;
  bloqueadoPorUsuario?: string;
  bloqueioExpiraEm?: string;
  // Informações do código
  codigoValido?: boolean;
  padraoBrasileiro?: boolean;
  codigoFormatado?: string;
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
export class ProdutoService extends BaseService {
  constructor(
    http: HttpClient,
    environmentService: EnvironmentService,
    private mock: MockDataService
  ) {
    super(http, environmentService);
  }

  // Operações CRUD básicas
  listarProdutos(): Observable<Produto[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.listarProdutos() as Observable<Produto[]>;
    }
    return this.get<Produto[]>('produtos');
  }

  buscarPorId(id: number): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.buscarPorIdProduto(id) as Observable<Produto>;
    }
    return this.get<Produto>(`produtos/${id}`);
  }

  criarProduto(produto: Produto): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.criarProduto(produto) as Observable<Produto>;
    }
    return this.post<Produto>('produtos', produto);
  }

  atualizarProduto(id: number, produto: Produto): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.atualizarProduto(id, produto) as Observable<Produto>;
    }
    return this.put<Produto>(`produtos/${id}`, produto);
  }

  deletarProduto(id: number): Observable<void> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      // mock returns an object; map to void by ignoring response
      return this.mock.deletarProduto(id).pipe(map(() => undefined));
    }
    return this.delete<void>(`produtos/${id}`);
  }

  // Estoque e categorias
  atualizarEstoque(id: number, quantidade: number): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.atualizarEstoqueProduto(id, quantidade) as Observable<Produto>;
    }
    return this.patch<Produto>(`produtos/${id}/estoque`, { quantidade });
  }

  buscarCategorias(): Observable<string[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.buscarCategorias();
    }
    return this.get<string[]>('produtos/categorias');
  }

  buscarProdutosBaixoEstoque(): Observable<Produto[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.buscarProdutosBaixoEstoque() as Observable<Produto[]>;
    }
    return this.get<Produto[]>('produtos/baixo-estoque');
  }

  contarProdutos(): Observable<number> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.contarProdutos();
    }
    return this.get<{ total: number }>('produtos/count').pipe(
      map(response => response.total)
    );
  }

  // Código de barras
  listarTiposCodigoBarras(): Observable<string[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.listarTiposCodigoBarras();
    }
    return this.get<string[]>('produtos/tipos-codigo-barras');
  }

  gerarCodigoBarras(request: any): Observable<any> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.gerarCodigoBarras(request);
    }
    return this.post<any>('produtos/gerar-codigo-barras', request);
  }

  validarCodigoBarras(codigo: string, tipo: string): Observable<any> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.validarCodigoBarras(codigo, tipo);
    }
    return this.post<any>('produtos/validar-codigo-barras', { codigo, tipo });
  }

  // Desabilitar produto
  desabilitarProduto(id: number): Observable<boolean> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.deletarProduto(id).pipe(map((r: any) => !!(r && r.success)));
    }
    return this.patch<boolean>(`produtos/${id}/desabilitar`);
  }

  // Estatísticas por tamanho
  getEstatisticasPorTamanho(produtoId: number): Observable<any> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      // mock não implementa estatísticas por tamanho detalhadas, retornar estrutura vazia
      return this.mock.getTiposTamanho();
    }
    return this.get<any>(`produtos/${produtoId}/estatisticas-tamanho`);
  }

  // Criar produto com verificação
  criarProdutoComVerificacao(produto: any): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.criarProduto(produto) as Observable<Produto>;
    }
    return this.post<Produto>('produtos/criar-verificacao', produto);
  }

  // Buscar por código de barras
  buscarPorCodigoBarras(codigo: string): Observable<Produto> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.buscarPorCodigoBarras(codigo) as Observable<Produto>;
    }
    return this.get<Produto>(`produtos/codigo-barras/${codigo}`);
  }

  // Produtos ativos
  getProdutosAtivos(): Observable<Produto[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.getProdutosAtivos() as Observable<Produto[]>;
    }
    return this.get<Produto[]>('produtos/ativos');
  }

  // Tipos de tamanho
  getTiposTamanho(): Observable<any[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) {
      return this.mock.getTiposTamanho();
    }
    return this.get<any[]>('produtos/tipos-tamanho');
  }
}
