import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EmailNotificationService } from '../../services/email-notification.service';
import { TIPO_USUARIO_LABELS, TipoUsuario } from '../../models/user.models';

@Component({
  selector: 'app-perfil',
  templateUrl: './perfil.component.html',
  styleUrls: ['./perfil.component.css']
})
export class PerfilComponent implements OnInit {
  perfilForm!: FormGroup;
  currentUser: any = null;
  isEditing = false;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private emailService: EmailNotificationService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadUserProfile();
  }

  private initializeForm(): void {
    this.perfilForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.required]],
      cargo: [''],
      departamento: [''],
      dataUltimoLogin: [{ value: '', disabled: true }],
      dataCriacao: [{ value: '', disabled: true }]
    });
  }

  private loadUserProfile(): void {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.currentUser = user;
        
        // Determinar cargo e departamento baseado no tipo de usuário
        const cargo = TIPO_USUARIO_LABELS[user.tipoUsuario] || 'Usuário';
        const departamento = this.getDepartamentoPorTipo(user.tipoUsuario);
        
        this.perfilForm.patchValue({
          nome: user.nome || 'Usuário',
          email: user.email || '',
          telefone: '(11) 99999-9999',
          cargo: cargo,
          departamento: departamento,
          dataUltimoLogin: user.ultimoLogin ? new Date(user.ultimoLogin).toLocaleDateString('pt-BR') : 'Nunca',
          dataCriacao: user.dataCriacao ? new Date(user.dataCriacao).toLocaleDateString('pt-BR') : 'N/A'
        });
      }
    });
  }

  private getDepartamentoPorTipo(tipoUsuario: TipoUsuario): string {
    const descricoesPorTipo = {
      [TipoUsuario.OWNER]: 'Controle Total do Sistema',
      [TipoUsuario.ADMIN]: 'Administração e Configuração',
      [TipoUsuario.VENDEDOR]: 'Vendas e Atendimento'
    };
    
    return descricoesPorTipo[tipoUsuario] || 'Geral';
  }

  enableEditing(): void {
    this.isEditing = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEditing(): void {
    this.isEditing = false;
    this.loadUserProfile();
    this.successMessage = '';
    this.errorMessage = '';
  }

  onSubmit(): void {
    if (this.perfilForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      // Detectar quais campos foram alterados
      const dadosOriginais = {
        nome: this.currentUser?.nome || 'Usuário',
        email: this.currentUser?.email || '',
        telefone: '(11) 99999-9999'
      };
      
      const dadosNovos = {
        nome: this.perfilForm.get('nome')?.value,
        email: this.perfilForm.get('email')?.value,
        telefone: this.perfilForm.get('telefone')?.value
      };
      
      const camposAlterados = this.detectarCamposAlterados(dadosOriginais, dadosNovos);

      // Simular salvamento do perfil
      setTimeout(() => {
        this.isLoading = false;
        this.isEditing = false;
        this.successMessage = 'Perfil atualizado com sucesso!';
        
        // Atualizar dados do usuário no serviço
        const updatedUser = {
          ...this.currentUser,
          nome: dadosNovos.nome,
          email: dadosNovos.email
        };
        
        console.log('Perfil atualizado:', updatedUser);
        
        // Enviar notificação por email se houve alterações
        if (camposAlterados.length > 0) {
          this.enviarNotificacaoAlteracao(dadosNovos.email, dadosNovos.nome, camposAlterados);
        }
        
        // Limpar mensagem após 3 segundos
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      }, 1000);
    } else {
      this.errorMessage = 'Por favor, corrija os campos inválidos.';
      this.markFormGroupTouched();
    }
  }

  private detectarCamposAlterados(dadosOriginais: any, dadosNovos: any): string[] {
    const alterados: string[] = [];
    
    if (dadosOriginais.nome !== dadosNovos.nome) {
      alterados.push(`Nome: "${dadosOriginais.nome}" → "${dadosNovos.nome}"`);
    }
    
    if (dadosOriginais.email !== dadosNovos.email) {
      alterados.push(`Email: "${dadosOriginais.email}" → "${dadosNovos.email}"`);
    }
    
    if (dadosOriginais.telefone !== dadosNovos.telefone) {
      alterados.push(`Telefone: "${dadosOriginais.telefone}" → "${dadosNovos.telefone}"`);
    }
    
    return alterados;
  }

  private enviarNotificacaoAlteracao(email: string, nome: string, camposAlterados: string[]): void {
    this.emailService.enviarNotificacaoAlteracaoPerfil(email, nome, camposAlterados).subscribe({
      next: (response) => {
        console.log('✅ Notificação de alteração de perfil enviada:', response);
        this.showToast('Email de notificação enviado para ' + email, 'info');
      },
      error: (error) => {
        console.error('❌ Erro ao enviar notificação:', error);
        this.showToast('Erro ao enviar notificação por email', 'warning');
      }
    });
  }

  private showToast(message: string, type: 'success' | 'info' | 'warning' | 'error' = 'success'): void {
    const toast = document.createElement('div');
    const bgClass = {
      'success': 'bg-success',
      'info': 'bg-info',
      'warning': 'bg-warning',
      'error': 'bg-danger'
    };
    
    toast.className = `toast align-items-center text-white ${bgClass[type]} border-0 position-fixed`;
    toast.style.top = '20px';
    toast.style.right = '20px';
    toast.style.zIndex = '9999';
    toast.setAttribute('role', 'alert');
    
    toast.innerHTML = `
      <div class="d-flex">
        <div class="toast-body">
          <i class="bi bi-${type === 'success' ? 'check-circle' : 
                          type === 'info' ? 'info-circle' : 
                          type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle'} me-2"></i>
          ${message}
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="this.parentElement.parentElement.remove()"></button>
      </div>
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 5000);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.perfilForm.controls).forEach(key => {
      const control = this.perfilForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.perfilForm.get(fieldName);
    
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        const fieldNames: any = {
          nome: 'Nome',
          email: 'Email',
          telefone: 'Telefone'
        };
        return `${fieldNames[fieldName]} é obrigatório`;
      }
      if (field.errors['email']) {
        return 'Email deve ter um formato válido';
      }
      if (field.errors['minlength']) {
        return 'Nome deve ter pelo menos 2 caracteres';
      }
    }
    
    return '';
  }

  // Máscara para telefone
  onTelefoneInput(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 11) {
      if (value.length <= 10) {
        value = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
      } else {
        value = value.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3');
      }
    }
    
    event.target.value = value;
    this.perfilForm.get('telefone')?.setValue(value);
  }

  // Navegar para configurações
  goToConfiguracoes(): void {
    this.router.navigate(['/configuracoes']);
  }

  // Navegar para alterar senha
  irParaAlterarSenha(): void {
    this.router.navigate(['/nova-senha'], { queryParams: { from: 'profile' } });
  }

  // Sair da conta
  logout(): void {
    if (confirm('Tem certeza que deseja sair da sua conta?')) {
      this.authService.logout();
    }
  }
}
