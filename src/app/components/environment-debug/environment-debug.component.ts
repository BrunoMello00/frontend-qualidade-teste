import { Component, OnInit } from '@angular/core';
import { EnvironmentService } from '../../services/environment.service';

@Component({
  selector: 'app-environment-debug',
  template: `
    <div *ngIf="showDebug" class="debug-panel">
      <div class="debug-header" (click)="toggleExpanded()">
        <span class="debug-icon">🌍</span>
        <span>Environment Info</span>
        <span class="debug-status" [class]="connectionStatus.success ? 'success' : 'error'">
          {{ connectionStatus.success ? '✅' : '❌' }}
        </span>
      </div>
      
      <div *ngIf="expanded" class="debug-content">
        <div class="debug-row">
          <strong>API URL:</strong> {{ environmentInfo.apiUrl }}
        </div>
        <div class="debug-row">
          <strong>Environment:</strong> {{ environmentInfo.environment }}
        </div>
        <div class="debug-row">
          <strong>Is Local:</strong> {{ environmentInfo.isLocal ? 'Yes' : 'No' }}
        </div>
        <div class="debug-row">
          <strong>Hostname:</strong> {{ environmentInfo.hostname }}
        </div>
        <div class="debug-row">
          <strong>Connection:</strong> 
          <span [class]="connectionStatus.success ? 'success' : 'error'">
            {{ connectionStatus.message }}
          </span>
        </div>
        <button class="debug-button" (click)="testConnection()">Test Connection</button>
      </div>
    </div>
  `,
  styles: [`
    .debug-panel {
      position: fixed;
      top: 10px;
      right: 10px;
      background: rgba(0, 0, 0, 0.8);
      color: white;
      border-radius: 8px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      z-index: 9999;
      min-width: 300px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    .debug-header {
      padding: 10px;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    }

    .debug-header:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .debug-icon {
      font-size: 16px;
    }

    .debug-status {
      margin-left: auto;
      font-size: 14px;
    }

    .debug-content {
      padding: 10px;
      max-height: 300px;
      overflow-y: auto;
    }

    .debug-row {
      margin-bottom: 8px;
      padding: 4px 0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .debug-row:last-child {
      border-bottom: none;
    }

    .debug-button {
      background: #007acc;
      color: white;
      border: none;
      padding: 6px 12px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 11px;
      margin-top: 10px;
    }

    .debug-button:hover {
      background: #005a9e;
    }

    .success {
      color: #4caf50;
    }

    .error {
      color: #f44336;
    }
  `]
})
export class EnvironmentDebugComponent implements OnInit {
  showDebug = false;
  expanded = false;
  environmentInfo: any = {};
  connectionStatus = { success: false, message: 'Not tested', url: '' };

  constructor(private environmentService: EnvironmentService) {}

  ngOnInit(): void {
    // Temporariamente ocultamos o painel de debug para não poluir a UI.
    // Para reativar, restaurar a lógica original abaixo.
    this.showDebug = false; // <--- debug-panel oculto
    // ...existing code...
    // this.showDebug = !this.environmentService.isProduction();
    // this.environmentInfo = this.environmentService.getEnvironmentInfo();
    // if (this.showDebug) {
    //   this.testConnection();
    // }
  }

  toggleExpanded(): void {
    this.expanded = !this.expanded;
  }

  async testConnection(): Promise<void> {
    this.connectionStatus = await this.environmentService.testConnection();
  }
}