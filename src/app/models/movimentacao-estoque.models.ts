// ===================================
// INTERFACES E ENUMS - MOVIMENTAÇÃO DE ESTOQUE
// ===================================

export enum TipoMovimentacao {
  ENTRADA = 'ENTRADA',
  SAIDA = 'SAIDA',
  AJUSTE = 'AJUSTE',
  PERDA = 'PERDA',
  DEVOLUCAO = 'DEVOLUCAO'
}

// ===== INTERFACES REQUEST =====

export interface EntradaEstoqueRequest {
  produtoId: number;
  quantidade: number;
  motivo: string;
  observacoes?: string;
}

export interface SaidaEstoqueRequest {
  produtoId: number;
  quantidade: number;
  motivo: string;
  observacoes?: string;
}

export interface AjusteEstoqueRequest {
  produtoId: number;
  quantidadeNova: number;
  motivo: string;
  observacoes?: string;
}

export interface MovimentacaoEstoqueRequest {
  produtoId: number;
  tipo: TipoMovimentacao;
  quantidade: number;
  motivo: string;
  observacoes?: string;
}

// ===== INTERFACES RESPONSE =====

export interface MovimentacaoEstoqueResponse {
  id: number;
  produtoId: number;
  nomeProduto: string;
  codigoProduto: string;
  tipo: TipoMovimentacao;
  quantidade: number;
  quantidadeAnterior: number;
  quantidadeAtual: number;
  motivo: string;
  observacoes?: string;
  dataMovimentacao: Date;
  nomeUsuario: string;
  emailUsuario: string;
  vendaId?: number;
}

export interface MovimentacaoResumoResponse {
  id: number;
  nomeProduto: string;
  tipo: TipoMovimentacao;
  quantidade: number;
  dataMovimentacao: Date;
  nomeUsuario: string;
}

export interface EstatisticasMovimentacaoResponse {
  totalMovimentacoes: number;
  totalEntradas: number;
  totalSaidas: number;
  totalAjustes: number;
  quantidadeEntradas: number;
  quantidadeSaidas: number;
  saldoMovimentacao: number;
  movimentacoesHoje: number;
}

export interface HistoricoEstoqueResponse {
  produtoId: number;
  nomeProduto: string;
  movimentacoes: MovimentacaoResumoResponse[];
  estatisticas: {
    totalEntradas: number;
    totalSaidas: number;
    saldoFinal: number;
  };
}

// ===== INTERFACES DE FILTRO =====

export interface FiltroMovimentacaoRequest {
  produtoId?: number;
  tipo?: TipoMovimentacao;
  dataInicio?: Date;
  dataFim?: Date;
  usuarioId?: number;
  page?: number;
  size?: number;
}

// ===== INTERFACES DE PAGINAÇÃO =====

export interface PageableMovimentacaoResponse {
  content: MovimentacaoEstoqueResponse[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

// ===== UTILITÁRIOS =====

export interface MovimentacaoEstoqueUtils {
  formatarTipo(tipo: TipoMovimentacao): string;
  formatarQuantidade(quantidade: number, tipo: TipoMovimentacao): string;
  calcularVariacao(quantidadeAnterior: number, quantidadeAtual: number): number;
  isEntrada(tipo: TipoMovimentacao): boolean;
  isSaida(tipo: TipoMovimentacao): boolean;
}

// ===== CONSTANTES =====

export const TIPOS_MOVIMENTACAO_OPCOES = [
  { value: TipoMovimentacao.ENTRADA, label: 'Entrada', icon: 'bi-plus-circle', color: 'success' },
  { value: TipoMovimentacao.SAIDA, label: 'Saída', icon: 'bi-dash-circle', color: 'danger' },
  { value: TipoMovimentacao.AJUSTE, label: 'Ajuste', icon: 'bi-gear', color: 'warning' },
  { value: TipoMovimentacao.PERDA, label: 'Perda', icon: 'bi-x-circle', color: 'danger' },
  { value: TipoMovimentacao.DEVOLUCAO, label: 'Devolução', icon: 'bi-arrow-return-left', color: 'info' }
];

export const MOTIVOS_PADRAO = {
  [TipoMovimentacao.ENTRADA]: [
    'Compra de fornecedor',
    'Devolução de cliente',
    'Transferência de filial',
    'Ajuste de inventário',
    'Recebimento de estoque'
  ],
  [TipoMovimentacao.SAIDA]: [
    'Venda para cliente',
    'Devolução para fornecedor',
    'Transferência para filial',
    'Perda/Avaria',
    'Uso interno'
  ],
  [TipoMovimentacao.AJUSTE]: [
    'Inventário físico',
    'Correção de erro',
    'Acerto de sistema',
    'Auditoria de estoque'
  ],
  [TipoMovimentacao.PERDA]: [
    'Produto vencido',
    'Avaria/Dano',
    'Furto/Roubo',
    'Descarte'
  ],
  [TipoMovimentacao.DEVOLUCAO]: [
    'Devolução de venda',
    'Troca de produto',
    'Garantia',
    'Produto com defeito'
  ]
};