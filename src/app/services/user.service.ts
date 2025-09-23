import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { BaseService } from './base.service';
import { Usuario } from '../models/user.models';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';
import { MockDataService } from './mock-data.service';

export interface UsuarioResponse {
  data: Usuario[];
  totalPages: number;
  totalElements: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  listarUsuarios(filtros?: any): Observable<UsuarioResponse> {
    if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.listarUsuarios(0, 10) as unknown as Observable<UsuarioResponse>;
    if (filtros) {
      return this.get<UsuarioResponse>('usuarios', filtros);
    }
    return this.get<UsuarioResponse>('usuarios', { page: '0', size: '10' });
  }

  buscarPorId(id: string): Observable<Usuario> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.buscarUsuarioPorId(id) as unknown as Observable<Usuario>;
    return this.get<Usuario>(`usuarios/${id}`);
  }

  criarUsuario(usuario: any): Observable<Usuario> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.criarUsuario(usuario) as unknown as Observable<Usuario>;
    return this.post<Usuario>('usuarios', usuario);
  }

  atualizarUsuario(id: string, usuario: Partial<Usuario>): Observable<Usuario> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.atualizarUsuario(id, usuario) as unknown as Observable<Usuario>;
    return this.put<Usuario>(`usuarios/${id}`, usuario);
  }

  deletarUsuario(id: string): Observable<void> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.excluirUsuario(id) as unknown as Observable<void>;
    return this.delete<void>(`usuarios/${id}`);
  }

  ativarUsuario(id: string): Observable<void> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.reativarUsuario(id) as unknown as Observable<void>;
    return this.patch<void>(`usuarios/${id}/ativar`);
  }

  desativarUsuario(id: string): Observable<void> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.desativarUsuario(id) as unknown as Observable<void>;
    return this.patch<void>(`usuarios/${id}/desativar`);
  }

  resetarSenha(id: string): Observable<void> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.resetarSenhaUsuario(id) as unknown as Observable<void>;
    return this.post<void>(`usuarios/${id}/resetar-senha`, {});
  }

  // Métodos adicionais para compatibilidade
  enviarConvite(convite: any): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.enviarConvite ? this.mock.enviarConvite(convite) : of({ success: true, message: 'Convite enviado (mock)' });
    return this.post<any>('usuarios/convite', convite);
  }

  removerUsuario(id: string): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.excluirUsuario(id);
    return this.delete<any>(`usuarios/${id}`);
  }

  bloquearUsuario(id: string): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.bloquearUsuario(id);
    return this.patch<any>(`usuarios/${id}/bloquear`, {});
  }

  reenviarConvite(id: string): Observable<any> {
  if (this.environmentService?.isLocal && this.environmentService.isLocal()) return this.mock.reenviarConvite(id);
    return this.post<any>(`usuarios/${id}/reenviar-convite`, {});
  }

  validarEmail(email: string): Observable<any> {
    if (this.environmentService?.isLocal && this.environmentService.isLocal()) {
      return this.mock.validarEmail(email);
    }
    return this.post<any>('usuarios/validar-email', { email });
  }
}
