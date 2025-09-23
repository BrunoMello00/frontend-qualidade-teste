// ========================================
// ENVIRONMENT PARA DESENVOLVIMENTO LOCAL
// ========================================

export const environment = {
  production: false,
  name: 'local',
  
  // API Configuration
  apiUrl: 'http://localhost:8080/api',
  apiTimeout: 30000,
  
  // Authentication
  auth: {
    tokenKey: 'auth_token',
    refreshTokenKey: 'refresh_token',
    tokenExpiry: 3600000 // 1 hora
  },
  
  // Features flags para desenvolvimento
  features: {
    enableDebugMode: true,
    // When true the frontend forces local/mock behavior even if hostname detection
    // would point to a real API. Useful for development and demos.
    enableMockData: true,
    enableConsoleLogging: true,
    enableErrorReporting: false
  },
  
  // Azure configurations (desabilitado em local)
  azure: {
    enabled: false,
    connectionString: '',
    keyVaultUrl: '',
    applicationInsights: {
      instrumentationKey: '',
      enabled: false
    }
  },
  
  // External services (desabilitado em local)
  externalServices: {
    enableAnalytics: false,
    enableCrashReporting: false
  },
  
  // Configurações de desenvolvimento
  development: {
    enableHotReload: true,
    showComponentBorders: false,
    logLevel: 'debug'
  }
};
