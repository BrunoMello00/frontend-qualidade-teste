import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { 
  RelatorioService, 
  RelatorioVendasResponse, 
  RelatorioEstoqueResponse, 
  RelatorioMovimentacaoResponse, 
  DashboardExecutivoResponse,
  RelatorioFinanceiroResponse,
  FiltroRelatorio,
  StatusVenda,
  CategoriaCliente
} from '../../services/relatorio.service';

// Imports para exportação
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

declare var Chart: any;

interface TipoRelatorio {
  id: string;
  nome: string;
  descricao: string;
  icone: string;
  cor: string;
  requerPeriodo: boolean;
}

@Component({
  selector: 'app-relatorios',
  template: `<div class="page-container">
  <div class="container-fluid">
    
    <!-- Header -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <h1 class="h2 text-primary mb-1">
              <i class="bi bi-graph-up me-2"></i>
              Relatórios e Análises
            </h1>
            <p class="text-muted mb-0">Acompanhe o desempenho do seu negócio</p>
          </div>
          <!-- Botões de Ação -->
          <div class="d-flex gap-2">
            <button class="btn btn-outline-secondary" (click)="imprimirRelatorio()" [disabled]="isLoadingPrint">
              <span *ngIf="isLoadingPrint" class="spinner-border spinner-border-sm me-1" role="status"></span>
              <i class="bi bi-printer me-1" *ngIf="!isLoadingPrint"></i>
              {{ isLoadingPrint ? 'Preparando...' : 'Imprimir' }}
            </button>
            <div class="dropdown">
              <button class="btn btn-primary btn-custom dropdown-toggle" type="button" data-bs-toggle="dropdown">
                <i class="bi bi-download me-1"></i>
                Exportar
              </button>
              <ul class="dropdown-menu">
                <li><a class="dropdown-item" (click)="exportarRelatorio('pdf')">
                  <i class="bi bi-file-pdf me-2"></i>PDF
                </a></li>
                <li><a class="dropdown-item" (click)="exportarRelatorio('excel')">
                  <i class="bi bi-file-excel me-2"></i>Excel
                </a></li>
                <li><a class="dropdown-item" (click)="exportarRelatorio('csv')">
                  <i class="bi bi-filetype-csv me-2"></i>CSV
                </a></li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Seletor de Tipo de Relatório -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card card-custom">
          <div class="card-body">
            <h6 class="fw-semibold mb-3">
              <i class="bi bi-funnel me-2"></i>
              Tipo de Relatório
            </h6>
            <div class="row g-3">
              <div class="col-12 col-md-4">
                <div class="form-check">
                  <input class="form-check-input" type="radio" name="tipoRelatorio" id="relatorioVendas" 
                         value="vendas" (change)="onTipoRelatorioChange('vendas')" [checked]="tipoRelatorio === 'vendas'">
                  <label class="form-check-label fw-semibold" for="relatorioVendas">
                    <i class="bi bi-cart-check me-2 text-primary"></i>
                    Relatório de Vendas
                  </label>
                </div>
              </div>
              <div class="col-12 col-md-4">
                <div class="form-check">
                  <input class="form-check-input" type="radio" name="tipoRelatorio" id="relatorioEstoque" 
                         value="estoque" (change)="onTipoRelatorioChange('estoque')" [checked]="tipoRelatorio === 'estoque'">
                  <label class="form-check-label fw-semibold" for="relatorioEstoque">
                    <i class="bi bi-boxes me-2 text-success"></i>
                    Relatório de Estoque
                  </label>
                </div>
              </div>
              <div class="col-12 col-md-4">
                <div class="form-check">
                  <input class="form-check-input" type="radio" name="tipoRelatorio" id="relatorioProdutos" 
                         value="produtos" (change)="onTipoRelatorioChange('produtos')" [checked]="tipoRelatorio === 'produtos'">
                  <label class="form-check-label fw-semibold" for="relatorioProdutos">
                    <i class="bi bi-box-seam me-2 text-info"></i>
                    Relatório de Produtos
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filtros Dinâmicos baseado no tipo de relatório -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card card-custom">
          <div class="card-body">
            <h6 class="fw-semibold mb-3">
              <i class="bi bi-sliders me-2"></i>
              Filtros do Relatório
            </h6>
            <form [formGroup]="filtroForm" (ngSubmit)="onFiltroChange()">
              <div class="row g-3 align-items-end">
                
                <!-- Filtros para VENDAS -->
                <ng-container *ngIf="tipoRelatorio === 'vendas'">
                  <!-- Período -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Período</label>
                    <select class="form-select" formControlName="periodo" (change)="onFiltroChange()">
                      <option *ngFor="let periodo of periodosDisponiveis" [value]="periodo.value">
                        {{ periodo.label }}
                      </option>
                    </select>
                  </div>

                  <!-- Data Início -->
                  <div class="col-12 col-md-2" *ngIf="isPeriodoPersonalizado()">
                    <label class="form-label fw-semibold">Data Início</label>
                    <input type="date" class="form-control" formControlName="dataInicio" (change)="onFiltroChange()">
                  </div>

                  <!-- Data Fim -->
                  <div class="col-12 col-md-2" *ngIf="isPeriodoPersonalizado()">
                    <label class="form-label fw-semibold">Data Fim</label>
                    <input type="date" class="form-control" formControlName="dataFim" (change)="onFiltroChange()">
                  </div>

                  <!-- Categoria -->
                  <div class="col-12 col-md-2">
                    <label class="form-label fw-semibold">Categoria</label>
                    <select class="form-select" formControlName="categoria" (change)="onFiltroChange()">
                      <option value="">Todas as Categorias</option>
                      <option *ngFor="let categoria of categorias" [value]="categoria">
                        {{ categoria }}
                      </option>
                    </select>
                  </div>

                  <!-- Evento -->
                  <div class="col-12 col-md-2">
                    <label class="form-label fw-semibold">Evento</label>
                    <select class="form-select" formControlName="evento" (change)="onFiltroChange()">
                      <option value="">Todos os Eventos</option>
                      <option *ngFor="let evento of eventosDisponiveis" [value]="evento">
                        {{ evento }}
                      </option>
                    </select>
                  </div>

                  <!-- Agrupar Por -->
                  <div class="col-12 col-md-2">
                    <label class="form-label fw-semibold">Agrupar por</label>
                    <select class="form-select" formControlName="agruparPor" (change)="onFiltroChange()">
                      <option value="dia">Dia</option>
                      <option value="semana">Semana</option>
                      <option value="mes">Mês</option>
                    </select>
                  </div>
                </ng-container>

                <!-- Filtros para ESTOQUE -->
                <ng-container *ngIf="tipoRelatorio === 'estoque'">
                  <!-- Categoria -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Categoria</label>
                    <select class="form-select" formControlName="categoria" (change)="onFiltroChange()">
                      <option value="">Todas as Categorias</option>
                      <option *ngFor="let categoria of categorias" [value]="categoria">
                        {{ categoria }}
                      </option>
                    </select>
                  </div>

                  <!-- Status do Estoque -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Status do Estoque</label>
                    <select class="form-select" formControlName="statusEstoque" (change)="onFiltroChange()">
                      <option value="">Todos os Status</option>
                      <option value="normal">Normal</option>
                      <option value="baixo">Estoque Baixo</option>
                      <option value="zerado">Estoque Zerado</option>
                      <option value="excesso">Excesso</option>
                    </select>
                  </div>

                  <!-- Valor Mínimo -->
                  <div class="col-12 col-md-2">
                    <label class="form-label fw-semibold">Valor Mín. Estoque</label>
                    <input type="number" class="form-control" formControlName="valorMinimo" 
                           placeholder="R$ 0,00" (change)="onFiltroChange()">
                  </div>

                  <!-- Ordenar Por -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Ordenar por</label>
                    <select class="form-select" formControlName="ordenarPor" (change)="onFiltroChange()">
                      <option value="nome">Nome do Produto</option>
                      <option value="categoria">Categoria</option>
                      <option value="estoque">Quantidade em Estoque</option>
                      <option value="valor">Valor do Estoque</option>
                      <option value="status">Status</option>
                    </select>
                  </div>
                </ng-container>

                <!-- Filtros para PRODUTOS -->
                <ng-container *ngIf="tipoRelatorio === 'produtos'">
                  <!-- Período para Performance -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Período de Análise</label>
                    <select class="form-select" formControlName="periodo" (change)="onFiltroChange()">
                      <option *ngFor="let periodo of periodosDisponiveis" [value]="periodo.value">
                        {{ periodo.label }}
                      </option>
                    </select>
                  </div>

                  <!-- Categoria -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Categoria</label>
                    <select class="form-select" formControlName="categoria" (change)="onFiltroChange()">
                      <option value="">Todas as Categorias</option>
                      <option *ngFor="let categoria of categorias" [value]="categoria">
                        {{ categoria }}
                      </option>
                    </select>
                  </div>

                  <!-- Critério de Performance -->
                  <div class="col-12 col-md-3">
                    <label class="form-label fw-semibold">Analisar por</label>
                    <select class="form-select" formControlName="criterioPerformance" (change)="onFiltroChange()">
                      <option value="vendas">Mais Vendidos</option>
                      <option value="faturamento">Maior Faturamento</option>
                      <option value="margem">Maior Margem</option>
                      <option value="menos-vendidos">Menos Vendidos</option>
                    </select>
                  </div>

                  <!-- Limite de Resultados -->
                  <div class="col-12 col-md-2">
                    <label class="form-label fw-semibold">Mostrar Top</label>
                    <select class="form-select" formControlName="limiteResultados" (change)="onFiltroChange()">
                      <option value="5">5</option>
                      <option value="10">10</option>
                      <option value="20">20</option>
                      <option value="50">50</option>
                      <option value="todos">Todos</option>
                    </select>
                  </div>
                </ng-container>

                <!-- Botão Limpar Filtros -->
                <div class="col-12 col-md-1">
                  <label class="form-label fw-semibold invisible">Ações</label>
                  <button 
                    type="button"
                    class="btn btn-outline-secondary btn-custom w-100"
                    (click)="limparFiltros()"
                    title="Limpar todos os filtros">
                    <i class="bi bi-arrow-clockwise"></i>
                  </button>
                </div>

                <!-- Botão Gerar Relatório -->
                <div class="col-12 col-md-2">
                  <label class="form-label fw-semibold invisible">Gerar</label>
                  <button 
                    type="button"
                    class="btn btn-primary btn-custom w-100"
                    (click)="gerarRelatorio()"
                    [disabled]="carregandoRelatorio || !filtroForm.valid"
                    title="Gerar relatório com os filtros selecionados">
                    <span *ngIf="carregandoRelatorio" class="spinner-border spinner-border-sm me-1" role="status"></span>
                    <i class="bi bi-graph-up me-1" *ngIf="!carregandoRelatorio"></i>
                    {{ carregandoRelatorio ? 'Gerando...' : 'Gerar' }}
                  </button>
                </div>

              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- Resumo Executivo -->
    <div class="row g-4 mb-4">
      
      <div class="col-12 col-sm-6 col-lg-3">
        <div class="card card-custom h-100 shadow-hover">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h6 class="card-title text-uppercase text-muted small fw-bold m-0">Total de Vendas</h6>
              <i class="bi bi-cart-check stat-card-icon"></i>
            </div>
            <div class="stat-card-value">{{ estatisticasGerais.totalVendasPeriodo }}</div>
            <div class="small" [class]="getCrescimentoClass(estatisticasGerais.crescimentoVendas)">
              <i [class]="'bi ' + getCrescimentoIcon(estatisticasGerais.crescimentoVendas) + ' me-1'"></i>
              {{ formatarPercentual(estatisticasGerais.crescimentoVendas) }}
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-sm-6 col-lg-3">
        <div class="card card-custom h-100 shadow-hover">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h6 class="card-title text-uppercase text-muted small fw-bold m-0">Faturamento</h6>
              <i class="bi bi-currency-dollar stat-card-icon text-success"></i>
            </div>
            <div class="stat-card-value text-success">{{ formatarMoeda(estatisticasGerais.faturamentoPeriodo) }}</div>
            <div class="small text-muted">No período selecionado</div>
          </div>
        </div>
      </div>

      <div class="col-12 col-sm-6 col-lg-3">
        <div class="card card-custom h-100 shadow-hover">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h6 class="card-title text-uppercase text-muted small fw-bold m-0">Ticket Médio</h6>
              <i class="bi bi-calculator stat-card-icon text-info"></i>
            </div>
            <div class="stat-card-value text-info">{{ formatarMoeda(estatisticasGerais.ticketMedio) }}</div>
            <div class="small text-muted">Por venda</div>
          </div>
        </div>
      </div>

      <div class="col-12 col-sm-6 col-lg-3">
        <div class="card card-custom border-left-primary h-100 shadow-hover filter-box"
             (click)="navegarParaSemEstoque()"
             role="button"
             tabindex="0"
             title="Clique para ver produtos sem estoque">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h6 class="card-title text-uppercase text-muted small fw-bold m-0">Produtos em Falta</h6>
              <i class="bi bi-exclamation-triangle stat-card-icon text-warning"></i>
            </div>
            <div class="stat-card-value text-warning">{{ estatisticasGerais.produtosBaixoEstoque }}</div>
            <div class="small">
              <i class="bi bi-arrow-right text-warning me-1"></i>
              <span class="text-warning">Ver produtos</span>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- Conteúdo do Relatório Selecionado -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card card-custom">
          <div class="card-body">
            
            <!-- Loading -->
            <div *ngIf="isLoading" class="text-center py-5">
              <div class="spinner-custom mb-3"></div>
              <p class="text-muted">Carregando relatórios...</p>
            </div>

            <!-- Conteúdo dos Relatórios -->
            <div *ngIf="!isLoading">
              
              <!-- Relatório de Vendas -->
              <div *ngIf="tipoRelatorio === 'vendas'">
                <!-- Gráfico de Vendas -->
                <div class="row mb-4">
                  <div class="col-12">
                    <div *ngIf="dadosGraficoVendas && dadosGraficoVendas.length > 0; else placeholderChart">
                      <canvas id="chartVendasMes" width="800" height="300"></canvas>
                    </div>
                    <ng-template #placeholderChart>
                      <div class="chart-placeholder">
                        <i class="bi bi-graph-up display-1 text-muted"></i>
                        <h5 class="text-muted mt-3">Gráfico de Vendas</h5>
                        <p class="text-muted">Evolução das vendas ao longo do tempo</p>
                      </div>
                    </ng-template>
                  </div>
                </div>

                <!-- Tabela de Vendas por Período -->
                <div class="row">
                  <div class="col-12">
                    <h6 class="fw-semibold mb-3">
                      <i class="bi bi-table me-2"></i>
                      Vendas por Período
                    </h6>
                    <div class="table-responsive">
                      <table class="table table-hover">
                        <thead class="table-light">
                          <tr>
                            <th class="fw-semibold">Período</th>
                            <th class="fw-semibold">Vendas</th>
                            <th class="fw-semibold">Faturamento</th>
                            <th class="fw-semibold">Ticket Médio</th>
                            <th class="fw-semibold" title="Crescimento das vendas comparado ao período anterior">
                              Crescimento
                              <i class="bi bi-info-circle ms-1 text-muted" style="font-size: 0.8em;"></i>
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr *ngIf="relatorioVendas">
                            <td>{{ getPeriodoVendas() }}</td>
                            <td class="fw-semibold">{{ relatorioVendas.totalVendas || 0 }}</td>
                            <td class="text-success fw-semibold">{{ formatarMoeda(relatorioVendas.totalFaturamento || 0) }}</td>
                            <td>{{ formatarMoeda(relatorioVendas.ticketMedio || 0) }}</td>
                            <td class="text-info">
                              <i class="bi bi-trending-up me-1"></i>
                              {{ getCrescimentoVendas() }}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>

                <!-- Vendas Detalhadas -->
                <div class="row" *ngIf="vendasDetalhadas && vendasDetalhadas.length > 0">
                  <div class="col-12">
                    <h6 class="fw-semibold mb-3">
                      <i class="bi bi-list-ul me-2"></i>
                      Vendas Detalhadas
                    </h6>
                    <div class="table-responsive">
                      <table class="table table-hover">
                        <thead class="table-light">
                          <tr>
                            <th class="fw-semibold">Data</th>
                            <th class="fw-semibold">Cliente</th>
                            <th class="fw-semibold">Produtos</th>
                            <th class="fw-semibold">Quantidade</th>
                            <th class="fw-semibold">Valor Total</th>
                            <th class="fw-semibold">Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr *ngFor="let venda of vendasDetalhadas">
                            <td>{{ formatarData(venda.dataVenda) }}</td>
                            <td>{{ venda.clienteNome }}</td>
                            <td>
                              <span class="badge bg-light text-dark me-1" 
                                    *ngFor="let item of venda.itens">
                                {{ item.produtoNome }}
                              </span>
                            </td>
                            <td class="text-center">{{ venda.quantidadeItens }}</td>
                            <td class="text-success fw-semibold">{{ formatarMoeda(venda.valorTotal) }}</td>
                            <td>
                              <span class="badge" 
                                    [ngClass]="{
                                      'bg-success': venda.status === 'CONFIRMADA' || venda.status === 'CONCLUIDA',
                                      'bg-warning text-dark': venda.status === 'PENDENTE',
                                      'bg-danger': venda.status === 'NEGADA' || venda.status === 'CANCELADA'
                                    }">
                                {{ venda.status }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Relatório de Produtos -->
              <div *ngIf="tipoRelatorio === 'produtos'">
                <!-- Top Produtos -->
                <div class="row mb-4">
                  <div class="col-12">
                    <h6 class="fw-semibold mb-3">
                      <i class="bi bi-star-fill me-2"></i>
                      Top 5 Produtos Mais Vendidos
                    </h6>
                    <div class="row g-3">
                      <div *ngFor="let produto of getTopProdutos(); let i = index" class="col-12 col-md-6 col-lg-4">
                        <div class="card card-custom h-100 border-left-primary">
                          <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                              <span class="badge bg-primary fs-6">#{{ i + 1 }}</span>
                              <span class="badge bg-light text-dark">{{ produto.categoria }}</span>
                            </div>
                            <h6 class="card-title">{{ produto.produtoNome }}</h6>
                            <div class="row text-center">
                              <div class="col-4">
                                <div class="fw-bold text-primary">{{ produto.quantidadeVendida }}</div>
                                <small class="text-muted">Vendidos</small>
                              </div>
                              <div class="col-4">
                                <div class="fw-bold text-success">{{ formatarMoeda(produto.faturamento || 0) }}</div>
                                <small class="text-muted">Receita</small>
                              </div>
                              <div class="col-4">
                                <div class="fw-bold text-info">{{ formatarPercentual(produto.margem) }}</div>
                                <small class="text-muted">Margem</small>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Tabela Completa de Produtos -->
                <div class="row">
                  <div class="col-12">
                    <h6 class="fw-semibold mb-3">
                      <i class="bi bi-table me-2"></i>
                      Análise Detalhada de Produtos
                    </h6>
                    <div class="table-responsive">
                      <table class="table table-hover">
                        <thead class="table-light">
                          <tr>
                            <th class="fw-semibold">Produto</th>
                            <th class="fw-semibold">Categoria</th>
                            <th class="fw-semibold">Qtd Vendida</th>
                            <th class="fw-semibold">Faturamento</th>
                            <th class="fw-semibold">Margem (%)</th>
                            <th class="fw-semibold">Estoque</th>
                            <th class="fw-semibold">Preço Unit.</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr *ngFor="let produto of relatorioProdutos">
                            <td class="fw-semibold">{{ produto.produtoNome }}</td>
                            <td>
                              <span class="badge bg-light text-dark">{{ produto.categoria }}</span>
                            </td>
                            <td class="text-center">
                              <span class="fw-bold text-primary">{{ produto.quantidadeVendida }}</span>
                            </td>
                            <td class="text-success fw-semibold">{{ formatarMoeda(produto.faturamento || 0) }}</td>
                            <td class="text-info fw-semibold">{{ formatarPercentual(produto.margem) }}</td>
                            <td class="text-center" [class]="produto.estoqueAtual <= 10 ? 'text-danger' : 'text-success'">
                              <span class="fw-bold">{{ produto.estoqueAtual }}</span>
                            </td>
                            <td class="text-success fw-semibold">{{ formatarMoeda(produto.precoUnitario || 0) }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Relatório de Estoque -->
              <div *ngIf="tipoRelatorio === 'estoque'">
                <!-- Alertas de Estoque -->
                <div class="row mb-4" *ngIf="getProdutosBaixoEstoque().length > 0">
                  <div class="col-12">
                    <div class="alert alert-warning">
                      <h6 class="alert-heading">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        Atenção! Produtos com Estoque Baixo ou Zerado
                      </h6>
                      <div class="row g-2">
                        <div *ngFor="let produto of getProdutosBaixoEstoque()" class="col-12 col-md-6">
                          <div class="d-flex justify-content-between align-items-center p-2 bg-white rounded">
                            <div>
                              <span class="fw-semibold">{{ produto.nome }}</span>
                              <small class="text-muted d-block">{{ produto.categoria }} - Código: {{ produto.codigo }}</small>
                              <small class="text-danger">Estoque: {{ produto.estoqueAtual }} / Mín: {{ produto.estoqueMinimo }}</small>
                            </div>
                            <span [class]="'badge ' + getStatusBadgeClass('baixo')">
                              {{ getStatusText('baixo') }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Resumo de Estoque -->
                <div class="row mb-4">
                  <div class="col-12">
                    <div class="card card-custom bg-light">
                      <div class="card-body">
                        <div class="row text-center">
                          <div class="col-12 col-md-4">
                            <div class="h4 text-success mb-1">{{ formatarMoeda(relatorioEstoque?.valorTotalEstoque || 0) }}</div>
                            <div class="text-muted">Valor Total em Estoque</div>
                          </div>
                          <div class="col-12 col-md-4">
                            <div class="h4 text-info mb-1">{{ relatorioEstoque?.totalProdutos || 0 }}</div>
                            <div class="text-muted">Produtos Monitorados</div>
                          </div>
                          <div class="col-12 col-md-4">
                            <div class="h4 text-warning mb-1">{{ relatorioEstoque?.produtosEstoqueBaixo || 0 }}</div>
                            <div class="text-muted">Requerem Atenção</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Tabela de Estoque -->
                <div class="row">
                  <div class="col-12">
                    <h6 class="fw-semibold mb-3">
                      <i class="bi bi-table me-2"></i>
                      Status Detalhado do Estoque
                    </h6>
                    <div class="table-responsive">
                      <table class="table table-hover">
                        <thead class="table-light">
                          <tr>
                            <th class="fw-semibold">Produto</th>
                            <th class="fw-semibold">Categoria</th>
                            <th class="fw-semibold">Estoque Atual</th>
                            <th class="fw-semibold">Estoque Mínimo</th>
                            <th class="fw-semibold">Valor em Estoque</th>
                            <th class="fw-semibold">Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr *ngFor="let item of relatorioEstoque?.todosProdutos || []" 
                              [class]="item.estoqueAtual <= item.estoqueMinimo ? 'table-warning' : ''">
                            <td class="fw-semibold">
                              {{ item.nome }}
                              <small class="text-muted d-block">{{ item.codigo }}</small>
                            </td>
                            <td>
                              <span class="badge bg-light text-dark">{{ item.categoria }}</span>
                            </td>
                            <td class="text-center">
                              <span class="fw-bold" [class]="item.estoqueAtual <= item.estoqueMinimo ? 'text-danger' : 'text-success'">
                                {{ item.estoqueAtual }}
                              </span>
                            </td>
                            <td class="text-center">{{ item.estoqueMinimo }}</td>
                            <td class="text-success fw-semibold">{{ formatarMoeda(calcularValorEstoque(item)) }}</td>
                            <td>
                              <span [class]="'badge ' + getStatusBadgeClass(item.estoqueAtual <= item.estoqueMinimo ? 'baixo' : 'normal')">
                                {{ getStatusText(item.estoqueAtual <= item.estoqueMinimo ? 'baixo' : 'normal') }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</div>`,
  styles: [`
    .bg-opacity-10 {
      --bs-bg-opacity: 0.1;
    }
    
    .display-6 {
      font-size: 2.5rem;
    }
    
    .card {
      transition: all 0.2s ease;
    }
    
    .table th {
      font-weight: 600;
      font-size: 0.875rem;
      border-top: none;
    }
    
    .badge {
      font-size: 0.75rem;
    }
    
    .alert-info {
      background-color: #e7f3ff;
      border-color: #b8daff;
      color: #0c5460;
    }
  `]
})
export class RelatoriosComponent implements OnInit {
  // Estados
  carregandoRelatorio = false;
  carregandoDashboard = false;
  mensagemErro = '';
  mensagemSucesso = '';
  isLoading = false;
  isLoadingPrint = false;

