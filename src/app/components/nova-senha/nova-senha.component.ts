import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EmailNotificationService } from '../../services/email-notification.service';

@Component({
  selector: 'app-nova-senha',
  templateUrl: './nova-senha.component.html',
  styleUrls: ['./nova-senha.component.css']
})
export class NovaSenhaComponent implements OnInit {
  novaSenhaForm!: FormGroup;
  validarCodigoForm!: FormGroup;
  validarSenhaAtualForm!: FormGroup; // Novo form para senha atual
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  codigoValidado = false; // Nova propriedade para controlar o fluxo
  senhaAtualValidada = false; // Nova propriedade para controlar fluxo de senha atual
  isUserLoggedIn = false; // Nova propriedade para verificar se usuário está logado
  fromProfile = false; // Nova propriedade para identificar se veio do perfil

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private emailService: EmailNotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    
    // Verificar se veio do perfil através de query param
    this.route.queryParams.subscribe(params => {
      this.fromProfile = params['from'] === 'profile';
    });
    
    // Verificar se o usuário está logado
    this.isUserLoggedIn = this.authService.isAuthenticated();
  }

  private initializeForms(): void {
    // Formulário para validar código
    this.validarCodigoForm = this.fb.group({
      codigo: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Formulário para validar senha atual
    this.validarSenhaAtualForm = this.fb.group({
      senhaAtual: ['', [Validators.required]]
    });

    // Formulário para nova senha
    this.novaSenhaForm = this.fb.group({
      novaSenha: ['', [Validators.required, Validators.minLength(6)]],
      confirmarSenha: ['', [Validators.required]]
    }, { validators: this.senhasIguais });
  }

  senhasIguais(form: FormGroup) {
    const novaSenha = form.get('novaSenha');
    const confirmarSenha = form.get('confirmarSenha');
    
    if (novaSenha && confirmarSenha && novaSenha.value !== confirmarSenha.value) {
      return { senhasDiferentes: true };
    }
    return null;
  }

  // Validar senha atual para usuários logados
  validarSenhaAtual(): void {
    if (this.validarSenhaAtualForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const senhaAtual = this.validarSenhaAtualForm.get('senhaAtual')?.value;
      
      // Simular validação da senha atual (em uma aplicação real, seria uma chamada para API)
      setTimeout(() => {
        // Senha de exemplo válida: "admin123" (seria comparada com a senha do usuário atual)
        if (senhaAtual === 'admin123') {
          this.isLoading = false;
          this.senhaAtualValidada = true;
          this.successMessage = 'Senha atual confirmada! Agora defina sua nova senha.';
          
          // Limpar mensagem após 3 segundos
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        } else {
          this.isLoading = false;
          this.errorMessage = 'Senha atual incorreta. Verifique e tente novamente.';
        }
      }, 1000);
    } else {
      this.markFormGroupTouched(this.validarSenhaAtualForm);
    }
  }

  // Validar código primeiro
  validarCodigo(): void {
    if (this.validarCodigoForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const codigo = this.validarCodigoForm.get('codigo')?.value;
      
      // Simular validação do código (em uma aplicação real, seria uma chamada para API)
      setTimeout(() => {
        // Código de exemplo válido: "123456"
        if (codigo === '123456') {
          this.isLoading = false;
          this.codigoValidado = true;
          this.successMessage = 'Código validado com sucesso! Agora defina sua nova senha.';
          
          // Limpar mensagem após 3 segundos
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        } else {
          this.isLoading = false;
          this.errorMessage = 'Código inválido. Verifique o código enviado por email.';
        }
      }, 1000);
    } else {
      this.markFormGroupTouched(this.validarCodigoForm);
    }
  }

  onSubmit(): void {
    if (this.novaSenhaForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';
      
      const dados = {
        codigo: this.isUserLoggedIn ? undefined : this.validarCodigoForm.get('codigo')?.value,
        senhaAtual: this.isUserLoggedIn ? this.validarSenhaAtualForm.get('senhaAtual')?.value : undefined,
        ...this.novaSenhaForm.value
      };
      
      this.authService.redefinirSenha(dados).subscribe({
        next: (response: { mensagem: string }) => {
          this.isLoading = false;
          this.successMessage = 'Senha redefinida com sucesso! Redirecionando...';
          console.log('Senha redefinida com sucesso:', response);
          
          // Enviar notificação por email sobre a mudança de senha
          this.enviarNotificacaoMudancaSenha();
          
          // Redirecionar baseado no status de login ou origem
          setTimeout(() => {
            if (this.isUserLoggedIn || this.fromProfile) {
              this.router.navigate(['/perfil']);
            } else {
              this.router.navigate(['/login']);
            }
          }, 3000);
        },
        error: (error: any) => {
          this.isLoading = false;
          this.errorMessage = error.message || 'Erro ao redefinir senha. Tente novamente.';
          console.error('Erro ao redefinir senha:', error);
        }
      });
    } else {
      this.markFormGroupTouched(this.novaSenhaForm);
    }
  }

  private enviarNotificacaoMudancaSenha(): void {
    // Obter dados do usuário (em um cenário real, seria obtido do contexto de autenticação)
    const dadosUsuario = {
      email: 'admin@admin.com', // Seria obtido do token ou contexto
      nome: 'Administrador'
    };

    this.emailService.enviarNotificacaoMudancaSenha(
      dadosUsuario.email,
      dadosUsuario.nome,
      new Date()
    ).subscribe({
      next: (response) => {
        console.log('✅ Notificação de mudança de senha enviada:', response);
        this.showToast('Email de confirmação enviado para ' + dadosUsuario.email, 'info');
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

  private markFormGroupTouched(formGroup?: FormGroup): void {
    const form = formGroup || this.novaSenhaForm;
    Object.keys(form.controls).forEach(key => {
      const control = form.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string, formGroup?: FormGroup): string {
    const form = formGroup || this.novaSenhaForm;
    const field = form.get(fieldName);
    
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        const fieldNames: any = {
          codigo: 'Código',
          senhaAtual: 'Senha atual',
          novaSenha: 'Nova senha',
          confirmarSenha: 'Confirmação de senha'
        };
        return `${fieldNames[fieldName]} é obrigatório`;
      }
      if (field.errors['minlength']) {
        if (fieldName === 'codigo') {
          return 'Código deve ter 6 caracteres';
        }
        if (fieldName === 'novaSenha') {
          return 'Senha deve ter pelo menos 6 caracteres';
        }
      }
    }

    // Verificar erro de senhas diferentes (apenas para o form de nova senha)
    if (fieldName === 'confirmarSenha' && this.novaSenhaForm.errors?.['senhasDiferentes']) {
      return 'Senhas não coincidem';
    }
    
    return '';
  }

  // Método para obter erro do código
  getCodigoError(): string {
    return this.getFieldError('codigo', this.validarCodigoForm);
  }

  // Método para obter erro da senha atual
  getSenhaAtualError(): string {
    return this.getFieldError('senhaAtual', this.validarSenhaAtualForm);
  }

  // Voltar para primeira etapa
  voltarParaCodigo(): void {
    this.codigoValidado = false;
    this.senhaAtualValidada = false;
    this.successMessage = '';
    this.errorMessage = '';
    this.novaSenhaForm.reset();
    this.validarSenhaAtualForm.reset();
  }

  voltarLogin(): void {
    if (this.isUserLoggedIn || this.fromProfile) {
      // Se usuário está logado ou veio do perfil, voltar para o perfil
      this.router.navigate(['/perfil']);
    } else {
      // Se não está logado, ir para login
      this.router.navigate(['/login']);
    }
  }

  voltarRedefinir(): void {
    this.router.navigate(['/redefinir-senha']);
  }
}
