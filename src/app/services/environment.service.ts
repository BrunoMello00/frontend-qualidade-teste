import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {
  
  private readonly localApiUrl = 'http://localhost:8080/api';
  private readonly azureApiUrl = 'https://app-backend-estoque-vendas.bravesky-fa21ce16.eastus2.azurecontainerapps.io/api';

  constructor() {}

  /**
   * Detecta automaticamente se está rodando localmente ou em produção
   */
  getApiUrl(): string {
    // Se environment.production for true, usar Azure
    if (environment.production) {
      return this.azureApiUrl;
    }

    // Se hostname for localhost ou 127.0.0.1, usar local
    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return this.localApiUrl;
    }

    // Se estiver rodando em Azure Static Web Apps ou outro domínio, usar Azure API
    if (hostname.includes('azurestaticapps.net') || 
        hostname.includes('azurewebsites.net') ||
        hostname.includes('azure.com') ||
        !hostname.includes('localhost')) {
      return this.azureApiUrl;
    }

    // Fallback para local se não conseguir detectar
    return this.localApiUrl;
  }

  /**
   * Verifica se está rodando em ambiente local
   */
  isLocal(): boolean {
    return this.getApiUrl() === this.localApiUrl;
  }

  /**
   * Verifica se está rodando em produção/Azure
   */
  isProduction(): boolean {
    return this.getApiUrl() === this.azureApiUrl;
  }

  /**
   * Obtém informações completas do ambiente
   */
  getEnvironmentInfo() {
    const apiUrl = this.getApiUrl();
    return {
      apiUrl,
      isLocal: this.isLocal(),
      isProduction: this.isProduction(),
      hostname: window.location.hostname,
      protocol: window.location.protocol,
      port: window.location.port,
      environment: environment.production ? 'production' : 'development'
    };
  }

  /**
   * Testa conectividade com o backend
   */
  async testConnection(): Promise<{ success: boolean; message: string; url: string }> {
    const apiUrl = this.getApiUrl();
    
    try {
      const response = await fetch(`${apiUrl}/actuator/health`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        return {
          success: true,
          message: 'Conexão com backend estabelecida',
          url: apiUrl
        };
      } else {
        return {
          success: false,
          message: `Erro na conexão: ${response.status} - ${response.statusText}`,
          url: apiUrl
        };
      }
    } catch (error) {
      return {
        success: false,
        message: `Erro de conectividade: ${error}`,
        url: apiUrl
      };
    }
  }

  /**
   * Log de informações do ambiente no console
   */
  logEnvironmentInfo(): void {
    const info = this.getEnvironmentInfo();
    console.group('🌍 Environment Information');
    console.log('📡 API URL:', info.apiUrl);
    console.log('🏠 Is Local:', info.isLocal);
    console.log('☁️ Is Production:', info.isProduction);
    console.log('🌐 Hostname:', info.hostname);
    console.log('🔗 Protocol:', info.protocol);
    console.log('🚪 Port:', info.port);
    console.log('⚙️ Environment:', info.environment);
    console.groupEnd();
  }
}