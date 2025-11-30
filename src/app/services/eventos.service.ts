import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Evento {
  id?: number;
  nome?: string;
  descricao?: string;
  data?: Date;
  dataInicio?: Date;
  dataFim?: Date;
  usuario?: string;
  ativo?: boolean;
  detalhes?: any;
  descontoPercentual?: number;
  descontoValor?: number;
  local?: string;
  enderecoCompleto?: string;
  status?: string;
  statusDescricao?: string;
  statusCor?: string;
  metaVendas?: number;
  metaQuantidadeVendas?: number;
  observacoes?: string;
  publico?: boolean;
  dataCadastro?: string;
  dataAtualizacao?: string;
  criadoPor?: string;
  atualizadoPor?: string;
  duracaoEmDias?: number;
  eventoVigente?: boolean;
  temDesconto?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EventosService {
  
  private readonly baseUrl = `${environment.apiUrl}/eventos`;

  constructor(private http: HttpClient) {}

  listarEventos(): Observable<Evento[]> {
    return this.http.get<Evento[]>(this.baseUrl);
  }

  listarEventosAtivos(): Observable<Evento[]> {
    return this.http.get<Evento[]>(`${this.baseUrl}/ativos`);
  }

  criarEvento(evento: Evento): Observable<Evento> {
    return this.http.post<Evento>(this.baseUrl, evento);
  }

  atualizarEvento(id: number, evento: Evento): Observable<Evento> {
    return this.http.put<Evento>(`${this.baseUrl}/${id}`, evento);
  }

  deletarEvento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  reativarEvento(id: number): Observable<Evento> {
    return this.http.put<Evento>(`${this.baseUrl}/${id}/reativar`, {});
  }

  alterarStatusEvento(id: number, ativo: boolean): Observable<Evento> {
    return this.http.put<Evento>(`${this.baseUrl}/${id}/status`, { ativo });
  }

  buscarPorPeriodo(dataInicio: Date, dataFim: Date): Observable<Evento[]> {
    const params = {
      dataInicio: dataInicio.toISOString(),
      dataFim: dataFim.toISOString()
    };
    return this.http.get<Evento[]>(`${this.baseUrl}/periodo`, { params });
  }
}
