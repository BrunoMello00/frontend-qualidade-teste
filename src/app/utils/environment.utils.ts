import { EnvironmentService } from '../services/environment.service';

/**
 * Utilitários para configuração de ambiente
 */
export class EnvironmentUtils {
  
  /**
   * Função para inicializar logs de ambiente na aplicação
   */
  static initializeEnvironmentLogging(): void {
    console.group('🚀 Application Startup Environment Check');
    
    const hostname = window.location.hostname;
    const protocol = window.location.protocol;
    const port = window.location.port;
    const isDev = hostname === 'localhost' || hostname === '127.0.0.1';
    
    console.log('🌐 Current URL:', window.location.href);
    console.log('🏠 Hostname:', hostname);
    console.log('🔗 Protocol:', protocol);
    console.log('🚪 Port:', port);
    
    // Detectar ambiente baseado na URL
    const environment = EnvironmentUtils.detectEnvironment();
    console.log('🌍 Detected Environment:', environment);
    
    // Validar configuração
    const validation = EnvironmentUtils.validateConfiguration();
    if (validation.valid) {
    } else {
      console.warn('⚠️ Environment configuration issues:', validation.issues);
    }
    
    console.groupEnd();
  }
  
  /**
   * Detecta o ambiente baseado na URL atual
   */
  static detectEnvironment(): 'local' | 'azure' | 'unknown' {
    const hostname = window.location.hostname;
    
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'local';
    }
    
    if (hostname.includes('azurestaticapps.net') || 
        hostname.includes('azurewebsites.net') ||
        hostname.includes('azure.com')) {
      return 'azure';
    }
    
    return 'unknown';
  }
  
  /**
   * Valida se a configuração do ambiente está correta
   */
  static validateConfiguration(): { valid: boolean; issues: string[] } {
    const issues: string[] = [];
    
    try {
      // Verificar se as URLs são válidas
      const localUrl = 'http://localhost:8080/api';
      const azureUrl = 'https://app-backend-estoque-vendas.bravesky-fa21ce16.eastus2.azurecontainerapps.io/api';
      
      try {
        new URL(localUrl);
      } catch {
        issues.push('Local API URL is malformed');
      }
      
      try {
        new URL(azureUrl);
      } catch {
        issues.push('Azure API URL is malformed');
      }
      
      // Verificar se window.location está disponível
      if (typeof window === 'undefined' || !window.location) {
        issues.push('Window location not available (SSR environment?)');
      }
      
    } catch (error) {
      issues.push(`Configuration validation error: ${error}`);
    }
    
    return {
      valid: issues.length === 0,
      issues
    };
  }
  
  /**
   * Função para testar conectividade com uma URL
   */
  static async testConnectivity(url: string): Promise<{
    success: boolean;
    responseTime: number;
    error?: string;
  }> {
    const startTime = Date.now();
    
    try {
      const response = await fetch(`${url}/actuator/health`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        },
        // Timeout de 5 segundos
        signal: AbortSignal.timeout(5000)
      });
      
      const responseTime = Date.now() - startTime;
      
      if (response.ok) {
        return {
          success: true,
          responseTime
        };
      } else {
        return {
          success: false,
          responseTime,
          error: `HTTP ${response.status}: ${response.statusText}`
        };
      }
    } catch (error) {
      const responseTime = Date.now() - startTime;
      return {
        success: false,
        responseTime,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }
  
  /**
   * Função para executar diagnóstico completo do ambiente
   */
  static async runDiagnostics(): Promise<{
    environment: string;
    currentUrl: string;
    expectedApiUrl: string;
    connectivity: {
      local: any;
      azure: any;
    };
    configuration: any;
  }> {
    const environment = EnvironmentUtils.detectEnvironment();
    const configuration = EnvironmentUtils.validateConfiguration();
    
    const localUrl = 'http://localhost:8080/api';
    const azureUrl = 'https://app-backend-estoque-vendas.bravesky-fa21ce16.eastus2.azurecontainerapps.io/api';
    
    // Determinar qual URL deveria estar sendo usada
    let expectedApiUrl = localUrl;
    if (environment === 'azure') {
      expectedApiUrl = azureUrl;
    }
    
    // Testar conectividade com ambas as URLs
    const [localConnectivity, azureConnectivity] = await Promise.all([
      EnvironmentUtils.testConnectivity(localUrl),
      EnvironmentUtils.testConnectivity(azureUrl)
    ]);
    
    return {
      environment,
      currentUrl: window.location.href,
      expectedApiUrl,
      connectivity: {
        local: localConnectivity,
        azure: azureConnectivity
      },
      configuration
    };
  }
}