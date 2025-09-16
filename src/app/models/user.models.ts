// ===================================
// ENUMS E TIPOS
// ===================================

export enum TipoUsuario {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  VENDEDOR = 'VENDEDOR'
}

export enum StatusUsuario {
  ATIVO = 'ATIVO',
  INATIVO = 'INATIVO',
  BLOQUEADO = 'BLOQUEADO',
  PENDENTE = 'PENDENTE'
}

export enum TipoAcao {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  PASSWORD_CHANGE = 'PASSWORD_CHANGE',
  ACCESS_DENIED = 'ACCESS_DENIED'
}

// ===================================
// INTERFACES PRINCIPAIS
// ===================================

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  codigoVendedor?: string;
  status: StatusUsuario;
  primeiroAcesso: boolean;
  ultimoLogin?: Date;
  tentativasLogin: number;
  bloqueadoAte?: Date;
  dataCriacao: Date;
  dataAtualizacao: Date;
  criadoPor?: string;
  atualizadoPor?: string;
  avatarUrl?: string;
  telefone?: string;
  observacoes?: string;
  metaMensal: number;
  comissaoPercentual: number;
  ativo: boolean;
}

export interface NovoUsuario {
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  telefone?: string;
  observacoes?: string;
  metaMensal?: number;
  comissaoPercentual?: number;
}

export interface Permissao {
  id: string;
  nome: string;
  descricao: string;
  modulo: string;
  acao: string;
  dataCriacao: Date;
}

export interface PermissaoUsuario {
  permissao: Permissao;
  concedida: boolean;
}

export interface AuditoriaLog {
  id: string;
  usuarioId: string;
  nomeUsuario: string;
  tipoAcao: TipoAcao;
  modulo: string;
  recurso: string;
  recursoId?: string;
  dadosAnteriores?: any;
  dadosNovos?: any;
  ipAddress: string;
  userAgent: string;
  timestamp: Date;
  sucesso: boolean;
  detalhes?: string;
}

export interface SessaoUsuario {
  id: string;
  usuarioId: string;
  tokenHash: string;
  ipAddress: string;
  userAgent: string;
  dataCriacao: Date;
  dataExpiracao: Date;
  ativo: boolean;
  ultimoAcesso: Date;
}

export interface MetaVendedor {
  id: string;
  vendedorId: string;
  ano: number;
  mes: number;
  metaVendas: number;
  metaQuantidade: number;
  valorAtingido: number;
  quantidadeAtingida: number;
  comissaoCalculada: number;
  bonus: number;
  observacoes?: string;
  dataCriacao: Date;
  criadoPor: string;
}

export interface EstatisticasVendedor {
  id: string;
  nome: string;
  codigoVendedor: string;
  email: string;
  totalVendas: number;
  valorTotalVendas: number;
  ticketMedio: number;
  ultimaVenda?: Date;
  metaMensal: number;
  comissaoPercentual: number;
  percentualMeta: number;
}

// ===================================
// INTERFACES DE AUTENTICAÇÃO
// ===================================

export interface LoginRequest {
  email: string;
  senha: string;
  lembrarMe?: boolean;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  token?: string;
  refreshToken?: string;
  usuario?: Usuario;
  primeiroAcesso?: boolean;
  permissoes?: string[];
}

export interface AlterarSenhaRequest {
  senhaAtual?: string;
  novaSenha: string;
  confirmarSenha: string;
  token?: string; // Para primeiro acesso
}

export interface ResetSenhaRequest {
  email: string;
}

export interface ConviteUsuarioRequest {
  nome: string;
  email: string;
  tipoUsuario: TipoUsuario;
  telefone?: string;
  observacoes?: string;
  metaMensal?: number;
  comissaoPercentual?: number;
}

// ===================================
// INTERFACES DE FILTROS E PAGINAÇÃO
// ===================================

export interface FiltroUsuarios {
  termo?: string;
  tipoUsuario?: TipoUsuario;
  status?: StatusUsuario;
  dataInicio?: Date;
  dataFim?: Date;
  page?: number;
  limit?: number;
  orderBy?: string;
  orderDirection?: 'ASC' | 'DESC';
}

