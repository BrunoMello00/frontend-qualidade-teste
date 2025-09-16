import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { SessionTimeoutService } from '../../services/session-timeout.service';
import { Router } from '@angular/router';

interface MfaConfig {
  enabled: boolean;
  method: 'email' | 'sms' | null;
  email?: string;
  phone?: string;
}

@Component({
  selector: 'app-configuracoes',
  templateUrl: './configuracoes.component.html',
  styleUrls: ['./configuracoes.component.css']
})
export class ConfiguracoesComponent implements OnInit {
  configForm!: FormGroup;
  mfaForm!: FormGroup;
  
  mfaConfig: MfaConfig = {
    enabled: false,
    method: null
  };
  
  isLoading = false;
  successMessage = '';
  errorMessage = '';
  
  // Estados de configuração
  activeTab = 'geral';
  showMfaSetup = false;
  mfaStep = 1; // 1: escolher método, 2: configurar, 3: verificar

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private sessionTimeoutService: SessionTimeoutService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadConfigurations();
    
    // Inicializar monitoramento de sessão
    this.sessionTimeoutService.startMonitoring();
    
    // Escutar mudanças no formulário para aplicar em tempo real
    this.configForm.get('sessaoTimeout')?.valueChanges.subscribe(timeout => {
      if (timeout) {
        console.log('⏱️ Mudança de timeout detectada:', timeout);
        this.sessionTimeoutService.setTimeoutDuration(timeout);
        this.salvarConfiguracaoImediata('sessaoTimeout', timeout);
        this.showToast(`Timeout configurado para ${timeout} minutos`, 'success');
      }
    });
  }

  private initializeForms(): void {
    // Formulário de configurações gerais (dados sempre sincronizados)
    this.configForm = this.fb.group({
      manterLogado: [false],
      sessaoTimeout: [30]
    });

    // Formulário de MFA
    this.mfaForm = this.fb.group({
      method: ['', Validators.required],
      email: [''],
      phone: [''],
      verificationCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  private loadConfigurations(): void {
    console.log('📂 Carregando configurações...');
    
    // Carregar configurações salvas do localStorage ou API
    const savedConfig = localStorage.getItem('userConfig');
    if (savedConfig) {
      const config = JSON.parse(savedConfig);
      console.log('📋 Configurações encontradas:', config);
      this.configForm.patchValue(config);
      
      // Aplicar configurações carregadas
      this.aplicarConfiguracoes(config);
    } else {
      console.log('🆕 Primeira vez - aplicando configurações padrão');
      // Aplicar configurações padrão na primeira vez
      const defaultConfig = this.configForm.value;
      this.aplicarConfiguracoes(defaultConfig);
      // Salvar configurações padrão
      localStorage.setItem('userConfig', JSON.stringify(defaultConfig));
    }

    // Carregar configuração MFA
    const savedMfa = localStorage.getItem('mfaConfig');
    if (savedMfa) {
      this.mfaConfig = JSON.parse(savedMfa);
      console.log('🔐 Configuração MFA carregada:', this.mfaConfig);
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.clearMessages();
  }

  // Configurações Gerais
  onConfigSubmit(): void {
    if (this.configForm.valid) {
      this.isLoading = true;
      this.clearMessages();

      setTimeout(() => {
        const config = this.configForm.value;
        
        // Salvar configurações
        localStorage.setItem('userConfig', JSON.stringify(config));
        
        // Aplicar mudanças imediatamente
        this.aplicarConfiguracoes(config);
        
        this.isLoading = false;
        this.successMessage = 'Configurações salvas e aplicadas com sucesso!';
        
        this.clearMessagesAfterDelay();
      }, 1000);
    }
  }

  private aplicarConfiguracoes(config: any): void {
    console.log('⚙️ Aplicando configurações:', config);
    
    // Configurar timeout de sessão usando o novo serviço
    this.sessionTimeoutService.setTimeoutDuration(config.sessaoTimeout);
    this.sessionTimeoutService.startMonitoring();
    
    // Log para debug
    console.log('✅ Configurações aplicadas com sucesso:', config);
  }

  private salvarConfiguracaoImediata(campo: string, valor: any): void {
    // Carregar configurações existentes
    const savedConfig = localStorage.getItem('userConfig');
    let config = savedConfig ? JSON.parse(savedConfig) : this.configForm.value;
    
    // Atualizar campo específico
    config[campo] = valor;
    
    // Salvar de volta no localStorage
    localStorage.setItem('userConfig', JSON.stringify(config));
    
    console.log('💾 Configuração salva automaticamente:', campo, '=', valor);
  }

  private aplicarTema(tema: string): void {
    console.log('🎨 Aplicando tema:', tema);
    const body = document.body;
    
    // Remover todas as classes de tema existentes
    body.classList.remove('tema-claro', 'tema-escuro', 'tema-auto');
    
    // Adicionar nova classe de tema
    switch (tema) {
      case 'escuro':
        body.classList.add('tema-escuro');
        body.setAttribute('data-bs-theme', 'dark');
        console.log('✅ Tema escuro aplicado');
        this.showToast('Tema escuro aplicado!', 'success');
        break;
      case 'auto':
        body.classList.add('tema-auto');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        body.setAttribute('data-bs-theme', prefersDark ? 'dark' : 'light');
        console.log('✅ Tema automático aplicado (sistema:', prefersDark ? 'escuro' : 'claro', ')');
        this.showToast('Tema automático aplicado!', 'success');
        break;
      default: // 'claro'
        body.classList.add('tema-claro');
        body.setAttribute('data-bs-theme', 'light');
        console.log('✅ Tema claro aplicado');
        this.showToast('Tema claro aplicado!', 'success');
        break;
    }
    
    // Log das classes aplicadas para debug
    console.log('🔍 Classes do body após aplicar tema:', Array.from(body.classList));
  }

  private showToast(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'success'): void {
    const toast = document.createElement('div');
    const bgClass = {
      'success': 'bg-success',
      'error': 'bg-danger', 
      'warning': 'bg-warning',
      'info': 'bg-info'
    };
    
    toast.className = `toast align-items-center text-white ${bgClass[type]} border-0 position-fixed`;
    toast.style.top = '20px';
    toast.style.right = '20px';
    toast.style.zIndex = '9999';
    toast.setAttribute('role', 'alert');
    
    toast.innerHTML = `
      <div class="d-flex">
        <div class="toast-body">
          <i class="bi bi-${type === 'success' ? 'check-circle' : 
                          type === 'error' ? 'exclamation-circle' : 
                          type === 'warning' ? 'exclamation-triangle' : 'info-circle'} me-2"></i>
          ${message}
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="this.parentElement.parentElement.remove()"></button>
      </div>
    `;
    
    document.body.appendChild(toast);
    
    // Auto remover após 4 segundos
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 4000);
  }

  // MFA Setup
  startMfaSetup(): void {
    this.showMfaSetup = true;
    this.mfaStep = 1;
    this.clearMessages();
  }

  goToMfaStep(step: number): void {
    this.mfaStep = step;
  }

  selectMfaMethod(method: 'email' | 'sms'): void {
    this.mfaForm.patchValue({ method });
    this.mfaStep = 2;
    
    // Pre-preencher dados se disponíveis
    if (method === 'email') {
      this.authService.currentUser$.subscribe(user => {
        if (user?.email) {
          this.mfaForm.patchValue({ email: user.email });
        }
      });
    }
  }

  configureMfa(): void {
    if (this.mfaForm.get('method')?.value) {
      this.isLoading = true;
      this.clearMessages();

      // Simular envio de código
      setTimeout(() => {
        this.isLoading = false;
        this.mfaStep = 3;
        this.successMessage = `Código de verificação enviado para ${this.getMfaDestination()}`;
      }, 1500);
    }
  }

  verifyMfaCode(): void {
    if (this.mfaForm.invalid) {
      this.errorMessage = 'Por favor, verifique os dados informados.';
      return;
    }

    const code = this.mfaForm.get('verificationCode')?.value;
    
    if (!code || code.length !== 6) {
      this.errorMessage = 'Digite um código de 6 dígitos.';
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    // Simular verificação do código
    setTimeout(() => {
      // Código de demonstração sempre aceita '123456'
      if (code === '123456') {
        const oldMfaEnabled = this.mfaConfig.enabled;
        
        this.mfaConfig = {
          enabled: true,
          method: this.mfaForm.get('method')?.value,
          email: this.mfaForm.get('email')?.value,
          phone: this.mfaForm.get('phone')?.value
        };
        
        localStorage.setItem('mfaConfig', JSON.stringify(this.mfaConfig));
        
        this.isLoading = false;
        this.showMfaSetup = false;
        this.mfaStep = 1; // Reset to step 1
        this.mfaForm.reset(); // Clear form
        this.successMessage = 'Autenticação multifator configurada com sucesso!';
        
        this.clearMessagesAfterDelay();
      } else {
        this.isLoading = false;
        this.errorMessage = 'Código inválido. Tente novamente.';
      }
    }, 1000);
  }

  disableMfa(): void {
    this.mfaConfig = {
      enabled: false,
      method: null
    };
    
    localStorage.setItem('mfaConfig', JSON.stringify(this.mfaConfig));
    this.successMessage = 'Autenticação multifator desabilitada.';
    
    this.clearMessagesAfterDelay();
  }

  cancelMfaSetup(): void {
    this.showMfaSetup = false;
    this.mfaStep = 1;
    this.mfaForm.reset();
    this.clearMessages();
  }

  // Utilitários
  getMfaDestination(): string {
    const method = this.mfaForm.get('method')?.value;
    if (method === 'email') {
      return this.mfaForm.get('email')?.value || '';
    } else if (method === 'sms') {
      return this.mfaForm.get('phone')?.value || '';
    }
    return '';
  }

  onPhoneInput(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    
    if (value.length <= 11) {
      if (value.length <= 10) {
        value = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
      } else {
        value = value.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3');
      }
    }
    
    event.target.value = value;
    this.mfaForm.get('phone')?.setValue(value);
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private clearMessagesAfterDelay(): void {
    setTimeout(() => {
      this.clearMessages();
    }, 3000);
  }

  // Reset de configurações
  resetConfigurations(): void {
    if (confirm('Tem certeza que deseja restaurar as configurações padrão?\n\nIsso irá:\n- Restaurar timeout para 30 minutos\n- Desativar MFA\n- Restaurar todas as outras configurações')) {
      
      this.isLoading = true;
      
      setTimeout(() => {
        // Limpar configurações salvas
        localStorage.removeItem('userConfig');
        localStorage.removeItem('mfaConfig');
        
        // Limpar timeout de sessão
        if ((window as any).sessionTimeout) {
          clearTimeout((window as any).sessionTimeout);
        }
        
        // Resetar MFA
        this.mfaConfig = { enabled: false, method: null };
        
        // Reinicializar formulários
        this.initializeForms();
        
        // Aplicar configurações padrão
        const defaultConfig = this.configForm.value;
        this.aplicarConfiguracoes(defaultConfig);
        
        this.isLoading = false;
        this.successMessage = 'Todas as configurações foram restauradas para o padrão!';
        this.showToast('Configurações restauradas com sucesso!', 'success');
        this.clearMessagesAfterDelay();
      }, 1000);
    }
  }
}
