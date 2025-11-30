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
    
    this.sessionTimeoutService.startMonitoring();
    
    this.configForm.get('sessaoTimeout')?.valueChanges.subscribe(timeout => {
      if (timeout) {
        this.sessionTimeoutService.setTimeoutDuration(timeout);
        this.salvarConfiguracaoImediata('sessaoTimeout', timeout);
        this.showToast(`Timeout configurado para ${timeout} minutos`, 'success');
      }
    });
  }

  private initializeForms(): void {
    this.configForm = this.fb.group({
      manterLogado: [false],
      sessaoTimeout: [30]
    });

    this.mfaForm = this.fb.group({
      method: ['', Validators.required],
      email: [''],
      phone: [''],
      verificationCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  private loadConfigurations(): void {
    
    const savedConfig = localStorage.getItem('userConfig');
    if (savedConfig) {
      const config = JSON.parse(savedConfig);
      this.configForm.patchValue(config);
      
      this.aplicarConfiguracoes(config);
    } else {
      const defaultConfig = this.configForm.value;
      this.aplicarConfiguracoes(defaultConfig);
      localStorage.setItem('userConfig', JSON.stringify(defaultConfig));
    }

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

  onConfigSubmit(): void {
    if (this.configForm.valid) {
      this.isLoading = true;
      this.clearMessages();

      setTimeout(() => {
        const config = this.configForm.value;
        
        localStorage.setItem('userConfig', JSON.stringify(config));
        
        this.aplicarConfiguracoes(config);
        
        this.isLoading = false;
        this.successMessage = 'Configurações salvas e aplicadas com sucesso!';
        
        this.clearMessagesAfterDelay();
      }, 1000);
    }
  }

  private aplicarConfiguracoes(config: any): void {
    
    this.sessionTimeoutService.setTimeoutDuration(config.sessaoTimeout);
    this.sessionTimeoutService.startMonitoring();
    
  }

  private salvarConfiguracaoImediata(campo: string, valor: any): void {
    const savedConfig = localStorage.getItem('userConfig');
    let config = savedConfig ? JSON.parse(savedConfig) : this.configForm.value;
    
    config[campo] = valor;
    
    localStorage.setItem('userConfig', JSON.stringify(config));
    
    console.log('💾 Configuração salva automaticamente:', campo, '=', valor);
  }

  private aplicarTema(tema: string): void {
    console.log('🎨 Aplicando tema:', tema);
    const body = document.body;
    
    body.classList.remove('tema-claro', 'tema-escuro', 'tema-auto');
    
    switch (tema) {
      case 'escuro':
        body.classList.add('tema-escuro');
        body.setAttribute('data-bs-theme', 'dark');
        this.showToast('Tema escuro aplicado!', 'success');
        break;
      case 'auto':
        body.classList.add('tema-auto');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        body.setAttribute('data-bs-theme', prefersDark ? 'dark' : 'light');
        this.showToast('Tema automático aplicado!', 'success');
        break;
      default: // 'claro'
        body.classList.add('tema-claro');
        body.setAttribute('data-bs-theme', 'light');
        this.showToast('Tema claro aplicado!', 'success');
        break;
    }
    
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
    
    setTimeout(() => {
      if (toast.parentElement) {
        toast.remove();
      }
    }, 4000);
  }

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

    setTimeout(() => {
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

  resetConfigurations(): void {
    if (confirm('Tem certeza que deseja restaurar as configurações padrão?\n\nIsso irá:\n- Restaurar timeout para 30 minutos\n- Desativar MFA\n- Restaurar todas as outras configurações')) {
      
      this.isLoading = true;
      
      setTimeout(() => {
        localStorage.removeItem('userConfig');
        localStorage.removeItem('mfaConfig');
        
        if ((window as any).sessionTimeout) {
          clearTimeout((window as any).sessionTimeout);
        }
        
        this.mfaConfig = { enabled: false, method: null };
        
        this.initializeForms();
        
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
