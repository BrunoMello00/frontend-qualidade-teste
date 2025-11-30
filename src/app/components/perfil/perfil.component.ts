import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UsuarioService, PerfilUsuario, AlterarSenhaRequest } from '../../services/usuario.service';
import { TIPO_USUARIO_LABELS, TipoUsuario } from '../../models/user.models';

@Component({
  selector: 'app-perfil',
  templateUrl: './perfil.component.html',
  styleUrls: ['./perfil.component.css']
})
export class PerfilComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  perfilForm!: FormGroup;
  senhaForm!: FormGroup;
  
  currentUser: any = null;
  perfil: PerfilUsuario | null = null;
  
  isEditing = false;
  isLoading = false;
  isLoadingPerfil = false;
  isChangingPassword = false;
  showPasswordForm = false;
  mostrarFormularioSenha = false;
  
  successMessage = '';
  errorMessage = '';
  passwordSuccessMessage = '';
  passwordErrorMessage = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadUserProfile();
    this.setupSubscriptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }


  private initializeForms(): void {
    this.perfilForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telefone: ['', [Validators.pattern(/^\(\d{2}\)\s\d{4,5}-\d{4}$/)]],
      cargo: [{ value: '', disabled: true }],
      departamento: [{ value: '', disabled: true }], // Novo campo
      dataUltimoLogin: [{ value: '', disabled: true }],
      totalLoginsMes: [{ value: '', disabled: true }],
      dataCriacao: [{ value: '', disabled: true }]
    });

    this.senhaForm = this.fb.group({
      senhaAtual: ['', [Validators.required]],
      novaSenha: ['', [Validators.required, Validators.minLength(6)]],
      confirmarNovaSenha: ['', [Validators.required]]
    }, {
      validators: this.passwordsMatchValidator
    });
  }

  private setupSubscriptions(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
        if (user) {
          this.updateFormWithUserData(user);
        }
      });
  }


  private loadUserProfile(): void {
    this.isLoadingPerfil = true;
    this.clearMessages();

    this.usuarioService.obterPerfil()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (perfil) => {
          this.perfil = perfil;
          this.updateFormWithProfileData(perfil);
          this.isLoadingPerfil = false;
        },
        error: (error) => {
          console.error('❌ Erro ao carregar perfil:', error);
          this.errorMessage = 'Erro ao carregar dados do perfil';
          this.isLoadingPerfil = false;
        }
      });
  }

  private updateFormWithUserData(user: any): void {
    if (!user) return;

    const cargo = user.tipoUsuario ? (TIPO_USUARIO_LABELS[user.tipoUsuario as TipoUsuario] || 'Usuário') : 'Usuário';

    this.perfilForm.patchValue({
      nome: user.nome || '',
      email: user.email || '',
      telefone: '(11) 99999-9999', // Placeholder
      cargo: cargo,
      dataUltimoLogin: user.ultimoLogin ? 
        new Date(user.ultimoLogin).toLocaleDateString('pt-BR', {
          day: '2-digit',
          month: '2-digit', 
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        }) : 'Nunca',
      dataCriacao: user.dataCriacao ? 
        new Date(user.dataCriacao).toLocaleDateString('pt-BR') : 'N/A'
    });
  }

  private updateFormWithProfileData(perfil: any): void {
    if (!perfil) return;

    this.perfilForm.patchValue({
      nome: perfil.nome || '',
      email: perfil.email || '',
      telefone: perfil.telefone || '',
      cargo: perfil.tipoUsuario ? this.usuarioService.formatarTipoUsuario(perfil.tipoUsuario) : 'Usuário',
      departamento: perfil.departamento || 'Geral', // Novo campo
      dataUltimoLogin: perfil.ultimoLogin ? 
        new Date(perfil.ultimoLogin).toLocaleDateString('pt-BR', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        }) : 'Nunca',
      totalLoginsMes: perfil.totalLoginsMes || 0,
      dataCriacao: perfil.dataCriacao ? 
        new Date(perfil.dataCriacao).toLocaleDateString('pt-BR') : 'N/A'
    });
  }


  enableEditing(): void {
    this.isEditing = true;
    this.clearMessages();
  }

  cancelEditing(): void {
    console.log('❌ Cancelando edição do perfil');
    this.isEditing = false;
    this.clearMessages();
    
    if (this.perfil) {
      this.updateFormWithProfileData(this.perfil);
    } else if (this.currentUser) {
      this.updateFormWithUserData(this.currentUser);
    }
  }

  onSubmit(): void {
    if (this.perfilForm.invalid) {
      console.log('❌ Formulário inválido');
      this.markFormGroupTouched(this.perfilForm);
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    const dadosAtualizacao = {
      nome: this.perfilForm.get('nome')?.value,
      email: this.perfilForm.get('email')?.value,
      telefone: this.perfilForm.get('telefone')?.value
    };

    console.log('💾 Salvando alterações do perfil:', dadosAtualizacao);

    this.usuarioService.atualizarPerfil(dadosAtualizacao)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (perfilAtualizado) => {
          this.perfil = perfilAtualizado;
          this.successMessage = 'Perfil atualizado com sucesso!';
          this.isEditing = false;
          this.isLoading = false;
          
          setTimeout(() => this.clearMessages(), 3000);
        },
        error: (error) => {
          console.error('❌ Erro ao atualizar perfil:', error);
          this.errorMessage = 'Erro ao atualizar perfil. Tente novamente.';
          this.isLoading = false;
        }
      });
  }


  togglePasswordForm(): void {
    this.showPasswordForm = !this.showPasswordForm;
    this.clearPasswordMessages();
    
    if (this.showPasswordForm) {
      this.senhaForm.reset();
      console.log('🔐 Abrindo formulário de alteração de senha');
    } else {
      console.log('❌ Fechando formulário de alteração de senha');
    }
  }

  onPasswordSubmit(): void {
    if (this.senhaForm.invalid) {
      console.log('❌ Formulário de senha inválido');
      this.markFormGroupTouched(this.senhaForm);
      return;
    }

    this.isChangingPassword = true;
    this.clearPasswordMessages();

    const senhaRequest: AlterarSenhaRequest = {
      senhaAtual: this.senhaForm.get('senhaAtual')?.value,
      novaSenha: this.senhaForm.get('novaSenha')?.value,
      confirmarNovaSenha: this.senhaForm.get('confirmarNovaSenha')?.value
    };

    console.log('🔐 Alterando senha...');

    this.usuarioService.alterarSenha(senhaRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.passwordSuccessMessage = 'Senha alterada com sucesso!';
          this.senhaForm.reset();
          this.isChangingPassword = false;
          
          setTimeout(() => {
            this.showPasswordForm = false;
            this.clearPasswordMessages();
          }, 2000);
        },
        error: (error) => {
          console.error('❌ Erro ao alterar senha:', error);
          this.passwordErrorMessage = 'Erro ao alterar senha. Verifique a senha atual.';
          this.isChangingPassword = false;
        }
      });
  }


  private passwordsMatchValidator(group: FormGroup) {
    const novaSenha = group.get('novaSenha')?.value;
    const confirmarNovaSenha = group.get('confirmarNovaSenha')?.value;
    
    return novaSenha === confirmarNovaSenha ? null : { passwordMismatch: true };
  }

  isFieldInvalid(formGroup: FormGroup, fieldName: string): boolean {
    const field = formGroup.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(field: string, formName?: string): string {
    const form = formName === 'senha' ? this.senhaForm : this.perfilForm;
    const control = form.get(field);
    
    if (control && control.errors && control.touched) {
      if (control.errors['required']) {
        return `${field} é obrigatório`;
      }
      if (control.errors['email']) {
        return 'Email inválido';
      }
      if (control.errors['minlength']) {
        return `${field} deve ter pelo menos ${control.errors['minlength'].requiredLength} caracteres`;
      }
      if (control.errors['pattern']) {
        return `${field} inválido`;
      }
      if (control.errors['passwordMismatch']) {
        return 'Senhas não coincidem';
      }
    }
    return '';
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }


  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private clearPasswordMessages(): void {
    this.passwordSuccessMessage = '';
    this.passwordErrorMessage = '';
  }

  formatarTelefone(event: any): void {
    let valor = event.target.value.replace(/\D/g, '');
    
    if (valor.length <= 11) {
      valor = valor.replace(/(\d{2})(\d{4,5})(\d{4})/, '($1) $2-$3');
    }
    
    event.target.value = valor;
    this.perfilForm.get('telefone')?.setValue(valor);
  }

  private detectarCamposAlterados(dadosOriginais: any, dadosNovos: any): string[] {
    const camposAlterados: string[] = [];
    
    for (const campo in dadosNovos) {
      if (dadosOriginais[campo] !== dadosNovos[campo]) {
        camposAlterados.push(campo);
      }
    }
    
    return camposAlterados;
  }

  private getDepartamentoPorTipo(tipoUsuario: TipoUsuario): string {
    const departamentos = {
      [TipoUsuario.OWNER]: 'Administração',
      [TipoUsuario.ADMIN]: 'Tecnologia da Informação', 
      [TipoUsuario.VENDEDOR]: 'Vendas e Atendimento',
      [TipoUsuario.ESTOQUISTA]: 'Estoque e Logística',
      [TipoUsuario.COMPRAS]: 'Compras e Suprimentos'
    };
    return departamentos[tipoUsuario] || 'Geral';
  }

  get nome() { return this.perfilForm.get('nome'); }
  get email() { return this.perfilForm.get('email'); }
  get telefone() { return this.perfilForm.get('telefone'); }
  get senhaAtual() { return this.senhaForm.get('senhaAtual'); }
  get novaSenha() { return this.senhaForm.get('novaSenha'); }
  get confirmarNovaSenha() { return this.senhaForm.get('confirmarNovaSenha'); }

  irParaAlterarSenha(): void {
    this.mostrarFormularioSenha = true;
  }

  onTelefoneInput(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    if (value.length <= 11) {
      if (value.length === 11) {
        value = value.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
      } else if (value.length === 10) {
        value = value.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
      }
      this.perfilForm.patchValue({ telefone: value });
    }
  }

  goToConfiguracoes(): void {
    this.router.navigate(['/configuracoes']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}