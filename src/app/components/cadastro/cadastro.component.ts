/*
 * ==========================================
 * COMPONENTE DE CADASTRO - TEMPORARIAMENTE DESABILITADO
 * ==========================================
 * 
 * Este componente foi desenvolvido mas está desabilitado pois o sistema
 * é para uso único/interno. Mantido aqui para possível uso futuro.
 * 
 * Para reabilitar:
 * 1. Descomentar importações em app.module.ts e app-routing.module.ts
 * 2. Adicionar rota '/cadastro' no routing
 * 3. Descomentar método cadastrar() no auth.service.ts
 * 4. Adicionar link de cadastro na tela de login
 * 
 * ==========================================
 */

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

// INTERFACES LOCAIS PARA CADASTRO DESABILITADO
interface CadastroRequest {
  nome: string;
  email: string;
  senha: string;
  confirmarSenha: string;
  telefone: string;
  cpf?: string;
}

interface CadastroResponse {
  sucesso: boolean;
  mensagem: string;
  usuario?: any;
}

@Component({
  selector: 'app-cadastro',
  templateUrl: './cadastro.component.html',
  styleUrls: ['./cadastro.component.css']
})
export class CadastroComponent implements OnInit {
  cadastroForm!: FormGroup;
  isLoading = false;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cadastroForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required]],
      confirmarSenha: ['', [Validators.required]],
      aceitaTermos: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });

    // Se já estiver autenticado, redirecionar para o dashboard
    if (this.authService.getToken()) {
      this.router.navigate(['/dashboard']);
    }
  }

  // Validador personalizado para verificar se as senhas coincidem
  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const senha = control.get('senha');
    const confirmarSenha = control.get('confirmarSenha');

    if (senha && confirmarSenha && senha.value !== confirmarSenha.value) {
      confirmarSenha.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
      if (confirmarSenha?.hasError('passwordMismatch')) {
        confirmarSenha.setErrors(null);
      }
      return null;
    }
  }

  // Métodos para o template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.cadastroForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getPasswordControl(): FormControl {
    return this.cadastroForm.get('senha') as FormControl;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
    const passwordInput = document.getElementById('senha') as HTMLInputElement;
    if (passwordInput) {
      passwordInput.type = this.showPassword ? 'text' : 'password';
    }
  }

  limparFormulario(): void {
    this.cadastroForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  senhasIguais(form: FormGroup) {
    const senha = form.get('senha');
    const confirmarSenha = form.get('confirmarSenha');
    
    if (senha && confirmarSenha && senha.value !== confirmarSenha.value) {
      return { senhasDiferentes: true };
    }
    return null;
  }

  onSubmit() {
    if (this.cadastroForm.invalid) {
      this.markFormGroupTouched();
      this.errorMessage = 'Por favor, corrija os erros no formulário.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    // MÉTODO DE CADASTRO DESABILITADO
    // Este sistema é para uso único/interno - cadastro não necessário
    setTimeout(() => {
      this.errorMessage = 'Função de cadastro desabilitada. Entre em contato com o administrador.';
      this.isSubmitting = false;
    }, 1000);
    
    /*
    // CÓDIGO ORIGINAL PRESERVADO PARA USO FUTURO
    const dadosCadastro = {
      nome: this.cadastroForm.get('nome')?.value,
      email: this.cadastroForm.get('email')?.value,
      senha: this.cadastroForm.get('senha')?.value,
      confirmarSenha: this.cadastroForm.get('confirmarSenha')?.value
    };

    this.authService.cadastrar(dadosCadastro).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        if (response.sucesso) {
          this.successMessage = response.mensagem;
          this.errorMessage = '';
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        } else {
          this.errorMessage = response.mensagem;
        }
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = 'Erro no servidor. Tente novamente.';
        console.error('Erro no cadastro:', error);
      }
    });
    */
  }

  private markFormGroupTouched(): void {
    Object.keys(this.cadastroForm.controls).forEach(key => {
      const control = this.cadastroForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.cadastroForm.get(fieldName);
    
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        const fieldNames: any = {
          nome: 'Nome',
          email: 'Email',
          telefone: 'Telefone',
          senha: 'Senha',
          confirmarSenha: 'Confirmação de senha'
        };
        return `${fieldNames[fieldName]} é obrigatório`;
      }
      if (field.errors['email']) {
        return 'Email deve ter um formato válido';
      }
      if (field.errors['minlength']) {
        if (fieldName === 'nome') {
          return 'Nome deve ter pelo menos 2 caracteres';
        }
        if (fieldName === 'senha') {
          return 'Senha deve ter pelo menos 6 caracteres';
        }
      }
      if (field.errors['pattern']) {
        if (fieldName === 'telefone') {
          return 'Telefone deve estar no formato (11) 99999-9999';
        }
        if (fieldName === 'cpf') {
          return 'CPF deve estar no formato 999.999.999-99';
        }
      }
    }

    // Verificar erro de senhas diferentes
    if (fieldName === 'confirmarSenha' && this.cadastroForm.errors?.['senhasDiferentes']) {
      return 'Senhas não coincidem';
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
    this.cadastroForm.get('telefone')?.setValue(value);
  }

  // Máscara para CPF
  onCpfInput(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 11) {
      value = value.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/, '$1.$2.$3-$4');
    }
    
    event.target.value = value;
    this.cadastroForm.get('cpf')?.setValue(value);
  }

  voltarLogin(): void {
    this.router.navigate(['/login']);
  }

  // MÉTODOS DE NAVEGAÇÃO PARA TEMPLATE DESABILITADO
  irParaLogin() {
    this.router.navigate(['/login']);
  }

  irParaRedefinirSenha() {
    this.router.navigate(['/redefinir-senha']);
  }
}
