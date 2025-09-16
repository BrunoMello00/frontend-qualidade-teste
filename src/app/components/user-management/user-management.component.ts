import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, combineLatest } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { 
  Usuario, 
  NovoUsuario,
  FiltroUsuarios,
  TipoUsuario,
  StatusUsuario,
  TIPO_USUARIO_LABELS,
  STATUS_USUARIO_LABELS,
  ConviteUsuarioRequest
} from '../../models/user.models';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Estado do componente
  usuarios: Usuario[] = [];
  usuarioSelecionado: Usuario | null = null;
  loading = false;
  total = 0;
  page = 1;
  limit = 10;
  totalPages = 0;

  // Formulários
  filtroForm!: FormGroup;
  usuarioForm!: FormGroup;
  conviteForm!: FormGroup;

  // Modais
  showUsuarioModal = false;
  showConviteModal = false;
  showDetalhesModal = false;
  showConfirmModal = false;
  showRelatorioModal = false;
  isEditMode = false;

  // Dados do relatório
  dadosRelatorioVendas: any = null;

  // Confirmação
  confirmAction: (() => void) | null = null;
  confirmMessage = '';
  confirmTitle = '';

  // Enums para template
  TipoUsuario = TipoUsuario;
  StatusUsuario = StatusUsuario;
  TIPO_USUARIO_LABELS = TIPO_USUARIO_LABELS;
  STATUS_USUARIO_LABELS = STATUS_USUARIO_LABELS;
  
  // Arrays para loops no template
  tiposUsuario = Object.values(TipoUsuario);
  statusUsuarios = Object.values(StatusUsuario);

  // Permissões
  canManageUsers = false;
  canCreateUsers = false;
  canEditUsers = false;
  canDeleteUsers = false;

  // Mensagens
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService
  ) {
    this.initializeForms();
    this.checkPermissions();
  }

  ngOnInit(): void {
    this.loadUsuarios();
    this.setupFormSubscriptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ===================================
  // INICIALIZAÇÃO
  // ===================================

  private initializeForms(): void {
    this.filtroForm = this.fb.group({
      termo: [''],
      tipoUsuario: [''],
      status: [''],
      dataInicio: [''],
      dataFim: ['']
    });

    this.usuarioForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      tipoUsuario: ['', Validators.required],
      telefone: [''],
      observacoes: [''],
      metaMensal: ['', [Validators.min(0)]],
      comissaoPercentual: ['', [Validators.min(0), Validators.max(100)]]
    });

    this.conviteForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      tipoUsuario: ['', Validators.required],
      telefone: [''],
      observacoes: [''],
      metaMensal: ['', [Validators.min(0)]],
      comissaoPercentual: ['', [Validators.min(0), Validators.max(100)]]
    });
  }

  private setupFormSubscriptions(): void {
    // Auto-busca quando filtros mudam
    this.filtroForm.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.page = 1;
        this.loadUsuarios();
      });

    // Validar email quando muda
    this.usuarioForm.get('email')?.valueChanges
      .pipe(
        debounceTime(1000),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(email => {
        if (email && this.usuarioForm.get('email')?.valid) {
          this.validateEmail(email);
        }
      });
  }

  private checkPermissions(): void {
    this.canManageUsers = this.authService.canManageUsers();
    this.canCreateUsers = this.authService.hasPermission('USUARIOS_CRIAR') || this.authService.isAdmin();
    this.canEditUsers = this.authService.hasPermission('USUARIOS_EDITAR') || this.authService.isAdmin();
    this.canDeleteUsers = this.authService.hasPermission('USUARIOS_EXCLUIR') || this.authService.isOwner() || this.authService.isAdmin();
  }

  // ===================================
  // CARREGAMENTO DE DADOS
  // ===================================

  loadUsuarios(): void {
    const filtros: FiltroUsuarios = {
      ...this.filtroForm.value,
      page: this.page,
      limit: this.limit
    };

    // Limpar valores vazios
    Object.keys(filtros).forEach(key => {
      if (filtros[key as keyof FiltroUsuarios] === '') {
        delete filtros[key as keyof FiltroUsuarios];
      }
    });

    this.userService.listarUsuarios(filtros)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          // Filtrar o usuário atual (admin) da lista para que não apareça
          const currentUser = this.authService.getCurrentUser();
          this.usuarios = result.data.filter(usuario => {
            // Não mostrar o usuário logado atualmente
            if (currentUser && usuario.id === currentUser.id) {
              return false;
            }
            // Não mostrar outros admins se o usuário atual for admin (política de segurança)
            if (currentUser?.tipoUsuario === TipoUsuario.ADMIN && usuario.tipoUsuario === TipoUsuario.ADMIN) {
              return false;
            }
            return true;
          });
          this.total = this.usuarios.length; // Atualizar total após filtro
          this.totalPages = Math.ceil(this.total / this.limit);
        },
        error: (error) => {
          this.showError('Erro ao carregar colaboradores: ' + error);
        }
      });
  }

  // ===================================
  // OPERAÇÕES CRUD
  // ===================================

  openCreateModal(): void {
    if (!this.canCreateUsers) {
      this.showError('Você não tem permissão para criar usuários');
      return;
    }

    this.isEditMode = false;
    this.usuarioSelecionado = null;
    this.usuarioForm.reset();
    this.showUsuarioModal = true;
  }

  openEditModal(usuario: Usuario): void {
    if (!this.canEditUsers) {
      this.showError('Você não tem permissão para editar usuários');
      return;
    }

    // Verificar se é admin - admins não podem ser editados
    if (usuario.tipoUsuario === TipoUsuario.ADMIN) {
      this.showError('Não é possível editar administradores');
      return;
    }

    this.isEditMode = true;
    this.usuarioSelecionado = usuario;
    this.usuarioForm.patchValue({
      nome: usuario.nome,
      email: usuario.email,
      tipoUsuario: usuario.tipoUsuario,
      telefone: usuario.telefone || '',
      observacoes: usuario.observacoes || '',
      metaMensal: usuario.metaMensal,
      comissaoPercentual: usuario.comissaoPercentual
    });
    this.showUsuarioModal = true;
  }

  openDetailsModal(usuario: Usuario): void {
    this.usuarioSelecionado = usuario;
    this.showDetalhesModal = true;
  }

  openConviteModal(): void {
    if (!this.canCreateUsers) {
      this.showError('Você não tem permissão para enviar convites');
      return;
    }

    this.conviteForm.reset();
    this.showConviteModal = true;
  }

  saveUsuario(): void {
    if (this.usuarioForm.invalid) {
      this.markFormGroupTouched(this.usuarioForm);
      return;
    }

    const dadosUsuario = this.usuarioForm.value;

    if (this.isEditMode && this.usuarioSelecionado) {
      // Editar usuário existente
      this.userService.atualizarUsuario(this.usuarioSelecionado.id, dadosUsuario)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess('Usuário atualizado com sucesso');
            this.closeUsuarioModal();
            this.loadUsuarios();
          },
          error: (error) => {
            this.showError('Erro ao atualizar usuário: ' + error);
          }
        });
    } else {
      // Criar novo usuário
      const novoUsuario: NovoUsuario = dadosUsuario;
      this.userService.criarUsuario(novoUsuario)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess('Usuário criado com sucesso');
            this.closeUsuarioModal();
            this.loadUsuarios();
          },
          error: (error) => {
            this.showError('Erro ao criar usuário: ' + error);
          }
        });
    }
  }

  enviarConvite(): void {
    if (this.conviteForm.invalid) {
      this.markFormGroupTouched(this.conviteForm);
      return;
    }

    const convite: ConviteUsuarioRequest = this.conviteForm.value;

    this.userService.enviarConvite(convite)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess('Convite enviado com sucesso');
          this.closeConviteModal();
        },
        error: (error) => {
          this.showError('Erro ao enviar convite: ' + error);
        }
      });
  }

  confirmarRemocao(usuario: Usuario): void {
    if (!this.canDeleteUsers) {
      this.showError('Você não tem permissão para remover usuários');
      return;
    }

    // Verificar se pode excluir este tipo de usuário
    const currentUser = this.authService.getCurrentUser();
    if (currentUser && !this.authService.canDeleteUser(usuario.tipoUsuario)) {
      this.showError('Você não tem permissão para excluir este tipo de usuário');
      return;
    }

    // Verificar se não está tentando excluir o próprio usuário
    if (currentUser && currentUser.id === usuario.id) {
      this.showError('Não é possível excluir o próprio usuário');
      return;
    }

    this.confirmTitle = 'Confirmar Remoção';
    this.confirmMessage = `Tem certeza que deseja remover o usuário "${usuario.nome}"? Esta ação não pode ser desfeita.`;
    this.confirmAction = () => this.removerUsuario(usuario.id);
    this.showConfirmModal = true;
  }

  private removerUsuario(id: string): void {
    this.userService.removerUsuario(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess('Usuário removido com sucesso');
          this.loadUsuarios();
        },
        error: (error) => {
          this.showError('Erro ao remover usuário: ' + error);
        }
      });
  }

  // ===================================
  // OPERAÇÕES DE STATUS
  // ===================================

  ativarUsuario(usuario: Usuario): void {
    if (!usuario.id) {
      this.showError('ID do usuário não encontrado');
      return;
    }

    // Verificar se é admin - admins não podem ter status alterado
    if (usuario.tipoUsuario === TipoUsuario.ADMIN) {
      this.showError('Não é possível alterar o status de administradores');
      return;
    }

    this.userService.ativarUsuario(usuario.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess(`Colaborador ${usuario.nome} ativado com sucesso`);
          this.loadUsuarios();
        },
        error: (error) => {
          this.showError('Erro ao ativar colaborador: ' + (error.message || error));
        }
      });
  }

  desativarUsuario(usuario: Usuario): void {
    if (!usuario.id) {
      this.showError('ID do usuário não encontrado');
      return;
    }

    // Verificar se é admin - admins não podem ter status alterado
    if (usuario.tipoUsuario === TipoUsuario.ADMIN) {
      this.showError('Não é possível alterar o status de administradores');
      return;
    }

    this.confirmTitle = 'Confirmar Desativação';
    this.confirmMessage = `Tem certeza que deseja desativar o colaborador "${usuario.nome}"?`;
    this.confirmAction = () => {
      this.userService.desativarUsuario(usuario.id!)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess(`Colaborador ${usuario.nome} desativado com sucesso`);
            this.loadUsuarios();
          },
          error: (error) => {
            this.showError('Erro ao desativar colaborador: ' + (error.message || error));
          }
        });
    };
    this.showConfirmModal = true;
  }

  bloquearUsuario(usuario: Usuario): void {
    if (!usuario.id) {
      this.showError('ID do usuário não encontrado');
      return;
    }

    // Verificar se é admin - admins não podem ser bloqueados
    if (usuario.tipoUsuario === TipoUsuario.ADMIN) {
      this.showError('Não é possível bloquear administradores');
      return;
    }

    this.confirmTitle = 'Confirmar Bloqueio';
    this.confirmMessage = `Tem certeza que deseja bloquear o colaborador "${usuario.nome}"?`;
    this.confirmAction = () => {
      this.userService.bloquearUsuario(usuario.id!)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            this.showSuccess(`Colaborador ${usuario.nome} bloqueado com sucesso`);
            this.loadUsuarios();
          },
          error: (error) => {
            this.showError('Erro ao bloquear colaborador: ' + (error.message || error));
          }
        });
    };
    this.showConfirmModal = true;
  }

  reenviarConvite(usuario: Usuario): void {
    this.userService.reenviarConvite(usuario.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.showSuccess('Convite reenviado com sucesso');
        },
        error: (error) => {
          this.showError('Erro ao reenviar convite: ' + error);
        }
      });
  }

  // ===================================
  // PAGINAÇÃO
  // ===================================

  onPageChange(newPage: number): void {
    this.page = newPage;
    this.loadUsuarios();
  }

  onLimitChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.limit = +target.value;
    this.page = 1;
    this.loadUsuarios();
  }

  // ===================================
  // UTILITÁRIOS
  // ===================================

  private validateEmail(email: string): void {
    this.userService.validarEmail(email)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          if (!result.disponivel) {
            this.usuarioForm.get('email')?.setErrors({ emailTaken: true });
          }
        },
        error: (error) => {
          console.error('Erro ao validar email:', error);
        }
      });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      if (control && 'controls' in control) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  isFieldInvalid(form: FormGroup, fieldName: string): boolean {
    const field = form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched)) {
      if (field.errors['required']) return `${fieldName} é obrigatório`;
      if (field.errors['email']) return 'Email inválido';
      if (field.errors['emailTaken']) return 'Email já está em uso';
      if (field.errors['minlength']) return `${fieldName} muito curto`;
      if (field.errors['min']) return `Valor mínimo é ${field.errors['min'].min}`;
      if (field.errors['max']) return `Valor máximo é ${field.errors['max'].max}`;
    }
    return '';
  }

  // ===================================
  // MÉTODOS DE MODAL
  // ===================================

  closeUsuarioModal(): void {
    this.showUsuarioModal = false;
    this.usuarioSelecionado = null;
    this.usuarioForm.reset();
  }

  closeConviteModal(): void {
    this.showConviteModal = false;
    this.conviteForm.reset();
  }

  closeDetalhesModal(): void {
    this.showDetalhesModal = false;
    this.usuarioSelecionado = null;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.confirmAction = null;
    this.confirmMessage = '';
    this.confirmTitle = '';
  }

  executeConfirmAction(): void {
    if (this.confirmAction) {
      this.confirmAction();
    }
    this.closeConfirmModal();
  }

  // ===================================
  // MENSAGENS
  // ===================================

  private showSuccess(message: string): void {
    this.successMessage = message;
    this.errorMessage = '';
    setTimeout(() => {
      this.successMessage = '';
    }, 5000);
  }

  private showError(message: string): void {
    this.errorMessage = message;
    this.successMessage = '';
    setTimeout(() => {
      this.errorMessage = '';
    }, 5000);
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  // ===================================
  // MÉTODOS PARA TEMPLATE
  // ===================================

  getStatusBadgeClass(status: StatusUsuario): string {
    switch (status) {
      case StatusUsuario.ATIVO:
        return 'badge bg-light text-dark';
      case StatusUsuario.INATIVO:
        return 'badge bg-light text-dark';
      case StatusUsuario.BLOQUEADO:
        return 'badge bg-light text-dark';
      case StatusUsuario.PENDENTE:
        return 'badge bg-light text-dark';
      default:
        return 'badge bg-light text-dark';
    }
  }

  getTipoUsuarioBadgeClass(tipo: TipoUsuario): string {
    switch (tipo) {
      case TipoUsuario.OWNER:
        return 'badge bg-primary text-white';
      case TipoUsuario.ADMIN:
        return 'badge bg-danger text-white';
      case TipoUsuario.VENDEDOR:
        return 'badge bg-info text-white';
      default:
        return 'badge bg-light text-dark';
    }
  }

  formatDate(date: Date | undefined): string {
    if (!date) return 'Nunca';
    return new Date(date).toLocaleString('pt-BR');
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value);
  }

  getTipoUsuarioLabel(tipo: TipoUsuario): string {
    return TIPO_USUARIO_LABELS[tipo] || '';
  }

  getStatusUsuarioLabel(status: StatusUsuario): string {
    return STATUS_USUARIO_LABELS[status] || '';
  }

  getPaginationArray(): number[] {
    const pages = [];
    const maxPages = Math.min(this.totalPages, 5);
    const startPage = Math.max(1, this.page - Math.floor(maxPages / 2));
    
    for (let i = startPage; i < startPage + maxPages && i <= this.totalPages; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  // ===================================
  // RELATÓRIO DE VENDAS
  // ===================================

  abrirRelatorioVendas(usuario: Usuario): void {
    if (usuario.tipoUsuario !== TipoUsuario.VENDEDOR) {
      this.showError('Relatório de vendas disponível apenas para vendedores');
      return;
    }

    console.log('Abrindo relatório de vendas para:', usuario.nome);
    
    // Simular dados de vendas
    this.dadosRelatorioVendas = {
      vendedor: usuario,
      periodo: 'Últimos 30 dias',
      totalVendas: Math.floor(Math.random() * 50) + 10,
      valorTotal: Math.floor(Math.random() * 50000) + 10000,
      ticketMedio: Math.floor(Math.random() * 500) + 100,
      metaAtingida: Math.floor(Math.random() * 150) + 50,
      comissaoMes: Math.floor(Math.random() * 2000) + 500,
      vendas: [
        { data: '2025-09-05', cliente: 'Cliente A', valor: 1500, produtos: 3 },
        { data: '2025-09-07', cliente: 'Cliente B', valor: 2200, produtos: 5 },
        { data: '2025-09-08', cliente: 'Cliente C', valor: 800, produtos: 2 },
        { data: '2025-09-09', cliente: 'Cliente D', valor: 3200, produtos: 8 }
      ]
    };

    // Mostrar modal usando Bootstrap
    this.showRelatorioModal = true;
  }

  closeRelatorioModal(): void {
    this.showRelatorioModal = false;
    this.dadosRelatorioVendas = null;
  }

  // ===================================
  // MÉTODOS DE UTILIDADE
  // ===================================
}
