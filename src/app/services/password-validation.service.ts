import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { BaseService } from './base.service';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';
import { MockDataService } from './mock-data.service';

export interface PasswordStrengthResponse {
  score: number;
  strength: string;
  feedback: string[];
  comprimentoAdequado: boolean;
  temMaiuscula: boolean;
  temMinuscula: boolean;
  temNumero: boolean;
  temCaractereEspecial: boolean;
  semPadroesInseguros: boolean;
  sugestoesMelhoria: string[];
  // Propriedades adicionais para compatibilidade
  isValid: boolean;
  errors: string[];
  valida: boolean;
  pontuacao: number;
  nivel: string;
  erros: string[];
  sugestoes: string[];
}

export interface PasswordValidationResponse {
  valid: boolean;
  errors: string[];
}

export interface PasswordPolicyResponse {
  minLength: number;
  minimoCaracteres: number;
  requireUppercase: boolean;
  requerMaiuscula: boolean;
  requireLowercase: boolean;
  requerMinuscula: boolean;
  requireNumbers: boolean;
  requerNumero: boolean;
  requireSpecialChars: boolean;
  requerCaractereEspecial: boolean;
  forbiddenPasswords: string[];
}

@Injectable({
  providedIn: 'root'
})
export class PasswordValidationService extends BaseService {
  constructor(http: HttpClient, environmentService: EnvironmentService, private mock: MockDataService) {
    super(http, environmentService);
  }

  validarSenha(senha: string): Observable<PasswordValidationResponse> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      const local = this.validatePasswordLocally(senha);
      return of({ valid: local.isValid, errors: local.erros || [] });
    }
    return this.post<PasswordValidationResponse>('password/validar', { senha });
  }

  analisarForcaSenha(senha: string): Observable<PasswordStrengthResponse> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      return of(this.validatePasswordLocally(senha));
    }
    return this.post<PasswordStrengthResponse>('password/analise', { senha });
  }

  obterPoliticaSenha(): Observable<PasswordPolicyResponse> {
    return this.get<PasswordPolicyResponse>('password/politica');
  }

  validarNovaSenha(senhaAtual: string, novaSenha: string): Observable<PasswordValidationResponse> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      const local = this.validatePasswordLocally(novaSenha);
      return of({ valid: local.isValid, errors: local.erros || [] });
    }
    return this.post<PasswordValidationResponse>('password/validar-nova', { 
      senhaAtual, 
      novaSenha 
    });
  }

  // Métodos adicionais para compatibilidade
  analyzePassword(senha: string): Observable<PasswordStrengthResponse> {
    return this.analisarForcaSenha(senha);
  }

  getPasswordPolicy(): Observable<PasswordPolicyResponse> {
    return this.obterPoliticaSenha();
  }

  getPasswordStrengthColor(score: number): string {
    if (score < 2) return 'text-danger';
    if (score < 4) return 'text-warning';
    return 'text-success';
  }

  getPasswordStrengthText(score: number): string {
    if (score < 2) return 'Fraca';
    if (score < 4) return 'Média';
    return 'Forte';
  }

  validatePasswordLocally(password: string): PasswordStrengthResponse {
    let score = 0;
    const feedback: string[] = [];
    const sugestoesMelhoria: string[] = [];
    
    // Verificações de força
    const comprimentoAdequado = password.length >= 8;
    const temMaiuscula = /[A-Z]/.test(password);
    const temMinuscula = /[a-z]/.test(password);
    const temNumero = /\d/.test(password);
    const temCaractereEspecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    const semPadroesInseguros = !/(\d{4,}|(.)\2{2,}|123|abc|password)/i.test(password);
    
    if (comprimentoAdequado) score++;
    else sugestoesMelhoria.push('Use pelo menos 8 caracteres');
    
    if (temMaiuscula) score++;
    else sugestoesMelhoria.push('Inclua pelo menos uma letra maiúscula');
    
    if (temMinuscula) score++;
    else sugestoesMelhoria.push('Inclua pelo menos uma letra minúscula');
    
    if (temNumero) score++;
    else sugestoesMelhoria.push('Inclua pelo menos um número');
    
    if (temCaractereEspecial) score++;
    else sugestoesMelhoria.push('Inclua pelo menos um caractere especial');
    
    if (semPadroesInseguros) score++;
    else sugestoesMelhoria.push('Evite padrões comuns como 123 ou abc');
    
    return {
      score,
      strength: this.getPasswordStrengthText(score),
      feedback: [`Pontuação: ${score}/6`],
      comprimentoAdequado,
      temMaiuscula,
      temMinuscula,
      temNumero,
      temCaractereEspecial,
      semPadroesInseguros,
      sugestoesMelhoria,
      // Propriedades adicionais para compatibilidade
      isValid: score >= 4,
      errors: sugestoesMelhoria,
      valida: score >= 4,
      pontuacao: score,
      nivel: this.getPasswordStrengthText(score),
      erros: sugestoesMelhoria,
      sugestoes: sugestoesMelhoria
    };
  }

  validatePassword(password: string, email?: string): Observable<PasswordStrengthResponse> {
    return this.analisarForcaSenha(password);
  }

  generateStrongPassword(length: number): Observable<{ password: string; senha: string }> {
    if (this.environmentService && this.environmentService.isLocal && this.environmentService.isLocal()) {
      // gerar senha forte localmente
      const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?';
      let pwd = '';
      for (let i = 0; i < (length || 12); i++) pwd += chars[Math.floor(Math.random() * chars.length)];
      return of({ password: pwd, senha: pwd });
    }
    return this.post<{ password: string; senha: string }>('password/gerar-forte', { length });
  }
}