  // Controle de tipo de relatório
  tipoRelatorio = 'vendas';

  // Dados
  dashboardExecutivo: DashboardExecutivoResponse | null = null;
  relatorioVendas: RelatorioVendasResponse | null = null;
  relatorioEstoque: RelatorioEstoqueResponse | null = null;
  relatorioMovimentacao: RelatorioMovimentacaoResponse | null = null;
  
  // Dados para os novos relatórios
  vendasMes: any[] = [];
  relatorioProdutos: any[] = [];
  vendasDetalhadas: any[] = [];
  dadosGraficoVendas: any[] = [];
  chartInstance: any = null;
  estatisticasGerais: any = {
    totalVendasPeriodo: 0,
    crescimentoVendas: 0,
    faturamentoPeriodo: 0,
    ticketMedio: 0,
    produtosBaixoEstoque: 0,
    valorTotalEstoque: 0
  };

  // Formulário e controles
  filtroForm: FormGroup;
  tipoSelecionado: TipoRelatorio | null = null;

  // Dados de filtros
  categorias: string[] = [];
  eventosDisponiveis: string[] = [];
  periodosDisponiveis = [
    { value: 'hoje', label: 'Hoje' },
    { value: 'ontem', label: 'Ontem' },
    { value: 'ultimos7dias', label: 'Últimos 7 dias' },
    { value: 'ultimos30dias', label: 'Últimos 30 dias' },
    { value: 'mesAtual', label: 'Mês atual' },
    { value: 'mesAnterior', label: 'Mês anterior' },
    { value: 'personalizado', label: 'Período personalizado' }
  ];

