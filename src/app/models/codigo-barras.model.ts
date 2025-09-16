export interface TipoCodigoBarras {
  codigo: string;
  nome: string;
  descricao: string;
  padraoBrasileiro: boolean;
  comprimento: number;
}

export interface CodigoBarrasRequest {
  tipoCodigoBarras: string;
  prefixo?: string;
  gerarAutomaticamente?: boolean;
  codigoManual?: string;
}

export interface CodigoBarrasResponse {
  codigo: string;
  tipoCodigoBarras: string;
  descricao: string;
  valido: boolean;
}

export interface GerarCodigoResponse {
  codigo: string;
  tipo: string;
  prefixo: string;
}
