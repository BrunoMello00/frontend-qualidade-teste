import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';
import { MockDataService } from './mock-data.service';
import { EnvironmentService } from './environment.service';
import { HttpClient } from '@angular/common/http';

export interface Cliente {
  id?: number;
  nome: string;
  email?: string;
  telefone?: string;
  cpf?: string;
  endereco?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
  dataNascimento?: string;
  dataCadastro?: string;
  pontos?: number;
  ativo?: boolean;
  categoria?: string;
  totalCompras?: number;
  quantidadeCompras?: number;
  ultimaCompra?: string;
}

export interface ClienteResponse {
  content: Cliente[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class ClienteService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  listarClientes(page: number = 0, size: number = 20, search?: string, categoria?: string, ativo?: boolean): Observable<ClienteResponse> {
    if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.listarClientes(page, size, search, categoria, ativo) as Observable<any>;
    return this.get<ClienteResponse>('/clientes', {
      page: page.toString(),
      size: size.toString()
    });
  }

  buscarPorId(id: number): Observable<Cliente> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.buscarClientePorId(Number(id)) as Observable<Cliente>;
    return this.get<Cliente>(`/clientes/${id}`);
  }

  criarCliente(cliente: Cliente): Observable<Cliente> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.criarCliente(cliente) as Observable<Cliente>;
    return this.post<Cliente>('/clientes', cliente);
  }

  atualizarCliente(id: number, cliente: Cliente): Observable<Cliente> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.atualizarCliente(Number(id), cliente) as Observable<Cliente>;
    return this.put<Cliente>(`/clientes/${id}`, cliente);
  }

  deletarCliente(id: number): Observable<void> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.deletarCliente(Number(id)).pipe();
    return this.delete<void>(`/clientes/${id}`);
  }

  buscarPorNome(nome: string): Observable<ClienteResponse> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.listarClientes(0, 50, nome) as Observable<any>;
    return this.get<ClienteResponse>('/clientes/buscar', { nome });
  }

  buscarPorCpf(cpf: string): Observable<Cliente> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.buscarPorCpf(cpf) as Observable<Cliente>;
    return this.get<Cliente>(`/clientes/cpf/${encodeURIComponent(cpf)}`);
  }

  adicionarPontos(clienteId: number, pontos: number, descricao: string): Observable<Cliente> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.adicionarPontosCliente(clienteId, pontos, descricao) as Observable<Cliente>;
    return this.patch<Cliente>(`/clientes/${clienteId}/pontos/adicionar`, {
      pontos,
      descricao
    });
  }

  contarClientes(): Observable<number> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.contarClientes();
    return this.get<number>('/clientes/count');
  }
}
