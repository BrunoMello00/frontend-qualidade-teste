import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { 
  UsuarioService, 
  Usuario, 
  TipoUsuario, 
  FiltrosUsuario, 
  EstatisticasUsuario,
  NovoUsuario 
} from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-management',
  template: `
    <div class="container-fluid py-4">
      <div class="row">
        <div class="col-12">
          <!-- Card de Estatísticas -->
          <div class="row mb-4" *ngIf="estatisticas">
            <div class="col-md-3 mb-3">
              <div class="card border-0 shadow-sm">
                <div class="card-body text-center">
                  <i class="bi bi-people display-6 text-primary mb-2"></i>
                  <h3 class="fw-bold">{{ estatisticas.totalUsuarios || 0 }}</h3>
                  <p class="text-muted mb-0">Total de Usuários</p>
                </div>
              </div>
            </div>
            <div class="col-md-3 mb-3">
              <div class="card border-0 shadow-sm">
                <div class="card-body text-center">
                  <i class="bi bi-check-circle display-6 text-success mb-2"></i>
                  <h3 class="fw-bold text-success">{{ estatisticas.usuariosAtivos || 0 }}</h3>
                  <p class="text-muted mb-0">Usuários Ativos</p>
                </div>
              </div>
            </div>
            <div class="col-md-3 mb-3">
              <div class="card border-0 shadow-sm">
                <div class="card-body text-center">
                  <i class="bi bi-shield-check display-6 text-info mb-2"></i>
                  <h3 class="fw-bold text-info">{{ estatisticas.totalAdmins || 0 }}</h3>
                  <p class="text-muted mb-0">Administradores</p>
                </div>
              </div>
            </div>
            <div class="col-md-3 mb-3">
              <div class="card border-0 shadow-sm">
                <div class="card-body text-center">
                  <i class="bi bi-person-badge display-6 text-secondary mb-2"></i>
                  <h3 class="fw-bold text-secondary">{{ estatisticas.totalVendedores || 0 }}</h3>
                  <p class="text-muted mb-0">Vendedores</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Card Principal -->
          <div class="card border-0 shadow-sm">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
              <div>
                <h3 class="mb-0"><i class="bi bi-person-gear me-2"></i>Gestão de Usuários</h3>
                <p class="text-muted mb-0">Gerencie usuários do sistema</p>
              </div>
              <div class="d-flex gap-2">
                <button class="btn btn-outline-secondary" (click)="recarregarUsuarios()" [disabled]="carregando">
                  <i class="bi bi-arrow-clockwise me-1"></i>
                  Atualizar
                </button>
                <button *ngIf="authService.canManageUsers()" class="btn btn-primary" (click)="abrirModalCriar()">
                  <i class="bi bi-plus-lg me-1"></i>
                  Novo Usuário
                </button>
              </div>
            </div>
            
            <div class="card-body">
              <!-- Filtros -->
              <div class="row mb-4">
                <div class="col-md-4">
                  <label class="form-label">Buscar usuários</label>
                  <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-search"></i></span>
                    <input 
                      type="text" 
                      class="form-control" 
                      placeholder="Nome ou email..."
                      [(ngModel)]="filtros.termo"
                      (keyup.enter)="aplicarFiltros()"
                      (input)="onBuscaChange($event)">
                  </div>
                </div>
                <div class="col-md-3">
                  <label class="form-label">Tipo de usuário</label>
                  <select class="form-select" [(ngModel)]="filtros.tipoUsuario" (change)="aplicarFiltros()">
                    <option value="">Todos os tipos</option>
                    <option value="OWNER">Proprietário</option>
                    <option value="ADMIN">Administrador</option>
                    <option value="VENDEDOR">Vendedor</option>
                  </select>
                </div>
                <div class="col-md-3">
                  <label class="form-label">Status</label>
                  <select class="form-select" [(ngModel)]="filtros.ativo" (change)="aplicarFiltros()">
                    <option value="">Todos os status</option>
                    <option [value]="true">Apenas ativos</option>
                    <option [value]="false">Apenas inativos</option>
                  </select>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                  <button class="btn btn-outline-primary w-100" (click)="limparFiltros()">
                    <i class="bi bi-x-circle me-1"></i>
                    Limpar
                  </button>
                </div>
              </div>

              <!-- Loading -->
              <div *ngIf="carregando" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Carregando...</span>
                </div>
                <p class="mt-2 text-muted">Carregando usuários...</p>
              </div>

              <!-- Mensagem de erro -->
              <div *ngIf="erro" class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                {{ erro }}
                <button type="button" class="btn-close" (click)="limparErro()"></button>
              </div>

              <!-- Tabela de usuários -->
              <div *ngIf="!carregando" class="table-responsive">
                <table class="table table-hover align-middle">
                  <thead class="bg-light">
                    <tr>
                      <th>
                        <button class="btn btn-link p-0 text-decoration-none text-dark fw-bold"
                                (click)="ordenarPor('nome')">
                          Nome
                          <i class="bi" [class]="getIconeOrdenacao('nome')"></i>
                        </button>
                      </th>
                      <th>Email</th>
                      <th>Tipo</th>
                      <th>Status</th>
                      <th>
                        <button class="btn btn-link p-0 text-decoration-none text-dark fw-bold"
                                (click)="ordenarPor('ultimoLogin')">
                          Último Acesso
                          <i class="bi" [class]="getIconeOrdenacao('ultimoLogin')"></i>
                        </button>
                      </th>
                      <th class="text-center">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let usuario of usuarios; trackBy: trackByUsuario">
                      <td>
                        <div class="d-flex align-items-center">
                          <div class="avatar me-3">
                            <div class="avatar-initials">
                              {{ getIniciais(usuario.nome) }}
                            </div>
                          </div>
                          <div>
                            <strong>{{ usuario.nome }}</strong>
                            <div class="text-muted small" *ngIf="usuario.telefone">
                              {{ usuario.telefone }}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span>{{ usuario.email }}</span>
                      </td>
                      <td>
                        <span class="badge fs-6" [class]="usuarioService.getClasseTipoUsuario(usuario.tipoUsuario)">
                          {{ usuarioService.formatarTipoUsuario(usuario.tipoUsuario) }}
                        </span>
                      </td>
                      <td>
                        <span class="badge fs-6" [class]="usuarioService.getClasseStatus(usuario.ativo, usuario.bloqueado)">
                          {{ usuarioService.formatarStatus(usuario.ativo, usuario.bloqueado) }}
                        </span>
                      </td>
                      <td>
                        <span *ngIf="usuario.ultimoLogin; else nunca">
                          {{ formatarData(usuario.ultimoLogin) }}
                        </span>
                        <ng-template #nunca>
                          <span class="text-muted">Nunca</span>
                        </ng-template>
                      </td>
                      <td class="text-center">
                        <div class="btn-group" role="group">
                          <button type="button" 
                                  class="btn btn-sm btn-outline-info"
                                  (click)="verDetalhes(usuario)"
                                  title="Ver detalhes">
                            <i class="bi bi-eye"></i>
                          </button>
                          <button type="button" 
                                  class="btn btn-sm btn-outline-primary"
                                  (click)="editarUsuario(usuario)"
                                  title="Editar">
                            <i class="bi bi-pencil"></i>
                          </button>
                          <button *ngIf="!usuario.ativo && !usuario.bloqueado" 
                                  type="button" 
                                  class="btn btn-sm btn-outline-success"
                                  (click)="ativarUsuario(usuario)"
                                  title="Ativar usuário">
                            <i class="bi bi-check-circle"></i>
                          </button>
                          <button *ngIf="usuario.ativo" 
                                  type="button" 
                                  class="btn btn-sm btn-outline-warning"
                                  (click)="desativarUsuario(usuario)"
                                  title="Desativar usuário">
                            <i class="bi bi-pause-circle"></i>
                          </button>
                          <button *ngIf="!usuario.bloqueado" 
                                  type="button" 
                                  class="btn btn-sm btn-outline-danger"
                                  (click)="bloquearUsuario(usuario)"
                                  title="Bloquear usuário">
                            <i class="bi bi-shield-x"></i>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
                
                <!-- Mensagem quando não há usuários -->
                <div *ngIf="usuarios.length === 0" class="text-center py-5">
                  <i class="bi bi-people display-1 text-muted"></i>
                  <h5 class="text-muted mt-3">Nenhum usuário encontrado</h5>
                  <p class="text-muted">
                    {{ filtros.termo || filtros.tipoUsuario || filtros.ativo !== undefined ? 
                       'Tente ajustar os filtros de busca' : 
                       'Adicione um novo usuário para começar' }}
                  </p>
                </div>
              </div>

              <!-- Paginação -->
              <div *ngIf="paginacao && paginacao.totalPages > 1" class="d-flex justify-content-between align-items-center mt-4">
                <div class="text-muted">
                  Mostrando {{ (paginacao.number * paginacao.size) + 1 }} a 
                  {{ Math.min((paginacao.number + 1) * paginacao.size, paginacao.totalElements) }} 
                  de {{ paginacao.totalElements }} usuários
                </div>
                <nav>
                  <ul class="pagination mb-0">
                    <li class="page-item" [class.disabled]="paginacao.number === 0">
                      <button class="page-link" (click)="irParaPagina(paginacao.number - 1)">
                        <i class="bi bi-chevron-left"></i>
                      </button>
                    </li>
                    <li class="page-item" 
                        *ngFor="let page of getPaginas()" 
                        [class.active]="page === paginacao.number">
                      <button class="page-link" (click)="irParaPagina(page)">{{ page + 1 }}</button>
                    </li>
                    <li class="page-item" [class.disabled]="paginacao.number >= paginacao.totalPages - 1">
                      <button class="page-link" (click)="irParaPagina(paginacao.number + 1)">
                        <i class="bi bi-chevron-right"></i>
                      </button>
                    </li>
                  </ul>
                </nav>
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .table th {
      border-top: none;
      font-weight: 600;
    }
    .badge {
      font-size: 0.75rem;
    }
    .avatar {
      width: 40px;
      height: 40px;
    }
    .avatar-initials {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
      font-size: 0.9rem;
    }
    .btn-link {
      border: none !important;
    }
    .card {
      transition: all 0.3s ease;
    }
    .table tbody tr:hover {
      background-color: rgba(0,123,255,0.05);
    }
  `]
})
export class UserManagementComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  usuarios: Usuario[] = [];
  estatisticas: EstatisticasUsuario | null = null;
  
  carregando = false;
  erro: string | null = null;
  
  filtros: FiltrosUsuario = {
    page: 0,
    size: 20,
    orderBy: 'nome',
    orderDirection: 'ASC'
  };
  
  paginacao: any = null;
  
  private timeoutBusca: any;

  constructor(
    public usuarioService: UsuarioService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.setupSubscriptions();
    this.carregarDados();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.timeoutBusca) {
      clearTimeout(this.timeoutBusca);
    }
  }


  private setupSubscriptions(): void {
    this.usuarioService.loading$
      .pipe(takeUntil(this.destroy$))
      .subscribe(loading => this.carregando = loading);

    this.usuarioService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe(erro => this.erro = erro);

    this.usuarioService.estatisticas$
      .pipe(takeUntil(this.destroy$))
      .subscribe(stats => this.estatisticas = stats);
  }


  private carregarDados(): void {
    this.carregarUsuarios();
    this.carregarEstatisticas();
  }

  carregarUsuarios(): void {
    
    this.usuarioService.listarUsuarios(this.filtros)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.usuarios = response.content;
          this.paginacao = {
            number: response.number,
            size: response.size,
            totalElements: response.totalElements,
            totalPages: response.totalPages
          };
        },
        error: (error) => {
          console.error('❌ Erro ao carregar usuários:', error);
        }
      });
  }

  carregarEstatisticas(): void {
    this.usuarioService.obterEstatisticas()
      .pipe(takeUntil(this.destroy$))
      .subscribe();
  }

  recarregarUsuarios(): void {
    this.usuarioService.recarregarUsuarios(this.filtros);
    this.usuarioService.recarregarEstatisticas();
    this.carregarUsuarios();
  }


  aplicarFiltros(): void {
    console.log('🔍 Aplicando filtros:', this.filtros);
    this.filtros.page = 0; // Reset para primeira página
    this.carregarUsuarios();
  }

  limparFiltros(): void {
    console.log('🧹 Limpando filtros');
    this.filtros = {
      page: 0,
      size: 20,
      orderBy: 'nome',
      orderDirection: 'ASC'
    };
    this.carregarUsuarios();
  }

  onBuscaChange(event: any): void {
    const termo = event.target.value;
    
    if (this.timeoutBusca) {
      clearTimeout(this.timeoutBusca);
    }
    
    this.timeoutBusca = setTimeout(() => {
      this.filtros.termo = termo;
      this.aplicarFiltros();
    }, 500); // Debounce de 500ms
  }


  ordenarPor(campo: string): void {
    if (this.filtros.orderBy === campo) {
      this.filtros.orderDirection = this.filtros.orderDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.filtros.orderBy = campo;
      this.filtros.orderDirection = 'ASC';
    }
    
    this.aplicarFiltros();
  }

  getIconeOrdenacao(campo: string): string {
    if (this.filtros.orderBy !== campo) {
      return 'bi-chevron-expand';
    }
    return this.filtros.orderDirection === 'ASC' ? 'bi-chevron-up' : 'bi-chevron-down';
  }


  irParaPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.paginacao.totalPages) {
      this.filtros.page = pagina;
      this.carregarUsuarios();
    }
  }

  getPaginas(): number[] {
    if (!this.paginacao) return [];
    
    const total = this.paginacao.totalPages;
    const atual = this.paginacao.number;
    const paginas: number[] = [];
    
    const inicio = Math.max(0, atual - 2);
    const fim = Math.min(total, atual + 3);
    
    for (let i = inicio; i < fim; i++) {
      paginas.push(i);
    }
    
    return paginas;
  }


  abrirModalCriar(): void {
    console.log('📝 Abrindo modal para criar usuário');
    alert('Modal de criação será implementado em breve!');
  }

  verDetalhes(usuario: Usuario): void {
    alert(`Detalhes de ${usuario.nome} serão implementados em breve!`);
  }

  editarUsuario(usuario: Usuario): void {
    alert(`Edição de ${usuario.nome} será implementada em breve!`);
  }

  ativarUsuario(usuario: Usuario): void {
    if (confirm(`Deseja ativar o usuário ${usuario.nome}?`)) {
      this.usuarioService.ativarUsuario(usuario.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.carregarUsuarios();
          },
          error: (error) => {
            console.error('❌ Erro ao ativar usuário:', error);
          }
        });
    }
  }

  desativarUsuario(usuario: Usuario): void {
    if (confirm(`Deseja desativar o usuário ${usuario.nome}?`)) {
      this.usuarioService.desativarUsuario(usuario.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.carregarUsuarios();
          },
          error: (error) => {
            console.error('❌ Erro ao desativar usuário:', error);
          }
        });
    }
  }

  bloquearUsuario(usuario: Usuario): void {
    if (confirm(`Deseja bloquear o usuário ${usuario.nome}? Esta ação pode ser revertida.`)) {
      this.usuarioService.bloquearUsuario(usuario.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            console.log('🚫 Usuário bloqueado com sucesso');
            this.carregarUsuarios();
          },
          error: (error) => {
            console.error('❌ Erro ao bloquear usuário:', error);
          }
        });
    }
  }


  trackByUsuario(index: number, usuario: Usuario): number {
    return usuario.id;
  }

  getIniciais(nome: string): string {
    if (!nome) return '??';
    
    const palavras = nome.trim().split(' ');
    if (palavras.length === 1) {
      return palavras[0].substring(0, 2).toUpperCase();
    }
    
    return (palavras[0][0] + palavras[palavras.length - 1][0]).toUpperCase();
  }

  formatarData(data: Date | string): string {
    if (!data) return 'Nunca';
    
    const dataObj = typeof data === 'string' ? new Date(data) : data;
    return dataObj.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  limparErro(): void {
    this.erro = null;
  }

  Math = Math;
}