export interface FiltroAuditoria {
  usuarioId?: string;
  tipoAcao?: TipoAcao;
  modulo?: string;
  dataInicio?: Date;
  dataFim?: Date;
  page?: number;
  limit?: number;
}

export interface FiltroVendedores {
  termo?: string;
  ano?: number;
  mes?: number;
  apenasAtivos?: boolean;
  page?: number;
  limit?: number;
}

export interface ResultadoPaginado<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

// ===================================
// INTERFACES DE DASHBOARD
// ===================================

export interface DashboardVendedor {
  vendedor: Usuario;
  estatisticas: {
    vendasHoje: number;
    vendasSemana: number;
    vendasMes: number;
    faturamentoHoje: number;
    faturamentoSemana: number;
    faturamentoMes: number;
    ticketMedio: number;
    metaMensal: number;
    percentualMeta: number;
    comissaoMes: number;
    ranking: number;
    totalVendedores: number;
  };
  vendas: any[]; // últimas vendas
  produtosMaisVendidos: {
    produtoId: string;
    produtoNome: string;
    quantidade: number;
    valor: number;
  }[];
  graficoVendas: {
    labels: string[];
    dados: number[];
  };
}

export interface DashboardGerencial {
  resumoGeral: {
    totalUsuarios: number;
    usuariosAtivos: number;
    vendedoresAtivos: number;
    totalVendas: number;
    faturamentoTotal: number;
    ticketMedio: number;
  };
  rankingVendedores: EstatisticasVendedor[];
  atividadeRecente: AuditoriaLog[];
  alertas: {
    tipo: 'warning' | 'error' | 'info';
    titulo: string;
    mensagem: string;
    data: Date;
  }[];
}

// ===================================
// INTERFACES DE VALIDAÇÃO
// ===================================

export interface ValidacaoSenha {
  tamanhoMinimo: boolean;
  temNumero: boolean;
  temLetraMaiuscula: boolean;
  temLetraMinuscula: boolean;
  temCaractereEspecial: boolean;
  valida: boolean;
}

export interface ConfiguracoesSistema {
  tentativasLoginMax: number;
  tempoBloqueioPadrao: number; // em minutos
  tempoSessao: number; // em horas
  forcarTrocaSenha: boolean;
  validadeSenha: number; // em dias
  tamanhoMinimoSenha: number;
  requireSenhaComplexaAdmin: boolean;
  requireSenhaComplexaVendedor: boolean;
  enviarEmailNovoUsuario: boolean;
  enviarEmailResetSenha: boolean;
}

// ===================================
// TIPOS DE RESPOSTA DA API
// ===================================

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
  errors?: string[];
  meta?: {
    timestamp: Date;
    version: string;
    requestId: string;
  };
}

export interface ApiError {
  success: false;
  message: string;
  errors: string[];
  statusCode: number;
}

// ===================================
// CONSTANTS
// ===================================

export const TIPO_USUARIO_LABELS = {
  [TipoUsuario.OWNER]: 'Proprietário',
  [TipoUsuario.ADMIN]: 'Administrador',
  [TipoUsuario.VENDEDOR]: 'Vendedor'
};

export const STATUS_USUARIO_LABELS = {
  [StatusUsuario.ATIVO]: 'Ativo',
  [StatusUsuario.INATIVO]: 'Inativo',
  [StatusUsuario.BLOQUEADO]: 'Bloqueado',
  [StatusUsuario.PENDENTE]: 'Pendente'
};

export const TIPO_ACAO_LABELS = {
  [TipoAcao.CREATE]: 'Criação',
  [TipoAcao.UPDATE]: 'Atualização',
  [TipoAcao.DELETE]: 'Exclusão',
  [TipoAcao.LOGIN]: 'Login',
  [TipoAcao.LOGOUT]: 'Logout',
  [TipoAcao.PASSWORD_CHANGE]: 'Alteração de Senha',
  [TipoAcao.ACCESS_DENIED]: 'Acesso Negado'
};

export const MODULOS_SISTEMA = [
  'PRODUTOS',
  'VENDAS',
  'RELATORIOS',
  'USUARIOS',
  'CONFIGURACOES',
  'AUDITORIA'
] as const;

export type ModuloSistema = typeof MODULOS_SISTEMA[number];
