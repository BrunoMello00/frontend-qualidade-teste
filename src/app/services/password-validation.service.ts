import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseService } from './base.service';

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

  validarSenha(senha: string): Observable<PasswordValidationResponse> {
    return this.post<PasswordValidationResponse>('password/validar', { senha });
  }

  analisarForcaSenha(senha: string): Observable<PasswordStrengthResponse> {
    return this.post<PasswordStrengthResponse>('password/analise', { senha });
  }

  obterPoliticaSenha(): Observable<PasswordPolicyResponse> {
    return this.get<PasswordPolicyResponse>('password/politica');
  }

  validarNovaSenha(senhaAtual: string, novaSenha: string): Observable<PasswordValidationResponse> {
    return this.post<PasswordValidationResponse>('password/validar-nova', { 
      senhaAtual, 
      novaSenha 
    });
  }

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
    return this.post<{ password: string; senha: string }>('password/gerar-forte', { length });
  }
}
