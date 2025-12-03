import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PasswordValidationService, PasswordStrengthResponse, PasswordValidationResponse, PasswordPolicyResponse } from '../../services/password-validation.service';

@Component({
  selector: 'app-password-strength-indicator',
  templateUrl: './password-strength-indicator.component.html',
  styleUrls: ['./password-strength-indicator.component.css']
})
export class PasswordStrengthIndicatorComponent implements OnInit, OnDestroy {
  
  @Input() passwordControl!: FormControl;
  @Input() email?: string;
  @Input() showPolicy: boolean = true;
  @Input() showGenerator: boolean = true;

  private destroy$ = new Subject<void>();
  
  validation = {
    isValid: false,
    score: 0,
    level: '',
    errors: [] as string[],
    suggestions: [] as string[]
  };

  analysis: PasswordStrengthResponse | null = null;
  
  isLoading = false;
  showDetails = false;
  policy: PasswordPolicyResponse | null = null;

  constructor(private passwordService: PasswordValidationService) {}

  ngOnInit(): void {
    this.loadPasswordPolicy();
    this.setupPasswordValidation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupPasswordValidation(): void {
    if (!this.passwordControl) {
      console.error('passwordControl é obrigatório para PasswordStrengthIndicator');
      return;
    }

    this.passwordControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(password => {
        if (password && password.length > 0) {
          this.validatePassword(password);
        } else {
          this.resetValidation();
        }
      });
  }

  private validatePassword(password: string): void {
    const localValidation = this.passwordService.validatePasswordLocally(password);
    this.validation = {
      isValid: localValidation.isValid,
      score: localValidation.score,
      level: this.passwordService.getPasswordStrengthText(localValidation.score),
      errors: localValidation.errors,
      suggestions: []
    };

    this.isLoading = true;
    this.passwordService.validatePassword(password, this.email)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.validation = {
            isValid: response.valida,
            score: response.pontuacao,
            level: response.nivel,
            errors: response.erros || [],
            suggestions: response.sugestoes || []
          };
          this.isLoading = false;
          
          this.getDetailedAnalysis(password);
        },
        error: (error) => {
          console.error('Erro ao validar senha:', error);
          this.isLoading = false;
        }
      });
  }

  private getDetailedAnalysis(password: string): void {
    this.passwordService.analyzePassword(password)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (analysis) => {
          this.analysis = analysis;
        },
        error: (error) => {
          console.error('Erro ao analisar senha:', error);
        }
      });
  }

  private resetValidation(): void {
    this.validation = {
      isValid: false,
      score: 0,
      level: '',
      errors: [],
      suggestions: []
    };
    this.analysis = null;
  }

  private loadPasswordPolicy(): void {
    this.passwordService.getPasswordPolicy()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (policy) => {
          this.policy = policy;
        },
        error: (error) => {
          console.error('Erro ao carregar política de senhas:', error);
        }
      });
  }

  getProgressBarColor(): string {
    return this.passwordService.getPasswordStrengthColor(this.validation.score);
  }

  getProgressBarWidth(): string {
    return `${this.validation.score}%`;
  }

  toggleDetails(): void {
    this.showDetails = !this.showDetails;
  }

  generatePassword(): void {
    this.passwordService.generateStrongPassword(12)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.passwordControl.setValue(response.senha);
          this.passwordControl.markAsTouched();
        },
        error: (error) => {
          console.error('Erro ao gerar senha:', error);
        }
      });
  }

  copyToClipboard(): void {
    const password = this.passwordControl.value;
    if (password && navigator.clipboard) {
      navigator.clipboard.writeText(password).then(() => {
        console.log('Senha copiada para a área de transferência');
      });
    }
  }
}
