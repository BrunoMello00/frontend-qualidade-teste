import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { MockDataService } from './mock-data.service';
import { EnvironmentService } from './environment.service';
import { HttpClient } from '@angular/common/http';

export interface Evento {
  id?: number;
  nome?: string;
  tipo?: string;
  descricao?: string;
  data?: Date;
  dataInicio?: Date;
  dataFim?: Date;
  usuario?: string;
  ativo?: boolean;
  detalhes?: any;
  // Propriedades de desconto baseadas no backend EventoDTO
  descontoPercentual?: number;
  descontoValor?: number;
  // Propriedades adicionais do backend
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
export class EventosService extends BaseService {

  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  listarEventos(page: number = 0, size: number = 50, search?: string, tipo?: string, status?: string): Observable<any> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.listarEventos(page, size, search, tipo, status) as Observable<any>;
    return this.get<Evento[]>('eventos', { page: page.toString(), size: size.toString(), search: search || '', tipo: tipo || '', status: status || '' });
  }

  criarEvento(evento: Evento): Observable<Evento> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.criarEvento(evento) as Observable<Evento>;
    return this.post<Evento>('eventos', evento);
  }

  atualizarEvento(id: number, evento: Evento): Observable<Evento> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.atualizarEvento(Number(id), evento) as Observable<Evento>;
    return this.put<Evento>(`eventos/${id}`, evento);
  }

  excluirEvento(id: number): Observable<void> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.excluirEvento(Number(id)).pipe() as Observable<any>;
    return this.delete<void>(`eventos/${id}`);
  }

  alterarStatusEvento(id: number, status: string): Observable<Evento> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.alterarStatusEvento(Number(id), status) as Observable<Evento>;
    return this.put<Evento>(`eventos/${id}/status`, { status });
  }

  buscarPorTipo(tipo: string): Observable<Evento[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.buscarPorTipo(tipo) as Observable<Evento[]>;
    return this.get<Evento[]>(`eventos/tipo/${tipo}`);
  }

  buscarPorPeriodo(dataInicio: Date, dataFim: Date): Observable<Evento[]> {
    if (this.environmentService.isLocal && this.environmentService.isLocal()) return this.mock.buscarPorPeriodo(dataInicio.toISOString(), dataFim.toISOString()) as Observable<Evento[]>;
    return this.get<Evento[]>('eventos/periodo', {
      dataInicio: dataInicio.toISOString(),
      dataFim: dataFim.toISOString()
    });
  }
}
