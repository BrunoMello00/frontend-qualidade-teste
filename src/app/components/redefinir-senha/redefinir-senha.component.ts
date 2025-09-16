import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-redefinir-senha',
  templateUrl: './redefinir-senha.component.html',
  styleUrls: ['./redefinir-senha.component.css']
})
export class RedefinirSenhaComponent implements OnInit {
  solicitarForm!: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  emailEnviado = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.solicitarForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.solicitarForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';
      
      const email = this.solicitarForm.value.email;
      
      this.authService.solicitarRedefinicaoSenha({ email }).subscribe({
        next: (response: { mensagem: string }) => {
          this.isLoading = false;
          this.successMessage = response.mensagem;
          this.emailEnviado = true;
          console.log('Email de redefinição enviado:', response);
        },
        error: (error: any) => {
          this.isLoading = false;
          this.errorMessage = error.message || 'Erro ao enviar email. Tente novamente.';
          console.error('Erro ao solicitar redefinição:', error);
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.solicitarForm.controls).forEach(key => {
      const control = this.solicitarForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.solicitarForm.get(fieldName);
    
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return 'Email é obrigatório';
      }
      if (field.errors['email']) {
        return 'Email deve ter um formato válido';
      }
    }
    
    return '';
  }

  voltarLogin(): void {
    this.router.navigate(['/login']);
  }

  irParaDefinirSenha(): void {
    this.router.navigate(['/nova-senha']);
  }
}
