// ========================================
// ENVIRONMENT PARA PRODUÇÃO
// ========================================

export const environment = {
  production: true,
  name: 'prod',
  
  // API Configuration - será substituído durante o build
  apiUrl: '${BACKEND_URI}',
  apiTimeout: 30000,
  
  // Authentication
  auth: {
    tokenKey: 'auth_token',
    refreshTokenKey: 'refresh_token',
    tokenExpiry: 3600000 // 1 hora
  },
  
  // Features flags para produção
  features: {
    enableDebugMode: false,
    enableMockData: false,
    enableConsoleLogging: false,
    enableErrorReporting: true
  },
  
  // Azure configurations (habilitado em produção)
  azure: {
    enabled: true,
    connectionString: '${AZURE_CONNECTION_STRING}',
    keyVaultUrl: '${AZURE_KEYVAULT_URI}',
    applicationInsights: {
      instrumentationKey: '${APPINSIGHTS_INSTRUMENTATIONKEY}',
      enabled: true
    }
  },
  
  // External services (habilitado em produção)
  externalServices: {
    enableAnalytics: true,
    enableCrashReporting: true
  },
  
  // Configurações de produção
  development: {
    enableHotReload: false,
    showComponentBorders: false,
    logLevel: 'error'
  }
};