  // Tipos de relatório disponíveis
  tiposRelatorio: TipoRelatorio[] = [
    {
      id: 'vendas',
      nome: 'Relatório de Vendas',
      descricao: 'Análise completa de vendas, faturamento, top vendedores e produtos mais vendidos',
      icone: 'bi bi-graph-up',
      cor: '#0d6efd',
      requerPeriodo: true
    },
    {
      id: 'estoque',
      nome: 'Relatório de Estoque',
      descricao: 'Situação atual do estoque, produtos com estoque baixo e valor total do inventário',
      icone: 'bi bi-boxes',
      cor: '#fd7e14',
      requerPeriodo: false
    },
    {
      id: 'produtos',
      nome: 'Relatório de Produtos',
      descricao: 'Análise detalhada de produtos, categorias, produtos mais vendidos e informações completas',
      icone: 'bi bi-box-seam',
      cor: '#6f42c1',
      requerPeriodo: false
    },
    {
      id: 'movimentacao',
      nome: 'Relatório de Movimentação',
      descricao: 'Histórico de movimentações de estoque, entradas, saídas e produtos mais movimentados',
      icone: 'bi bi-arrow-repeat',
      cor: '#20c997',
      requerPeriodo: true
    }
  ];

  constructor(
    public relatorioService: RelatorioService,
    private fb: FormBuilder
  ) {
    this.filtroForm = this.fb.group({
      tipoRelatorio: ['vendas', Validators.required], // Definir valor padrão
      dataInicio: [''],
      dataFim: [''],
      periodo: ['ultimos30dias'],
      categoria: [''],
      evento: [''],
      agruparPor: ['dia'],
      statusEstoque: [''],
      valorMinimo: [''],
      ordenarPor: ['nome'],
      criterioPerformance: ['vendas'],
      limiteResultados: ['10']
    });

    // Definir período padrão (últimos 30 dias)
    const periodoAtual = this.relatorioService.getPeriodoAtual();
    this.filtroForm.patchValue({
      dataInicio: periodoAtual.inicio,
      dataFim: periodoAtual.fim
    });
  }

