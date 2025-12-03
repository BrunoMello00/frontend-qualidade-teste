import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  mfaForm: FormGroup;
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  
  showMfaStep = false;
  mfaMethod: 'email' | 'sms' = 'email';
  mfaDestination = '';
  userEmail = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required]],
      senha: ['', [Validators.required]]
    });

    this.mfaForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.redirectToHomePage();
    }
  }

  private redirectToHomePage(): void {
    if (this.authService.canViewDashboard()) {
      this.router.navigate(['/dashboard']);
    } else if (this.authService.canManageSales() || this.authService.canViewProducts()) {
      this.router.navigate(['/produtos']);
    } else {
      this.router.navigate(['/perfil']);
    }
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const email = this.loginForm.get('email')?.value;
      const senha = this.loginForm.get('senha')?.value;
      this.userEmail = email;

      this.authService.checkMfaRequired(email, senha).subscribe({
        next: (mfaResponse: { mfaRequired?: boolean; requiresMfa?: boolean; mfaMethod?: string; destination?: string; message?: string }) => {
          if (mfaResponse.requiresMfa || mfaResponse.mfaRequired) {
            this.isLoading = false;
            this.showMfaStep = true;
            this.mfaMethod = (mfaResponse.mfaMethod as 'email' | 'sms') || 'email';
            this.mfaDestination = mfaResponse.destination || '';
            this.successMessage = mfaResponse.message || 'Código MFA enviado';
            this.clearMessages();
          } else {
            this.performLogin(email, senha);
          }
        },
        error: (error: any) => {
          this.isLoading = false;
          this.errorMessage = 'Email ou senha inválidos.';
          console.error('Erro no login:', error);
        }
      });
    } else {
      this.errorMessage = 'Por favor, preencha todos os campos.';
      this.markFormGroupTouched();
    }
  }

  private performLogin(email: string, senha: string): void {
    this.authService.login(email, senha).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.redirectToHomePage();
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = 'Email ou senha inválidos.';
        console.error('Erro no login:', error);
      }
    });
  }

  onMfaSubmit(): void {
    if (this.mfaForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';
      
      const code = this.mfaForm.get('code')?.value;
      
      this.authService.verifyMfaCode(this.userEmail, code).subscribe({
        next: (response: { success: boolean; message: string }) => {
          this.isLoading = false;
          
          if (response.success) {
            this.successMessage = response.message;
            setTimeout(() => {
              this.redirectToHomePage();
            }, 1000);
          } else {
            this.errorMessage = response.message;
          }
        },
        error: (error: any) => {
          this.isLoading = false;
          this.errorMessage = 'Erro ao verificar código. Tente novamente.';
          console.error('Erro MFA verify:', error);
        }
      });
    } else {
      this.errorMessage = 'Digite um código de 6 dígitos.';
    }
  }

  resendMfaCode(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    
    this.authService.resendMfaCode(this.userEmail).subscribe({
      next: (response: { message: string }) => {
        this.isLoading = false;
        this.successMessage = response.message;
        this.mfaForm.get('code')?.setValue('');
      },
      error: (error: any) => {
        this.isLoading = false;
        this.errorMessage = 'Erro ao reenviar código. Tente novamente.';
        console.error('Erro resend MFA:', error);
      }
    });
  }

  backToLogin(): void {
    this.showMfaStep = false;
    this.mfaForm.reset();
    this.clearMessages();
  }

  private markFormGroupTouched(): void {
    Object.keys(this.loginForm.controls).forEach(key => {
      const control = this.loginForm.get(key);
      control?.markAsTouched();
    });
  }

  getErrorMessage(field: string): string {
    const control = this.loginForm.get(field);
    
    if (control?.hasError('required')) {
      return `${field === 'email' ? 'Email' : 'Senha'} é obrigatório`;
    }
    
    return '';
  }

  irParaRedefinirSenha(): void {
    this.router.navigate(['/redefinir-senha']);
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
