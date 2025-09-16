import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { BaseService } from './base.service';
import { MockDataService } from './mock-data.service';
import { EnvironmentService } from './environment.service';

@Injectable({ providedIn: 'root' })
export class DashboardService extends BaseService {
  constructor(
    http: HttpClient,
    environmentService: EnvironmentService,
    private mock: MockDataService,
    private env: EnvironmentService
  ) {
    super(http, environmentService);
  }

  obterEstatisticas(period: 'hoje'|'semana'|'mes' = 'mes'): Observable<any> {
    if (this.env.isLocal()) {
      return this.mock.getDashboardStats(period);
    }
    return this.get('/dashboard/estatisticas', { periodo: period });
  }

  obterVendasSemanais(): Observable<any[]> {
    if (this.env.isLocal()) {
      return this.mock.getVendasSemanais();
    }
    return this.get('/dashboard/vendas-semana');
  }

  obterTopProdutos(limit = 5): Observable<any[]> {
    if (this.env.isLocal()) {
      return this.mock.getTopProdutos(limit);
    }
    return this.get('/dashboard/top-produtos', { limit: String(limit) });
  }

  obterVendasPorCategoria(): Observable<any[]> {
    if (this.env.isLocal()) {
      return this.mock.getVendasPorCategoria();
    }
    return this.get('/dashboard/vendas-por-categoria');
  }
}