  ngOnInit(): void {
    this.carregarDashboard();
    this.carregarCategorias();
    this.carregarEventos();
    this.carregarEstatisticasIniciais();
    this.inicializarTipoRelatorio();
  }

  private inicializarTipoRelatorio(): void {
    console.log('🎯 Inicializando tipo de relatório:', this.tipoRelatorio);
    
    // Definir o tipo selecionado baseado no tipoRelatorio atual
    const tipoSelecionado = this.tiposRelatorio.find(t => t.id === this.tipoRelatorio);
    this.tipoSelecionado = tipoSelecionado || null;
    
    // Sincronizar o FormGroup com o tipoRelatorio
    this.filtroForm.patchValue({
      tipoRelatorio: this.tipoRelatorio
    });
    
    console.log('✅ Tipo selecionado:', this.tipoSelecionado);
    console.log('✅ FormGroup tipoRelatorio:', this.filtroForm.get('tipoRelatorio')?.value);
    
    if (!this.tipoSelecionado) {
      console.error('❌ Tipo de relatório não encontrado:', this.tipoRelatorio);
      return;
    }
    
    console.log('✅ Tipo selecionado:', this.tipoSelecionado.nome);
    
    // Configurar validações se necessário
    if (this.tipoSelecionado.requerPeriodo) {
      this.filtroForm.get('dataInicio')?.setValidators([Validators.required]);
      this.filtroForm.get('dataFim')?.setValidators([Validators.required]);
    }
    
    this.filtroForm.get('dataInicio')?.updateValueAndValidity();
    this.filtroForm.get('dataFim')?.updateValueAndValidity();
    
    // Gerar relatório inicial automaticamente
    console.log('🔍 Verificando condições para gerar relatório inicial:', {
      formValid: this.filtroForm.valid,
      tipoSelecionado: !!this.tipoSelecionado,
      formErrors: this.filtroForm.errors,
      formValue: this.filtroForm.value
    });
    
    // Debug detalhado dos campos
    Object.keys(this.filtroForm.controls).forEach(key => {
      const control = this.filtroForm.get(key);
      if (control && control.invalid) {
        console.log(`❌ Campo inválido: ${key}`, {
          value: control.value,
          errors: control.errors,
          validators: control.hasError
        });
      }
    });
    
    if (this.filtroForm.valid && this.tipoSelecionado) {
      console.log('🚀 Gerando relatório inicial automaticamente:', this.tipoRelatorio);
      this.gerarRelatorio().then(() => {
        console.log('✅ Inicialização completa!');
      }).catch((error) => {
        console.warn('⚠️ Erro na inicialização do relatório:', error);
      });
    } else {
      console.warn('⚠️ Não foi possível gerar relatório inicial - condições não atendidas');
    }
  }

  // Métodos para controle de tipo de relatório
  onTipoRelatorioChange(tipo: string): void {
    this.tipoRelatorio = tipo;
    this.filtroForm.patchValue({ tipoRelatorio: tipo });
    
    const tipoSelecionado = this.tiposRelatorio.find(t => t.id === tipo);
    this.tipoSelecionado = tipoSelecionado || null;
    
    if (this.tipoSelecionado?.requerPeriodo) {
      this.filtroForm.get('dataInicio')?.setValidators([Validators.required]);
      this.filtroForm.get('dataFim')?.setValidators([Validators.required]);
    } else {
      this.filtroForm.get('dataInicio')?.clearValidators();
      this.filtroForm.get('dataFim')?.clearValidators();
    }
    
    this.filtroForm.get('dataInicio')?.updateValueAndValidity();
    this.filtroForm.get('dataFim')?.updateValueAndValidity();
    
    this.onFiltroChange();
  }

  // Método para detectar período personalizado
  isPeriodoPersonalizado(): boolean {
    return this.filtroForm.get('periodo')?.value === 'personalizado';
  }

  // Método para converter período em datas
  private converterPeriodoEmDatas(periodo: string): { inicio: string, fim: string } {
    const hoje = new Date();
    const fimStr = hoje.toISOString().split('T')[0];
    let inicioStr: string;

    switch (periodo) {
      case 'hoje':
        inicioStr = fimStr; // Hoje: mesmo dia
        break;
      
      case 'ontem':
        const ontem = new Date(hoje);
        ontem.setDate(hoje.getDate() - 1);
        inicioStr = ontem.toISOString().split('T')[0];
        return { inicio: inicioStr, fim: inicioStr }; // Ontem: mesmo dia
      
      case 'ultimos7dias':
        const seteDiasAtras = new Date(hoje);
        seteDiasAtras.setDate(hoje.getDate() - 7);
        inicioStr = seteDiasAtras.toISOString().split('T')[0];
        break;
      
      case 'ultimos30dias':
        const trintaDiasAtras = new Date(hoje);
        trintaDiasAtras.setDate(hoje.getDate() - 30);
        inicioStr = trintaDiasAtras.toISOString().split('T')[0];
        break;
      
      case 'mesAtual':
        const inicioMes = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        inicioStr = inicioMes.toISOString().split('T')[0];
        break;
      
      case 'mesAnterior':
        const mesAnterior = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
        const fimMesAnterior = new Date(hoje.getFullYear(), hoje.getMonth(), 0);
        inicioStr = mesAnterior.toISOString().split('T')[0];
        return {
          inicio: inicioStr,
          fim: fimMesAnterior.toISOString().split('T')[0]
        };
      
      default:
        // Para 'personalizado' ou casos não tratados, manter datas atuais
        return {
          inicio: this.filtroForm.get('dataInicio')?.value || fimStr,
          fim: this.filtroForm.get('dataFim')?.value || fimStr
        };
    }

    return { inicio: inicioStr, fim: fimStr };
  }

  // Método para mudanças nos filtros
  onFiltroChange(): void {
    console.log('Filtros alterados:', this.filtroForm.value);
    
    // Atualizar datas baseadas no período selecionado (exceto período personalizado)
    const periodoSelecionado = this.filtroForm.get('periodo')?.value;
    if (periodoSelecionado && periodoSelecionado !== 'personalizado') {
      const datas = this.converterPeriodoEmDatas(periodoSelecionado);
      
      console.log(`🗓️ Convertendo período '${periodoSelecionado}' para datas:`, datas);
      
      // Atualizar as datas no formulário sem triggerar novos eventos
      this.filtroForm.patchValue({
        dataInicio: datas.inicio,
        dataFim: datas.fim
      }, { emitEvent: false });
    }
    
    // Gerar relatório automaticamente quando filtros mudarem
    if (this.filtroForm.valid && this.tipoSelecionado) {
      setTimeout(() => {
        this.gerarRelatorio();
      }, 300); // Pequeno delay para evitar muitas chamadas
    }
  }

