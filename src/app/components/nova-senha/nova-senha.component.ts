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
    
    this.route.queryParams.subscribe(params => {
      this.fromProfile = params['from'] === 'profile';
    });
    
    this.isUserLoggedIn = this.authService.isAuthenticated();
  }

  private initializeForms(): void {
    this.validarCodigoForm = this.fb.group({
      codigo: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.validarSenhaAtualForm = this.fb.group({
      senhaAtual: ['', [Validators.required]]
    });

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

  validarSenhaAtual(): void {
    if (this.validarSenhaAtualForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const senhaAtual = this.validarSenhaAtualForm.get('senhaAtual')?.value;
      
      setTimeout(() => {
        if (senhaAtual === 'admin123') {
          this.isLoading = false;
          this.senhaAtualValidada = true;
          this.successMessage = 'Senha atual confirmada! Agora defina sua nova senha.';
          
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

  validarCodigo(): void {
    if (this.validarCodigoForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const codigo = this.validarCodigoForm.get('codigo')?.value;
      
      setTimeout(() => {
        if (codigo === '123456') {
          this.isLoading = false;
          this.codigoValidado = true;
          this.successMessage = 'Código validado com sucesso! Agora defina sua nova senha.';
          
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
          
          this.enviarNotificacaoMudancaSenha();
          
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

    if (fieldName === 'confirmarSenha' && this.novaSenhaForm.errors?.['senhasDiferentes']) {
      return 'Senhas não coincidem';
    }
    
    return '';
  }

  getCodigoError(): string {
    return this.getFieldError('codigo', this.validarCodigoForm);
  }

  getSenhaAtualError(): string {
    return this.getFieldError('senhaAtual', this.validarSenhaAtualForm);
  }

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
      this.router.navigate(['/perfil']);
    } else {
      this.router.navigate(['/login']);
    }
  }

  voltarRedefinir(): void {
    this.router.navigate(['/redefinir-senha']);
  }
}