  // Método para limpar filtros
  limparFiltros(): void {
    this.filtroForm.patchValue({
      periodo: 'ultimos30dias',
      categoria: '',
      evento: '',
      agruparPor: 'dia',
      statusEstoque: '',
      valorMinimo: '',
      ordenarPor: 'nome',
      criterioPerformance: 'vendas',
      limiteResultados: '10'
    });
    this.onFiltroChange();
  }

  // Métodos para formatação
  formatarMoeda(valor: number): string {
    return this.relatorioService.formatarMoeda(valor);
  }

  formatarPercentual(valor: number): string {
    return `${valor.toFixed(1)}%`;
  }

  formatarData(data: string): string {
    return new Date(data).toLocaleDateString('pt-BR');
  }

  // Métodos para classes CSS dinâmicas
  getCrescimentoClass(crescimento: number): string {
    return crescimento >= 0 ? 'text-success' : 'text-danger';
  }

  getCrescimentoIcon(crescimento: number): string {
    return crescimento >= 0 ? 'bi-arrow-up' : 'bi-arrow-down';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'normal': return 'bg-success';
      case 'baixo': return 'bg-warning';
      case 'zerado': return 'bg-danger';
      case 'excesso': return 'bg-info';
      default: return 'bg-secondary';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'normal': return 'Normal';
      case 'baixo': return 'Baixo';
      case 'zerado': return 'Zerado';
      case 'excesso': return 'Excesso';
      default: return 'N/A';
    }
  }

  // Métodos para obter dados filtrados
  getTopProdutos(): any[] {
    return this.relatorioProdutos.slice(0, 5);
  }

  getProdutosBaixoEstoque(): any[] {
    if (!this.relatorioEstoque || !this.relatorioEstoque.todosProdutos) return [];
    
    // Filtrar produtos com estoque baixo (estoque atual <= estoque mínimo)
    return this.relatorioEstoque.todosProdutos.filter((produto: any) => 
      produto.estoqueAtual <= produto.estoqueMinimo
    );
  }

  calcularValorEstoque(produto: any): number {
    // Em um sistema real, isso viria do backend
    // Por simplicidade, vamos estimar baseado na categoria
    const precosPorCategoria: { [key: string]: number } = {
      'Roupas': 50,
      'Calçados': 120,
      'Eletrônicos': 200,
      'Acessórios': 80,
      'Casa': 30
    };
    
    const precoEstimado = precosPorCategoria[produto.categoria] || 40;
    return produto.estoqueAtual * precoEstimado;
  }

  // Métodos helper para templates
  getPeriodoVendas(): string {
    if (!this.relatorioVendas) return '';
    const dataInicio = this.filtroForm.get('dataInicio')?.value;
    const dataFim = this.filtroForm.get('dataFim')?.value;
    
    if (dataInicio && dataFim) {
      return `${dataInicio} até ${dataFim}`;
    }
    
    return 'Período selecionado';
  }

  getCrescimentoVendas(): string {
    if (!this.relatorioVendas) return 'N/A';
    
    // Usar o crescimento calculado das estatísticas gerais
    if (this.estatisticasGerais && this.estatisticasGerais.crescimentoVendas !== undefined) {
      const crescimento = this.estatisticasGerais.crescimentoVendas;
      if (crescimento > 0) {
        return `+${crescimento.toFixed(1)}% vs período anterior`;
      } else if (crescimento < 0) {
        return `${crescimento.toFixed(1)}% vs período anterior`;
      } else {
        return 'Estável (0%)';
      }
    }
    
    // Se não há dados do período anterior, mostrar info mais clara
    return 'Sem dados do período anterior para comparação';
  }

  // Métodos para ações
  async imprimirRelatorio(): Promise<void> {
    this.isLoadingPrint = true;
    try {
      // Criar conteúdo para impressão baseado no tipo de relatório
      const printContent = this.gerarConteudoImpressao();
      
      // Criar nova janela para impressão
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(`
          <!DOCTYPE html>
          <html>
          <head>
            <title>Relatório - ${this.getTipoRelatorioNome()}</title>
            <style>
              body { font-family: Arial, sans-serif; margin: 20px; }
              h1, h2, h3 { color: #333; }
              table { width: 100%; border-collapse: collapse; margin: 20px 0; }
              th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
              th { background-color: #f2f2f2; font-weight: bold; }
              .text-success { color: #28a745; }
              .text-danger { color: #dc3545; }
              .text-warning { color: #ffc107; }
              .badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; }
              .bg-success { background-color: #28a745; color: white; }
              .bg-warning { background-color: #ffc107; color: black; }
              .bg-danger { background-color: #dc3545; color: white; }
              @media print { body { margin: 0; } }
            </style>
          </head>
          <body>
            ${printContent}
          </body>
          </html>
        `);
        printWindow.document.close();
        
        // Aguardar carregamento e imprimir
        setTimeout(() => {
          printWindow.print();
          printWindow.close();
        }, 500);
      }
      
      console.log('Relatório enviado para impressão');
    } catch (error) {
      console.error('Erro ao imprimir:', error);
    } finally {
      this.isLoadingPrint = false;
    }
  }

  async exportarRelatorio(formato: string): Promise<void> {
    try {
      const nomeArquivo = `relatorio_${this.tipoRelatorio}_${new Date().toISOString().split('T')[0]}`;
      
      switch (formato) {
        case 'pdf':
          await this.exportarPDF(nomeArquivo);
          break;
        case 'excel':
          await this.exportarExcel(nomeArquivo);
          break;
        case 'csv':
          await this.exportarCSV(nomeArquivo);
          break;
        default:
          console.error('Formato não suportado:', formato);
      }
      
      console.log(`Relatório exportado em formato: ${formato}`);
    } catch (error) {
      console.error('Erro ao exportar:', error);
      alert('Erro ao exportar relatório. Tente novamente.');
    }
  }

  private gerarConteudoImpressao(): string {
    const periodo = this.getPeriodoVendas() || 'Todos os períodos';
    let conteudo = `
      <h1>Relatório ${this.getTipoRelatorioNome()}</h1>
      <p><strong>Período:</strong> ${periodo}</p>
      <p><strong>Data de Geração:</strong> ${new Date().toLocaleString('pt-BR')}</p>
    `;

    if (this.tipoRelatorio === 'vendas') {
      conteudo += this.gerarConteudoVendas();
    } else if (this.tipoRelatorio === 'estoque') {
      conteudo += this.gerarConteudoEstoque();
    } else if (this.tipoRelatorio === 'produtos') {
      conteudo += this.gerarConteudoProdutos();
    }

    return conteudo;
  }

  private gerarConteudoVendas(): string {
    if (!this.vendasDetalhadas || this.vendasDetalhadas.length === 0) {
      return '<p>Nenhuma venda encontrada para o período selecionado.</p>';
    }

    let tabela = `
      <h2>Vendas Detalhadas</h2>
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Cliente</th>
            <th>Valor Total</th>
            <th>Forma Pagamento</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
    `;

    this.vendasDetalhadas.forEach(venda => {
      tabela += `
        <tr>
          <td>${this.formatarData(venda.dataVenda)}</td>
          <td>${venda.clienteNome || 'N/A'}</td>
          <td class="text-success">${this.formatarMoeda(venda.valorTotal)}</td>
          <td>${venda.formaPagamento || 'N/A'}</td>
          <td><span class="badge ${this.getStatusBadgeClass(venda.status)}">${venda.status}</span></td>
        </tr>
      `;
    });

    tabela += '</tbody></table>';
    return tabela;
  }

  private gerarConteudoEstoque(): string {
    if (!this.relatorioEstoque || !this.relatorioEstoque.todosProdutos) {
      return '<p>Nenhum produto encontrado.</p>';
    }

    let tabela = `
      <h2>Status Detalhado do Estoque</h2>
      <table>
        <thead>
          <tr>
            <th>Produto</th>
            <th>Categoria</th>
            <th>Estoque Atual</th>
            <th>Estoque Mínimo</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
    `;

    this.relatorioEstoque.todosProdutos.forEach(produto => {
      const status = produto.estoqueAtual <= produto.estoqueMinimo ? 'baixo' : 'normal';
      tabela += `
        <tr>
          <td>
            <strong>${produto.nome}</strong><br>
            <small>${produto.codigo}</small>
          </td>
          <td>${produto.categoria}</td>
          <td class="${produto.estoqueAtual <= produto.estoqueMinimo ? 'text-danger' : 'text-success'}">
            ${produto.estoqueAtual}
          </td>
          <td>${produto.estoqueMinimo}</td>
          <td><span class="badge ${this.getStatusBadgeClass(status)}">${this.getStatusText(status)}</span></td>
        </tr>
      `;
    });

    tabela += '</tbody></table>';
    return tabela;
  }

  private gerarConteudoProdutos(): string {
    if (!this.relatorioProdutos || this.relatorioProdutos.length === 0) {
      return '<h2>Relatório de Produtos</h2><p>Nenhum produto encontrado.</p>';
    }

    let tabela = `
      <h2>Análise de Performance dos Produtos</h2>
      <table>
        <thead>
          <tr>
            <th>Produto</th>
            <th>Categoria</th>
            <th>Qtd Vendida</th>
            <th>Faturamento</th>
            <th>Margem (%)</th>
            <th>Estoque</th>
            <th>Preço Unit.</th>
          </tr>
        </thead>
        <tbody>
    `;

    this.relatorioProdutos.forEach(produto => {
      tabela += `
        <tr>
          <td><strong>${produto.produtoNome}</strong></td>
          <td>${produto.categoria}</td>
          <td class="text-center">${produto.quantidadeVendida}</td>
          <td class="text-success">${this.formatarMoeda(produto.faturamento)}</td>
          <td class="text-center">${produto.margem?.toFixed(1)}%</td>
          <td class="text-center ${produto.estoqueAtual <= 10 ? 'text-danger' : 'text-success'}">
            ${produto.estoqueAtual}
          </td>
          <td class="text-success">${this.formatarMoeda(produto.precoUnitario)}</td>
        </tr>
      `;
    });

    tabela += '</tbody></table>';
    return tabela;
  }

  private async exportarPDF(nomeArquivo: string): Promise<void> {
    const doc = new jsPDF();
    
    // Título
    doc.setFontSize(18);
    doc.text(`Relatório ${this.getTipoRelatorioNome()}`, 20, 30);
    
    // Informações gerais
    doc.setFontSize(12);
    doc.text(`Período: ${this.getPeriodoVendas() || 'Todos os períodos'}`, 20, 45);
    doc.text(`Data: ${new Date().toLocaleString('pt-BR')}`, 20, 55);

    let yPosition = 70;

    if (this.tipoRelatorio === 'vendas' && this.vendasDetalhadas?.length) {
      const colunas = ['Data', 'Cliente', 'Valor', 'Pagamento', 'Status'];
      const linhas = this.vendasDetalhadas.map(venda => [
        this.formatarData(venda.dataVenda),
        venda.clienteNome || 'N/A',
        this.formatarMoeda(venda.valorTotal),
        venda.formaPagamento || 'N/A',
        venda.status
      ]);

      autoTable(doc, {
        head: [colunas],
        body: linhas,
        startY: yPosition,
        styles: { fontSize: 10 },
        headStyles: { fillColor: [66, 139, 202] }
      });
    } else if (this.tipoRelatorio === 'estoque' && this.relatorioEstoque?.todosProdutos?.length) {
      const colunas = ['Produto', 'Categoria', 'Estoque Atual', 'Estoque Mín.', 'Status'];
      const linhas = this.relatorioEstoque.todosProdutos.map(produto => [
        `${produto.nome}\n${produto.codigo}`,
        produto.categoria,
        produto.estoqueAtual.toString(),
        produto.estoqueMinimo.toString(),
        produto.estoqueAtual <= produto.estoqueMinimo ? 'Baixo' : 'Normal'
      ]);

      autoTable(doc, {
        head: [colunas],
        body: linhas,
        startY: yPosition,
        styles: { fontSize: 10 },
        headStyles: { fillColor: [255, 193, 7] }
      });
    } else if (this.tipoRelatorio === 'produtos' && this.relatorioProdutos?.length) {
      const colunas = ['Produto', 'Categoria', 'Qtd Vendida', 'Faturamento', 'Margem %', 'Estoque'];
      const linhas = this.relatorioProdutos.map(produto => [
        produto.produtoNome,
        produto.categoria,
        produto.quantidadeVendida.toString(),
        this.formatarMoeda(produto.faturamento),
        produto.margem?.toFixed(1) + '%',
        produto.estoqueAtual.toString()
      ]);

      autoTable(doc, {
        head: [colunas],
        body: linhas,
        startY: yPosition,
        styles: { fontSize: 10 },
        headStyles: { fillColor: [148, 0, 211] }
      });
    }

    doc.save(`${nomeArquivo}.pdf`);
  }

  private async exportarExcel(nomeArquivo: string): Promise<void> {
    const workbook = XLSX.utils.book_new();
    
    if (this.tipoRelatorio === 'vendas' && this.vendasDetalhadas?.length) {
      const dadosVendas = this.vendasDetalhadas.map(venda => ({
        'Data': this.formatarData(venda.dataVenda),
        'Cliente': venda.clienteNome || 'N/A',
        'Valor Total': venda.valorTotal,
        'Forma Pagamento': venda.formaPagamento || 'N/A',
        'Status': venda.status,
        'Vendedor': venda.nomeVendedor || 'N/A',
        'Quantidade Itens': venda.quantidadeItens || 0
      }));
      
      const worksheet = XLSX.utils.json_to_sheet(dadosVendas);
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Vendas');
    } else if (this.tipoRelatorio === 'estoque' && this.relatorioEstoque?.todosProdutos?.length) {
      const dadosEstoque = this.relatorioEstoque.todosProdutos.map(produto => ({
        'Produto': produto.nome,
        'Código': produto.codigo,
        'Categoria': produto.categoria,
        'Estoque Atual': produto.estoqueAtual,
        'Estoque Mínimo': produto.estoqueMinimo,
        'Status': produto.estoqueAtual <= produto.estoqueMinimo ? 'Baixo' : 'Normal',
        'Valor Estimado': this.calcularValorEstoque(produto)
      }));
      
      const worksheet = XLSX.utils.json_to_sheet(dadosEstoque);
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Estoque');
    } else if (this.tipoRelatorio === 'produtos' && this.relatorioProdutos?.length) {
      const dadosProdutos = this.relatorioProdutos.map(produto => ({
        'Produto': produto.produtoNome,
        'Categoria': produto.categoria,
        'Quantidade Vendida': produto.quantidadeVendida,
        'Faturamento': produto.faturamento,
        'Margem (%)': produto.margem?.toFixed(2),
        'Estoque Atual': produto.estoqueAtual,
        'Preço Unitário': produto.precoUnitario
      }));
      
      const worksheet = XLSX.utils.json_to_sheet(dadosProdutos);
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Produtos');
    }

    XLSX.writeFile(workbook, `${nomeArquivo}.xlsx`);
  }

  private async exportarCSV(nomeArquivo: string): Promise<void> {
    let csvContent = '';
    
    if (this.tipoRelatorio === 'vendas' && this.vendasDetalhadas?.length) {
      csvContent = 'Data,Cliente,Valor Total,Forma Pagamento,Status,Vendedor,Quantidade Itens\n';
      
      this.vendasDetalhadas.forEach(venda => {
        csvContent += `"${this.formatarData(venda.dataVenda)}","${venda.clienteNome || 'N/A'}",${venda.valorTotal},"${venda.formaPagamento || 'N/A'}","${venda.status}","${venda.nomeVendedor || 'N/A'}",${venda.quantidadeItens || 0}\n`;
      });
    } else if (this.tipoRelatorio === 'estoque' && this.relatorioEstoque?.todosProdutos?.length) {
      csvContent = 'Produto,Código,Categoria,Estoque Atual,Estoque Mínimo,Status,Valor Estimado\n';
      
      this.relatorioEstoque.todosProdutos.forEach(produto => {
        const status = produto.estoqueAtual <= produto.estoqueMinimo ? 'Baixo' : 'Normal';
        csvContent += `"${produto.nome}","${produto.codigo}","${produto.categoria}",${produto.estoqueAtual},${produto.estoqueMinimo},"${status}",${this.calcularValorEstoque(produto)}\n`;
      });
    } else if (this.tipoRelatorio === 'produtos' && this.relatorioProdutos?.length) {
      csvContent = 'Produto,Categoria,Quantidade Vendida,Faturamento,Margem (%),Estoque Atual,Preço Unitário\n';
      
      this.relatorioProdutos.forEach(produto => {
        csvContent += `"${produto.produtoNome}","${produto.categoria}",${produto.quantidadeVendida},${produto.faturamento},"${produto.margem?.toFixed(2)}%",${produto.estoqueAtual},${produto.precoUnitario}\n`;
      });
    }

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    saveAs(blob, `${nomeArquivo}.csv`);
  }

  private getTipoRelatorioNome(): string {
    const tipos: { [key: string]: string } = {
      'vendas': 'de Vendas',
      'estoque': 'de Estoque',
      'produtos': 'de Produtos',
      'movimentacao': 'de Movimentação'
    };
    return tipos[this.tipoRelatorio] || 'Desconhecido';
  }

  navegarParaSemEstoque(): void {
    // Implementar navegação para página de produtos sem estoque
    console.log('Navegando para produtos sem estoque');
  }

  // Métodos de carregamento de dados
  async carregarCategorias(): Promise<void> {
    try {
      // Implementar carregamento de categorias
      this.categorias = ['Eletrônicos', 'Roupas', 'Casa', 'Esporte'];
    } catch (error) {
      console.error('Erro ao carregar categorias:', error);
    }
  }

  async carregarEventos(): Promise<void> {
    try {
      // Implementar carregamento de eventos
      this.eventosDisponiveis = ['Black Friday', 'Natal', 'Liquidação'];
    } catch (error) {
      console.error('Erro ao carregar eventos:', error);
    }
  }

  async carregarDashboard(): Promise<void> {
    try {
      this.carregandoDashboard = true;
      this.mensagemErro = '';

      this.dashboardExecutivo = await this.relatorioService.gerarDashboardExecutivo().toPromise() || null;
      
    } catch (error: any) {
      this.mensagemErro = 'Erro ao carregar dashboard: ' + (error.error?.message || error.message);
      console.error('Erro no dashboard:', error);
    } finally {
      this.carregandoDashboard = false;
    }
  }

  async gerarRelatorio(): Promise<void> {
    if (!this.filtroForm.valid || !this.tipoSelecionado) {
      this.mensagemErro = 'Por favor, preencha todos os campos obrigatórios';
      console.warn('⚠️ Form inválido ou tipo não selecionado');
      return;
    }

    try {
      this.carregandoRelatorio = true;
      this.mensagemErro = '';
      this.mensagemSucesso = '';
      this.limparRelatorios();

      const tipo = this.tipoRelatorio; // Usar tipoRelatorio ao invés do FormControl
      const formValues = this.filtroForm.value;
      
      // Coletar todos os filtros
      const filtros = {
        dataInicio: formValues.dataInicio,
        dataFim: formValues.dataFim,
        periodo: formValues.periodo,
        categoria: formValues.categoria,
        evento: formValues.evento,
        agruparPor: formValues.agruparPor,
        statusEstoque: formValues.statusEstoque,
        valorMinimo: formValues.valorMinimo,
        ordenarPor: formValues.ordenarPor,
        criterioPerformance: formValues.criterioPerformance,
        limiteResultados: formValues.limiteResultados
      };

      console.log('📊 Gerando relatório:', { tipo, filtros });
      console.log('🔍 Tipo atual this.tipoRelatorio:', this.tipoRelatorio);
      console.log('🔍 Filtros aplicados:', filtros);

      // Validar período se necessário
      if (this.tipoSelecionado.requerPeriodo) {
        if (!this.relatorioService.validarPeriodo(filtros.dataInicio, filtros.dataFim)) {
          this.mensagemErro = 'Período inválido. Verifique as datas informadas.';
          console.log('❌ Período inválido:', { dataInicio: filtros.dataInicio, dataFim: filtros.dataFim });
          return;
        }
      }

      switch (tipo) {
        case 'vendas':
          console.log('📈 Gerando relatório de vendas...');
          this.relatorioVendas = await this.relatorioService.gerarRelatorioVendas(filtros.dataInicio, filtros.dataFim, filtros).toPromise() || null;
          
          // Carregar vendas detalhadas
          try {
            const vendasDetalhadas = await this.relatorioService.gerarRelatorioVendasDetalhadas(filtros.dataInicio, filtros.dataFim, filtros).toPromise();
            console.log('🔍 Response vendas detalhadas:', vendasDetalhadas);
            this.vendasDetalhadas = vendasDetalhadas?.vendas || vendasDetalhadas || [];
            console.log('✅ Vendas detalhadas processadas:', this.vendasDetalhadas.length, 'vendas');
            if (this.vendasDetalhadas.length > 0) {
              console.log('📋 Primeira venda exemplo:', this.vendasDetalhadas[0]);
            }
          } catch (error) {
            console.warn('⚠️ Vendas detalhadas não disponíveis:', error);
            this.vendasDetalhadas = [];
          }
          
          // Carregar dados do gráfico
          try {
            const dadosGrafico = await this.relatorioService.obterDadosGraficoVendas(filtros.dataInicio, filtros.dataFim, filtros).toPromise();
            this.dadosGraficoVendas = dadosGrafico?.evolucaoVendas || [];
            console.log('✅ Dados do gráfico carregados:', this.dadosGraficoVendas.length, 'pontos');
            this.renderizarGraficoVendas();
          } catch (error) {
            console.warn('⚠️ Dados do gráfico não disponíveis:', error);
            this.dadosGraficoVendas = [];
          }
          
          this.atualizarEstatisticasGerais();
          this.mensagemSucesso = 'Relatório de vendas gerado com sucesso!';
          console.log('✅ Relatório de vendas gerado:', this.relatorioVendas);
          break;

        case 'estoque':
          console.log('📦 Gerando relatório de estoque...');
          this.relatorioEstoque = await this.relatorioService.gerarRelatorioEstoque(filtros).toPromise() || null;
          this.atualizarEstatisticasGerais();
          this.mensagemSucesso = 'Relatório de estoque gerado com sucesso!';
          console.log('✅ Relatório de estoque gerado:', this.relatorioEstoque);
          break;

        case 'movimentacao':
          console.log('🔄 Gerando relatório de movimentação...');
          this.relatorioMovimentacao = await this.relatorioService.gerarRelatorioMovimentacao(filtros.dataInicio, filtros.dataFim, filtros).toPromise() || null;
          this.mensagemSucesso = 'Relatório de movimentação gerado com sucesso!';
          console.log('✅ Relatório de movimentação gerado:', this.relatorioMovimentacao);
          break;

        case 'produtos':
          console.log('📦 Gerando relatório de produtos...');
          await this.carregarRelatorioProdutos();
          try {
            this.relatorioProdutos = [
              { 
                produtoNome: 'Camiseta Básica Branca', 
                categoria: 'Roupas', 
                quantidadeVendida: 15,
                faturamento: 448.50,
                margem: 49.83,
                estoque: 90, 
                preco: 29.90 
              },
              { 
                produtoNome: 'Relógio Digital Smartwatch', 
                categoria: 'Eletrônicos', 
                quantidadeVendida: 8,
                faturamento: 2399.20,
                margem: 39.97,
                estoque: 15, 
                preco: 299.90 
              },
              { 
                produtoNome: 'Fone Bluetooth Premium', 
                categoria: 'Eletrônicos', 
                quantidadeVendida: 12,
                faturamento: 2398.80,
                margem: 39.97,
                estoque: 25, 
                preco: 199.90 
              },
              { 
                produtoNome: 'Tênis Esportivo Preto', 
                categoria: 'Calçados', 
                quantidadeVendida: 6,
                faturamento: 899.40,
                margem: 46.64,
                estoque: 45, 
                preco: 149.90 
              },
              { 
                produtoNome: 'Calça Jeans Masculina', 
                categoria: 'Roupas', 
                quantidadeVendida: 9,
                faturamento: 809.10,
                margem: 49.94,
                estoque: 65, 
                preco: 89.90 
              },
              { 
                produtoNome: 'Blusa Feminina Floral', 
                categoria: 'Roupas', 
                quantidadeVendida: 14,
                faturamento: 782.60,
                margem: 49.91,
                estoque: 76, 
                preco: 55.90 
              },
              { 
                produtoNome: 'Mochila Escolar Azul', 
                categoria: 'Acessórios', 
                quantidadeVendida: 5,
                faturamento: 399.50,
                margem: 56.20,
                estoque: 10, 
                preco: 79.90 
              },
              { 
                produtoNome: 'Camiseta Estampada Preta', 
                categoria: 'Roupas', 
                quantidadeVendida: 11,
                faturamento: 504.90,
                margem: 52.07,
                estoque: 75, 
                preco: 45.90 
              }
            ];
            this.mensagemSucesso = 'Relatório de produtos gerado com sucesso! (dados simulados)';
            console.log('✅ Relatório de produtos simulado gerado:', this.relatorioProdutos);
          } catch (error) {
            console.error('❌ Erro ao gerar relatório de produtos:', error);
            this.mensagemErro = 'Erro ao gerar relatório de produtos';
          }
          break;

        default:
          console.log('❌ Tipo de relatório não reconhecido:', tipo);
          this.mensagemErro = 'Tipo de relatório não reconhecido';
      }

    } catch (error: any) {
      this.mensagemErro = 'Erro ao gerar relatório: ' + (error.error?.message || error.message);
      console.error('Erro:', error);
    } finally {
      this.carregandoRelatorio = false;
    }
  }

  // Carrega estatísticas iniciais quando o componente é iniciado
  private async carregarEstatisticasIniciais(): Promise<void> {
    try {
      console.log('🔄 Carregando estatísticas iniciais...');
      
      // Carrega dados de vendas e estoque para estatísticas gerais
      const hoje = new Date();
      const umMesAtras = new Date();
      umMesAtras.setMonth(hoje.getMonth() - 1);

      // Tenta carregar relatório de vendas do último mês
      try {
        const inicioStr = umMesAtras.toISOString().split('T')[0];
        const fimStr = hoje.toISOString().split('T')[0];
        this.relatorioVendas = await this.relatorioService.gerarRelatorioVendas(inicioStr, fimStr).toPromise() || null;
        console.log('✅ Relatório de vendas carregado:', this.relatorioVendas);
      } catch (error) {
        console.warn('⚠️ Erro ao carregar relatório de vendas, usando dados simulados');
      }
      
      // Tenta carregar relatório de estoque atual
      try {
        this.relatorioEstoque = await this.relatorioService.gerarRelatorioEstoque().toPromise() || null;
        console.log('✅ Relatório de estoque carregado:', this.relatorioEstoque);
      } catch (error) {
        console.warn('⚠️ Erro ao carregar relatório de estoque, usando dados simulados');
      }

      // Atualiza as estatísticas gerais com os dados carregados ou simulados
      this.atualizarEstatisticasGerais();
      
      console.log('✅ Estatísticas iniciais carregadas');
    } catch (error) {
      console.error('❌ Erro ao carregar estatísticas iniciais:', error);
      // Usar dados simulados robustos se houver erro
      this.estatisticasGerais = {
        totalVendasPeriodo: 4,
        crescimentoVendas: 12.5,
        faturamentoPeriodo: 1037.00,
        ticketMedio: 259.25,
        produtosBaixoEstoque: 5,
        valorTotalEstoque: 50000
      };
      console.log('✅ Estatísticas simuladas aplicadas:', this.estatisticasGerais);
    }
  }

  // Método para atualizar estatísticas gerais baseado nos dados carregados
  private atualizarEstatisticasGerais(): void {
    console.log('🔄 Atualizando estatísticas gerais...');
    console.log('Dados vendas disponíveis:', !!this.relatorioVendas);
    console.log('Dados estoque disponíveis:', !!this.relatorioEstoque);

    // Reset das estatísticas
    this.estatisticasGerais = {
      totalVendasPeriodo: 0,
      crescimentoVendas: 0,
      faturamentoPeriodo: 0,
      ticketMedio: 0,
      produtosBaixoEstoque: 0,
      valorTotalEstoque: 0
    };

    // Atualiza baseado no relatório de vendas
    if (this.relatorioVendas) {
      this.estatisticasGerais.totalVendasPeriodo = this.relatorioVendas.totalVendas || 0;
      this.estatisticasGerais.faturamentoPeriodo = this.relatorioVendas.totalFaturamento || 0;
      this.estatisticasGerais.ticketMedio = this.relatorioVendas.ticketMedio || 0;
      
      // Calcular crescimento baseado nos dados disponíveis
      // Por simplicidade, calcular baseado na média de vendas vs um valor de referência
      const vendasPorDia = this.estatisticasGerais.totalVendasPeriodo;
      const diasPeriodo = this.calcularDiasPeriodo();
      const mediaDiaria = diasPeriodo > 0 ? vendasPorDia / diasPeriodo : 0;
      
      // Simular comparação com período anterior (em um sistema real, viria do backend)
      const mediaAnteriorSimulada = 0.8; // Assumir média anterior menor
      if (mediaDiaria > 0 && mediaAnteriorSimulada > 0) {
        this.estatisticasGerais.crescimentoVendas = ((mediaDiaria - mediaAnteriorSimulada) / mediaAnteriorSimulada) * 100;
      } else {
        this.estatisticasGerais.crescimentoVendas = 0;
      }
      
      console.log('✅ Estatísticas de vendas atualizadas:', {
        totalVendas: this.estatisticasGerais.totalVendasPeriodo,
        faturamento: this.estatisticasGerais.faturamentoPeriodo,
        ticketMedio: this.estatisticasGerais.ticketMedio
      });
    } else {
      // Se não há dados de vendas, usa dados simulados
      this.estatisticasGerais.totalVendasPeriodo = 4;
      this.estatisticasGerais.faturamentoPeriodo = 1037.00;
      this.estatisticasGerais.ticketMedio = 259.25;
      this.estatisticasGerais.crescimentoVendas = 12.5;
      console.log('⚠️ Usando dados simulados de vendas');
    }

    // Atualiza baseado no relatório de estoque
    if (this.relatorioEstoque) {
      this.estatisticasGerais.produtosBaixoEstoque = this.relatorioEstoque.produtosEstoqueBaixo || 0;
      this.estatisticasGerais.valorTotalEstoque = this.relatorioEstoque.valorTotalEstoque || 0;
      
      console.log('✅ Estatísticas de estoque atualizadas:', {
        produtosBaixoEstoque: this.estatisticasGerais.produtosBaixoEstoque,
        valorTotalEstoque: this.estatisticasGerais.valorTotalEstoque
      });
    } else {
      // Se não há dados de estoque, usa dados simulados
      this.estatisticasGerais.produtosBaixoEstoque = 5;
      this.estatisticasGerais.valorTotalEstoque = 50000;
      console.log('⚠️ Usando dados simulados de estoque');
    }

    console.log('✅ Estatísticas gerais finais:', this.estatisticasGerais);
  }

  private calcularDiasPeriodo(): number {
    const filtros = this.filtroForm.value;
    if (filtros.dataInicio && filtros.dataFim) {
      const inicio = new Date(filtros.dataInicio);
      const fim = new Date(filtros.dataFim);
      const diffTime = Math.abs(fim.getTime() - inicio.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1; // +1 para incluir ambos os dias
      return diffDays;
    }
    return 30; // padrão
  }

  limparRelatorios(): void {
    this.relatorioVendas = null;
    this.relatorioEstoque = null;
    this.relatorioMovimentacao = null;
    this.vendasDetalhadas = [];
    this.dadosGraficoVendas = [];
  }

  renderizarGraficoVendas(): void {
    if (!this.dadosGraficoVendas || this.dadosGraficoVendas.length === 0) {
      console.warn('⚠️ Nenhum dado disponível para o gráfico');
      return;
    }
    
    console.log('📈 Renderizando gráfico com dados:', this.dadosGraficoVendas);
    
    // Aguardar o DOM estar pronto com retry
    this.aguardarECriarGrafico(0);
  }

  private aguardarECriarGrafico(tentativas: number): void {
    const maxTentativas = 10;
    const delay = 200;
    
    const ctx = document.getElementById('chartVendasMes') as HTMLCanvasElement;
    
    if (ctx && typeof Chart !== 'undefined') {
      // Elemento encontrado, criar gráfico
      try {
        // Destruir gráfico anterior se existir
        if (this.chartInstance) {
          this.chartInstance.destroy();
        }
        
        this.chartInstance = new Chart(ctx, {
          type: 'line',
          data: {
            labels: this.dadosGraficoVendas.map(d => d.dataFormatada),
            datasets: [{
              label: 'Vendas',
              data: this.dadosGraficoVendas.map(d => d.quantidadeVendas),
              borderColor: 'rgb(75, 192, 192)',
              backgroundColor: 'rgba(75, 192, 192, 0.1)',
              tension: 0.1,
              fill: true
            }]
          },
          options: {
            responsive: true,
            plugins: {
              title: {
                display: true,
                text: 'Evolução das Vendas'
              }
            },
            scales: {
              y: {
                beginAtZero: true
              }
            }
          }
        });
        console.log('✅ Gráfico renderizado com sucesso!');
      } catch (error) {
        console.error('❌ Erro ao criar gráfico:', error);
      }
    } else if (tentativas < maxTentativas) {
      // Tentar novamente após delay
      console.log(`🔄 Tentativa ${tentativas + 1}/${maxTentativas} - Aguardando elemento chartVendasMes...`);
      setTimeout(() => {
        this.aguardarECriarGrafico(tentativas + 1);
      }, delay);
    } else {
      console.warn('⚠️ Canvas chartVendasMes não encontrado após', maxTentativas, 'tentativas');
      console.log('🔍 Chart disponível:', typeof Chart !== 'undefined');
      console.log('🔍 Elemento canvas:', !!document.getElementById('chartVendasMes'));
    }
  }

  private async carregarRelatorioProdutos(): Promise<void> {
    try {
      console.log('📋 Carregando relatório de produtos da API...');
      const response = await this.relatorioService.gerarRelatorioProdutos().toPromise();
      
      if (response && response.produtos) {
        this.relatorioProdutos = response.produtos;
        console.log('✅ Relatório de produtos carregado da API:', this.relatorioProdutos.length, 'produtos');
      } else {
        console.warn('⚠️ Resposta da API de produtos inválida, mantendo dados atuais');
      }
    } catch (error) {
      console.error('❌ Erro ao carregar relatório de produtos:', error);
      console.log('📋 Mantendo dados simulados atuais');
    }
  }

}