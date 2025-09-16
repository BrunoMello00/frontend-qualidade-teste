import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Usuario, TipoUsuario, StatusUsuario, LoginRequest, LoginResponse, ApiResponse } from '../models/user.models';

const STORAGE_KEY = 'mock_data_v1';

function randomDelay() {
  return 200 + Math.floor(Math.random() * 300); // 200-500ms
}

function generateId(prefix = ''): string {
  return prefix + Math.random().toString(36).substring(2, 9);
}

@Injectable({
  providedIn: 'root'
})
export class MockDataService {
  private state: any = {
    usuarios: [] as Usuario[],
    mfaCodes: {} as Record<string, string>
  };

  constructor() {
    this.load();
  }

  // Inicializa dados se não existirem
  private seedIfEmpty() {
    if (!this.state.usuarios || this.state.usuarios.length === 0) {
      const now = new Date();
      this.state.usuarios = [
        {
          id: 'u1',
          nome: 'Bruno Mello',
          email: 'bruno@admin.com',
          tipoUsuario: TipoUsuario.OWNER,
          status: StatusUsuario.ATIVO,
          primeiroAcesso: false,
          ultimoLogin: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 10),
          tentativasLogin: 0,
          dataCriacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 400),
          dataAtualizacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 10),
          avatarUrl: '',
          telefone: '(11) 99999-9999',
          metaMensal: 50000,
          comissaoPercentual: 5,
          ativo: true
        },
        {
          id: 'u2',
          nome: 'Ana Silva',
          email: 'ana@admin.com',
          tipoUsuario: TipoUsuario.ADMIN,
          status: StatusUsuario.ATIVO,
          primeiroAcesso: false,
          ultimoLogin: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 2),
          tentativasLogin: 0,
          dataCriacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 200),
          dataAtualizacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 2),
          avatarUrl: '',
          telefone: '(21) 98888-7777',
          metaMensal: 30000,
          comissaoPercentual: 4,
          ativo: true
        },
        {
          id: 'u3',
          nome: 'Carlos Pereira',
          email: 'carlos@vendas.com',
          tipoUsuario: TipoUsuario.VENDEDOR,
          status: StatusUsuario.ATIVO,
          primeiroAcesso: true,
          ultimoLogin: new Date(now.getTime() - 1000 * 60 * 60 * 6),
          tentativasLogin: 0,
          dataCriacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 30),
          dataAtualizacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 1),
          avatarUrl: '',
          telefone: '(31) 97777-6666',
          metaMensal: 20000,
          comissaoPercentual: 6,
          ativo: true
        },
        {
          id: 'u4',
          nome: 'Mariana Costa',
          email: 'mariana@estoque.com',
          tipoUsuario: TipoUsuario.VENDEDOR,
          status: StatusUsuario.ATIVO,
          primeiroAcesso: false,
          ultimoLogin: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 3),
          tentativasLogin: 0,
          dataCriacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 90),
          dataAtualizacao: new Date(now.getTime() - 1000 * 60 * 60 * 24 * 3),
          avatarUrl: '',
          telefone: '(41) 96666-5555',
          metaMensal: 15000,
          comissaoPercentual: 4.5,
          ativo: true
        }
      ];

      // Senhas (como mapa separado, não persistidas em model Usuario)
      this.state['passwords'] = {
        'u1': '123456',
        'u2': 'admin123',
        'u3': 'vendedor1',
        'u4': 'vendedor2'
      };

      this.save();
    }
  }

  private load() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        this.state = JSON.parse(raw);
      }
    } catch (e) {
      console.warn('Falha ao carregar mock state, criando novo.');
    }

    this.seedIfEmpty();
  }

  private save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.state));
    } catch (e) {
      console.error('Falha ao salvar mock state:', e);
    }
  }

  // =========================
  // USUÁRIOS E AUTENTICAÇÃO
  // =========================

  findUserByEmail(email: string): Usuario | undefined {
    return this.state.usuarios.find((u: Usuario) => u.email.toLowerCase() === email.toLowerCase());
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    const user = this.findUserByEmail(request.email);
    const d = randomDelay();

    if (!user) {
      return of({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    }

    const pwdMap = this.state['passwords'] || {};
    const pwd = pwdMap[user.id];

    if (pwd !== request.senha) {
      // incrementa tentativas
      user.tentativasLogin = (user.tentativasLogin || 0) + 1;
      this.save();
      return of({ success: false, message: 'Credenciais inválidas' }).pipe(delay(d));
    }

    // sucesso do login - atualizar último login
    user.ultimoLogin = new Date();
    user.tentativasLogin = 0;
    this.save();

    // simular token
    const token = 'mock-token-' + generateId();
    const refreshToken = 'mock-refresh-' + generateId();

    const response: LoginResponse = {
      success: true,
      message: 'Login bem-sucedido (mock)',
      token,
      refreshToken,
      usuario: user,
      primeiroAcesso: user.primeiroAcesso,
      permissoes: this.getPermissionsForUser(user)
    };

    return of(response).pipe(delay(d));
  }

  logout(token?: string): Observable<ApiResponse> {
    const d = randomDelay();
    return of({ success: true, message: 'Logout realizado (mock)' }).pipe(delay(d));
  }

  // Verifica se é necessário MFA: aqui podemos forçar MFA para vendedores como exemplo
  checkMfaRequired(email: string, senha: string): Observable<any> {
    const d = randomDelay();
    const user = this.findUserByEmail(email);
    if (!user) {
      return of({ mfaRequired: false, requiresMfa: false, message: 'Credenciais inválidas' }).pipe(delay(d));
    }

    // Simular que vendedores exigem MFA e que o código foi enviado
    const requiresMfa = user.tipoUsuario === TipoUsuario.VENDEDOR;
    if (requiresMfa) {
      const code = ('' + Math.floor(100000 + Math.random() * 900000));
      this.state.mfaCodes[user.email] = code;
      this.save();
      console.info('MFA code for', user.email, code);
      return of({ requiresMfa: true, mfaMethod: 'email', destination: user.email, message: 'Código MFA enviado para o email (mock)' }).pipe(delay(d));
    }

    return of({ requiresMfa: false, message: 'MFA não necessário' }).pipe(delay(d));
  }

  verifyMfaCode(email: string, code: string): Observable<any> {
    const d = randomDelay();
    const stored = this.state.mfaCodes[email];
    if (!stored) {
      return of({ success: false, message: 'Nenhum código MFA encontrado' }).pipe(delay(d));
    }

    if (stored !== code) {
      return of({ success: false, message: 'Código incorreto' }).pipe(delay(d));
    }

    // remover código após sucesso
    delete this.state.mfaCodes[email];
    this.save();
    return of({ success: true, message: 'MFA verificado com sucesso' }).pipe(delay(d));
  }

  resendMfaCode(email: string): Observable<any> {
    const d = randomDelay();
    const user = this.findUserByEmail(email);
    if (!user) {
      return of({ message: 'Usuário não encontrado' }).pipe(delay(d));
    }
    const code = ('' + Math.floor(100000 + Math.random() * 900000));
    this.state.mfaCodes[user.email] = code;
    this.save();
    console.info('MFA code resent for', user.email, code);
    return of({ message: 'Código MFA reenviado (mock)' }).pipe(delay(d));
  }

  solicitarRedefinicaoSenha(email: string): Observable<ApiResponse> {
    const d = randomDelay();
    const user = this.findUserByEmail(email);
    if (!user) {
      return of({ success: false, message: 'Email não cadastrado' }).pipe(delay(d));
    }

    // gerar token fictício
    const token = 'reset-' + generateId();
    this.state['resetTokens'] = this.state['resetTokens'] || {};
    this.state['resetTokens'][token] = { userId: user.id, createdAt: new Date().toISOString() };
    this.save();

    return of({ success: true, message: 'Email de redefinição enviado (mock)', data: { token } }).pipe(delay(d));
  }

  redefinirSenha(token: string, novaSenha: string): Observable<ApiResponse> {
    const d = randomDelay();
    const entry = this.state['resetTokens']?.[token];
    if (!entry) {
      return of({ success: false, message: 'Token inválido ou expirado' }).pipe(delay(d));
    }

    const user = this.state.usuarios.find((u: Usuario) => u.id === entry.userId);
    if (!user) {
      return of({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    }

    this.state['passwords'] = this.state['passwords'] || {};
    this.state['passwords'][user.id] = novaSenha;
    delete this.state['resetTokens'][token];
    this.save();

    return of({ success: true, message: 'Senha redefinida com sucesso (mock)' }).pipe(delay(d));
  }

  // Permissões simples baseadas no tipo de usuário
  private getPermissionsForUser(user: Usuario): string[] {
    if (user.tipoUsuario === TipoUsuario.OWNER) {
      return ['*'];
    }
    if (user.tipoUsuario === TipoUsuario.ADMIN) {
      return ['PRODUTOS_GERENCIAR', 'USUARIOS_GERENCIAR', 'RELATORIOS_VISUALIZAR'];
    }
    return ['VENDAS_GERENCIAR', 'PRODUTOS_VISUALIZAR'];
  }

  // Métodos de utilidade
  getAllUsers(): Observable<Usuario[]> {
    const d = randomDelay();
    return of(this.state.usuarios).pipe(delay(d));
  }

  // =========================
  // USUÁRIOS - endpoints compatíveis com /usuarios
  // =========================

  listarUsuarios(page: number = 0, limit: number = 10, termo?: string, tipoUsuario?: string, status?: string): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    let all = (this.state.usuarios || []).slice();
    if (termo) {
      const t = String(termo).toLowerCase();
      all = all.filter((u: any) => (u.nome && u.nome.toLowerCase().includes(t)) || (u.email && u.email.toLowerCase().includes(t)));
    }
    if (tipoUsuario) all = all.filter((u: any) => String(u.tipoUsuario) === String(tipoUsuario));
    if (status) all = all.filter((u: any) => String(u.status) === String(status));

    const total = all.length;
    const start = page * limit;
    const data = all.slice(start, start + limit);
    const resp = { data, total, page, limit, totalPages: Math.ceil(total / limit) };
    return of(resp).pipe(delay(d));
  }

  buscarUsuarioPorId(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const u = (this.state.usuarios || []).find((x: any) => String(x.id) === String(id));
    return of(u).pipe(delay(d));
  }

  criarUsuario(usuarioReq: any): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const nextId = generateId('u');
    const now = new Date();
    const u = Object.assign({ id: nextId, dataCriacao: now.toISOString(), ativo: true }, usuarioReq);
    this.state.usuarios = this.state.usuarios || [];
    this.state.usuarios.push(u);
    // opcional: criar senha padrão
    this.state['passwords'] = this.state['passwords'] || {};
    this.state['passwords'][u.id] = usuarioReq.senha || 'changeme';
    this.save();
    return of(u).pipe(delay(d));
  }

  atualizarUsuario(id: string | number, usuarioReq: any): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const idx = (this.state.usuarios || []).findIndex((x: any) => String(x.id) === String(id));
    if (idx === -1) return of(null).pipe(delay(d));
    const merged = Object.assign({}, this.state.usuarios[idx], usuarioReq, { dataAtualizacao: new Date().toISOString() });
    this.state.usuarios[idx] = merged;
    this.save();
    return of(merged).pipe(delay(d));
  }

  excluirUsuario(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const idx = (this.state.usuarios || []).findIndex((x: any) => String(x.id) === String(id));
    if (idx === -1) return of({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    this.state.usuarios[idx].ativo = false;
    this.save();
    return of({ success: true }).pipe(delay(d));
  }

  desativarUsuario(id: string | number): Observable<any> {
    return this.excluirUsuario(id);
  }

  reativarUsuario(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const idx = (this.state.usuarios || []).findIndex((x: any) => String(x.id) === String(id));
    if (idx === -1) return of(null).pipe(delay(d));
    this.state.usuarios[idx].ativo = true;
    this.save();
    return of(this.state.usuarios[idx]).pipe(delay(d));
  }

  listarVendedores(): Observable<any[]> {
    const d = randomDelay();
    this.seedIfEmpty();
    const arr = (this.state.usuarios || []).filter((u: any) => u.tipoUsuario === TipoUsuario.VENDEDOR && u.ativo !== false);
    return of(arr).pipe(delay(d));
  }

  obterPermissoes(id: string | number): Observable<any[]> {
    const d = randomDelay();
    this.seedIfEmpty();
    const u = (this.state.usuarios || []).find((x: any) => String(x.id) === String(id));
    if (!u) return of([]).pipe(delay(d));
    const perms = this.getPermissionsForUser(u);
    // shape compatível com UsuarioService.obterPermissoes
    const arr = perms.map((p: string, ix: number) => ({ permissao: { id: `p${ix}`, nome: p, descricao: p, modulo: 'Geral', acao: p }, concedida: true }));
    return of(arr).pipe(delay(d));
  }

  atualizarPermissao(id: string | number, permissaoId: string, concedida: boolean): Observable<any> {
    const d = randomDelay();
    // mock: não armazena permissões detalhadas, apenas responde sucesso
    return of({ success: true, message: 'Permissão atualizada (mock)' }).pipe(delay(d));
  }

  // Enviar convite por email (mock)
  enviarConvite(usuarioReq: any): Observable<any> {
    const d = randomDelay();
    console.info('Mock enviarConvite:', usuarioReq);
    // opcionalmente criar usuário provisório
    return of({ success: true, message: 'Convite enviado (mock)' }).pipe(delay(d));
  }

  // Bloquear usuário (mock)
  bloquearUsuario(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const idx = (this.state.usuarios || []).findIndex((x: any) => String(x.id) === String(id));
    if (idx === -1) return of<any>({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    this.state.usuarios[idx].status = StatusUsuario.BLOQUEADO || 'BLOQUEADO';
    this.state.usuarios[idx].ativo = false;
    this.save();
    return of<any>({ success: true, message: 'Usuário bloqueado (mock)' }).pipe(delay(d));
  }

  // Reenviar convite (mock)
  reenviarConvite(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const u = (this.state.usuarios || []).find((x: any) => String(x.id) === String(id));
    if (!u) return of<any>({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    console.info('Mock reenviarConvite para', u.email || u.nome);
    return of<any>({ success: true, message: 'Convite reenviado (mock)' }).pipe(delay(d));
  }

  // Resetar senha por id (mock)
  resetarSenhaUsuario(id: string | number): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const u = (this.state.usuarios || []).find((x: any) => String(x.id) === String(id));
    if (!u) return of<any>({ success: false, message: 'Usuário não encontrado' }).pipe(delay(d));
    this.state['passwords'] = this.state['passwords'] || {};
    this.state['passwords'][u.id] = 'changeme';
    this.save();
    console.info('Mock resetarSenhaUsuario para', u.email || u.nome);
    return of<any>({ success: true, message: 'Senha resetada para changeme (mock)' }).pipe(delay(d));
  }

  contarUsuarios(): Observable<number> {
    const d = randomDelay();
    this.seedIfEmpty();
    return of<number>((this.state.usuarios || []).filter((u: any) => u.ativo !== false).length).pipe(delay(d));
  }

  obterEstatisticasVendedores(ano?: number, mes?: number, apenasAtivos: boolean = true): Observable<any[]> {
    const d = randomDelay();
    this.seedIfEmpty();
    this.seedVendasIfEmpty();
    const vendedores = (this.state.usuarios || []).filter((u: any) => u.tipoUsuario === TipoUsuario.VENDEDOR && (!apenasAtivos || u.ativo !== false));
    const stats = vendedores.map((v: any) => {
      const vendas = (this.state.vendas || []).filter((x: any) => String(x.vendedorId) === String(v.id));
      const totalVendas = vendas.length;
      const valorTotalVendas = vendas.reduce((s: number, x: any) => s + Number(x.total || 0), 0);
      const ultimaVenda = vendas.length ? vendas.slice().reverse()[0].dataVenda : null;
      const ticketMedio = totalVendas > 0 ? valorTotalVendas / totalVendas : 0;
      return {
        id: v.id,
        nome: v.nome,
        codigoVendedor: v.codigoVendedor || '',
        email: v.email,
        totalVendas,
        valorTotalVendas: Number(valorTotalVendas.toFixed(2)),
        ticketMedio: Number(ticketMedio.toFixed(2)),
        ultimaVenda,
        metaMensal: v.metaMensal || 0,
        comissaoPercentual: v.comissaoPercentual || 0,
        percentualMeta: v.metaMensal ? Number(((valorTotalVendas / v.metaMensal) * 100).toFixed(2)) : 0
      };
    });
    return of(stats).pipe(delay(d));
  }

  // =========================
  // DASHBOARD / ESTATÍSTICAS
  // =========================

  getDashboardStats(period: 'hoje' | 'semana' | 'mes' = 'mes'): Observable<any> {
    const d = randomDelay();
    // Exemplo de estatísticas realistas
    const stats = {
      vendasHoje: { valor: 2845.50, variacao: +12.5 },
      vendasMes: { valor: 45230.80, variacao: -3.2 },
      // produtosBaixoEstoque será preenchido dinamicamente abaixo
      clientesAtivos: 234,
      eventosAtivos: 3,
      vendasSemanais: [
        { data: '2025-09-09', vendas: 1200.50 },
        { data: '2025-09-10', vendas: 980.30 },
        { data: '2025-09-11', vendas: 1450.20 },
        { data: '2025-09-12', vendas: 760.00 },
        { data: '2025-09-13', vendas: 3240.00 },
        { data: '2025-09-14', vendas: 1980.30 },
        { data: '2025-09-15', vendas: 2845.50 }
      ],
      topProdutos: [
        { nome: 'Vestido Floral', vendas: 23, receita: 1150.00 },
        { nome: 'Camisa Polo', vendas: 18, receita: 900.00 },
        { nome: 'Tênis Casual', vendas: 15, receita: 1500.00 },
        { nome: 'Calça Jeans', vendas: 12, receita: 720.00 },
        { nome: 'Jaqueta', vendas: 9, receita: 810.00 }
      ],
      vendasPorCategoria: [
        { categoria: 'Roupas', valor: 15230.00, percentual: 45.2 },
        { categoria: 'Calçados', valor: 8200.00, percentual: 24.3 },
        { categoria: 'Acessórios', valor: 5400.00, percentual: 16.0 },
        { categoria: 'Infantil', valor: 3200.00, percentual: 9.5 },
        { categoria: 'Outros', valor: 1200.00, percentual: 5.0 }
      ]
    };

    // Ajustes simples conforme período
    if (period === 'hoje') {
      stats.vendasMes.valor = stats.vendasHoje.valor;
    }

    // Expor propriedades numéricas e aliases esperados pelos componentes
    try {
      // Garantir que exista seed de produtos para calcular total
      (this as any).seedProductsIfEmpty && (this as any).seedProductsIfEmpty();
    } catch (e) {
      // ignore
    }
    const totalProdutos = (this.state && this.state.produtos) ? (this.state.produtos || []).filter((p: any) => p.ativo !== false).length : 0;
    // calcular produtos com estoque baixo a partir do estado atual
    const produtosBaixo = (this.state && this.state.produtos) ? (this.state.produtos || []).filter((p: any) => {
      if (p.ativo === false) return false;
      const estoqueAtual = Number(p.estoque || p.quantidade || 0);
      const minimo = Number(p.estoqueMinimo || p.quantidadeMinima || 5);
      return estoqueAtual <= minimo;
    }).length : 0;
    // Alias utilizados pelos templates/components
    (stats as any).totalProdutos = totalProdutos;
    (stats as any).produtosBaixoEstoque = produtosBaixo;
    (stats as any).produtosEstoqueBaixo = produtosBaixo;
    // vendasDia/vendasMes numéricos (mapeando de vendasHoje/vendasMes.valor)
    (stats as any).vendasDia = Number(((stats as any).vendasHoje && (stats as any).vendasHoje.valor) || 0);
    (stats as any).vendasMes = Number(((stats as any).vendasMes && (stats as any).vendasMes.valor) || (stats as any).vendasDia || 0);

    return of(stats).pipe(delay(d));
  }

  getVendasSemanais(): Observable<{ data: string; vendas: number; }[]> {
    const d = randomDelay();
    const arr = [
      { data: '2025-09-09', vendas: 1200.50 },
      { data: '2025-09-10', vendas: 980.30 },
      { data: '2025-09-11', vendas: 1450.20 },
      { data: '2025-09-12', vendas: 760.00 },
      { data: '2025-09-13', vendas: 3240.00 },
      { data: '2025-09-14', vendas: 1980.30 },
      { data: '2025-09-15', vendas: 2845.50 }
    ];
    return of(arr).pipe(delay(d));
  }

  getTopProdutos(limit = 5): Observable<any[]> {
    const d = randomDelay();
    const top = [
      { produtoId: 'p1', produtoNome: 'Vestido Floral', categoria: 'Roupas', quantidadeVendida: 23, valorTotal: 1150.00, percentualVendas: 12.0 },
      { produtoId: 'p2', produtoNome: 'Camisa Polo', categoria: 'Roupas', quantidadeVendida: 18, valorTotal: 900.00, percentualVendas: 9.4 },
      { produtoId: 'p3', produtoNome: 'Tênis Casual', categoria: 'Calçados', quantidadeVendida: 15, valorTotal: 1500.00, percentualVendas: 8.1 },
      { produtoId: 'p4', produtoNome: 'Calça Jeans', categoria: 'Roupas', quantidadeVendida: 12, valorTotal: 720.00, percentualVendas: 6.5 },
      { produtoId: 'p5', produtoNome: 'Jaqueta', categoria: 'Roupas', quantidadeVendida: 9, valorTotal: 810.00, percentualVendas: 4.8 }
    ];
    return of(top.slice(0, limit)).pipe(delay(d));
  }

  getVendasPorCategoria(): Observable<any[]> {
    const d = randomDelay();
    const categorias = [
      { categoria: 'Roupas', valor: 15230.00, percentual: 45.2 },
      { categoria: 'Calçados', valor: 8200.00, percentual: 24.3 },
      { categoria: 'Acessórios', valor: 5400.00, percentual: 16.0 }
    ];
    return of(categorias).pipe(delay(d));
  }

  // =========================
  // PRODUTOS (mock)
  // =========================

  private seedProductsIfEmpty() {
    if (!this.state.produtos || this.state.produtos.length === 0) {
      const now = new Date();
      // criar um conjunto pequeno (pode ser expandido) com campos compatíveis
      this.state.produtos = [
        { id: 1, nome: 'Vestido Floral', descricao: 'Vestido estampado feminino', preco: 49.99, categoria: 'Roupas', codigoBarras: '7890000000011', quantidade: 12, estoque: 12, estoqueMinimo: 5, fornecedor: 'Fornecedor A', temTamanhos: true, tamanhos: [{ tamanho: 'P', quantidade: 4 }, { tamanho: 'M', quantidade: 5 }, { tamanho: 'G', quantidade: 3 }], ativo: true, dataCadastro: now.toISOString(), vendidosMes: 23, margem: 0.45 },
        { id: 2, nome: 'Camisa Polo', descricao: 'Camisa polo masculina', preco: 49.99, categoria: 'Roupas', codigoBarras: '7890000000028', quantidade: 3, estoque: 3, estoqueMinimo: 5, fornecedor: 'Fornecedor B', temTamanhos: true, tamanhos: [{ tamanho: 'M', quantidade: 2 }, { tamanho: 'G', quantidade: 1 }], ativo: true, dataCadastro: now.toISOString(), vendidosMes: 18, margem: 0.35 },
        { id: 3, nome: 'Tênis Casual', descricao: 'Tênis confortável', preco: 99.99, categoria: 'Calçados', codigoBarras: '7890000000035', quantidade: 20, estoque: 20, estoqueMinimo: 5, fornecedor: 'Fornecedor C', temTamanhos: true, tamanhos: [{ tamanho: '40', quantidade: 10 }, { tamanho: '41', quantidade: 6 }, { tamanho: '42', quantidade: 4 }], ativo: true, dataCadastro: now.toISOString(), vendidosMes: 15, margem: 0.50 },
        { id: 4, nome: 'Calça Jeans', descricao: 'Calça jeans unissex', preco: 59.99, categoria: 'Roupas', codigoBarras: '7890000000042', quantidade: 8, estoque: 8, estoqueMinimo: 5, fornecedor: 'Fornecedor D', temTamanhos: true, tamanhos: [{ tamanho: 'M', quantidade: 4 }, { tamanho: 'G', quantidade: 4 }], ativo: true, dataCadastro: now.toISOString(), vendidosMes: 12, margem: 0.40 },
        { id: 5, nome: 'Jaqueta', descricao: 'Jaqueta leve', preco: 89.99, categoria: 'Roupas', codigoBarras: '7890000000059', quantidade: 2, estoque: 2, estoqueMinimo: 3, fornecedor: 'Fornecedor E', temTamanhos: true, tamanhos: [{ tamanho: 'M', quantidade: 1 }, { tamanho: 'G', quantidade: 1 }], ativo: true, dataCadastro: now.toISOString(), vendidosMes: 9, margem: 0.48 }
      ];

      // tipos de código de barras disponíveis
      this.state['tiposCodigoBarras'] = ['EAN-13', 'UPC-A', 'Code128'];

      this.save();
    }
  }

  private ensureProductsSeeded() {
    this.seedIfEmpty();
    this.seedProductsIfEmpty();
  }

  listarProdutos(): Observable<any[]> {
    const d = randomDelay();
    this.ensureProductsSeeded();
  return of<any[]>(this.state.produtos.slice()).pipe(delay(d));
  }

  buscarPorIdProduto(id: number): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const p = this.state.produtos.find((x: any) => Number(x.id) === Number(id));
  return of<any>(p).pipe(delay(d));
  }

  criarProduto(produto: any): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const nextId = (this.state.produtos.reduce((max: number, p: any) => Math.max(max, Number(p.id || 0)), 0) || 0) + 1;
    const now = new Date();
    const newProd = { ...produto, id: nextId, dataCadastro: now.toISOString(), ativo: produto.ativo !== undefined ? produto.ativo : true };
    this.state.produtos.push(newProd);
    this.save();
  return of<any>(newProd).pipe(delay(d));
  }

  atualizarProduto(id: number, produto: any): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const idx = this.state.produtos.findIndex((x: any) => Number(x.id) === Number(id));
    if (idx === -1) {
      return of(null).pipe(delay(d));
    }
    const updated = { ...this.state.produtos[idx], ...produto, dataAtualizacao: new Date().toISOString() };
    this.state.produtos[idx] = updated;
    this.save();
  return of<any>(updated).pipe(delay(d));
  }

  deletarProduto(id: number): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const idx = this.state.produtos.findIndex((x: any) => Number(x.id) === Number(id));
    if (idx === -1) {
    return of<any>({ success: false, message: 'Produto não encontrado' }).pipe(delay(d));
    }
    // para manter histórico de vendas, fazemos soft-delete (marcar inativo)
    this.state.produtos[idx].ativo = false;
    this.save();
    return of({ success: true, message: 'Produto desativado (mock)' }).pipe(delay(d));
  }

  atualizarEstoqueProduto(id: number, quantidade: number): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const p = this.state.produtos.find((x: any) => Number(x.id) === Number(id));
    if (!p) return of(null).pipe(delay(d));
    p.quantidade = quantidade;
    p.estoque = quantidade;
    p.ultimaMovimentacao = new Date().toISOString();
    this.save();
  return of<any>(p).pipe(delay(d));
  }

  buscarCategorias(): Observable<string[]> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const catsRaw = Array.from(new Set(this.state.produtos.map((p: any) => p.categoria)));
    const cats: string[] = catsRaw.filter((c: any) => !!c).map((c: any) => String(c));
    return of(cats).pipe(delay(d));
  }

  buscarProdutosBaixoEstoque(): Observable<any[]> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const low = this.state.produtos.filter((p: any) => p.ativo !== false && (Number(p.estoque || p.quantidade || 0) <= Number(p.estoqueMinimo || p.quantidadeMinima || 5)));
  return of<any[]>(low).pipe(delay(d));
  }

  contarProdutos(): Observable<number> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const total = this.state.produtos.filter((p: any) => p.ativo !== false).length;
  return of<number>(total).pipe(delay(d));
  }

  listarTiposCodigoBarras(): Observable<string[]> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const raw = this.state['tiposCodigoBarras'];
    const tipos: string[] = Array.isArray(raw) ? raw.map((t: any) => String(t)) : [];
    return of(tipos).pipe(delay(d));
  }

  gerarCodigoBarras(request: any): Observable<any> {
    const d = randomDelay();
    // gerar um código simples EAN-like (13 dígitos)
    const prefix = '789';
    const suffix = ('' + Math.floor(100000000 + Math.random() * 900000000)).substring(0,10);
    const codigo = prefix + suffix.substring(0,10);
    // opcional: imagem base64 placeholder
    const fakeImage = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA...';
  return of<any>({ codigo, imagem: fakeImage }).pipe(delay(d));
  }

  validarCodigoBarras(codigo: string, tipo: string): Observable<any> {
    const d = randomDelay();
    // validação simples: se tem 13 dígitos e começa com 789
    const valid = typeof codigo === 'string' && /^\d{13}$/.test(codigo);
  return of<any>({ codigo, tipo, valido: valid }).pipe(delay(d));
  }

  buscarPorCodigoBarras(codigo: string): Observable<any> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const p = this.state.produtos.find((x: any) => x.codigoBarras === codigo || x.codigo === codigo);
  return of<any>(p).pipe(delay(d));
  }

  getProdutosAtivos(): Observable<any[]> {
    const d = randomDelay();
    this.ensureProductsSeeded();
    const arr = this.state.produtos.filter((p: any) => p.ativo !== false);
  return of<any[]>(arr).pipe(delay(d));
  }

  getTiposTamanho(): Observable<any[]> {
    const d = randomDelay();
    // devolver alguns tipos de tamanho
  return of<any[]>(['P', 'M', 'G', 'GG', '40', '41', '42']).pipe(delay(d));
  }

  // =========================
  // CLIENTES (mock)
  // =========================

  private CATEGORIES: any = {
    'Regular': { minimo: 0, desconto: 0, cor: '#808080' },
    'Premium': { minimo: 1000, desconto: 5, cor: '#FFD700' },
    'VIP': { minimo: 3000, desconto: 10, cor: '#800080' }
  };

  private formatCpfRaw(digits: number[]): string {
    const s = digits.join('');
    return `${s.substring(0,3)}.${s.substring(3,6)}.${s.substring(6,9)}-${s.substring(9,11)}`;
  }

  private generateCpf(): string {
    const rand = () => Math.floor(Math.random()*9);
    const n: number[] = [];
    for (let i=0;i<9;i++) n.push(rand());

    let sum = 0;
    for (let i=0;i<9;i++) sum += n[i] * (10 - i);
    let d1 = 11 - (sum % 11);
    if (d1 >= 10) d1 = 0;
    n.push(d1);

    sum = 0;
    for (let i=0;i<10;i++) sum += n[i] * (11 - i);
    let d2 = 11 - (sum % 11);
    if (d2 >= 10) d2 = 0;
    n.push(d2);

    return this.formatCpfRaw(n);
  }

  private validateCpf(cpf: string): boolean {
    if (!cpf) return false;
    const digits = cpf.replace(/\D/g,'');
    if (digits.length !== 11) return false;
    if (/^(\d)\1{10}$/.test(digits)) return false;

    const calc = (arr: number[], factor: number) => {
      let total = 0;
      for (let i = 0; i < arr.length; i++) total += arr[i] * (factor - i);
      const rest = total % 11;
      return rest < 2 ? 0 : 11 - rest;
    };

    const nums = digits.split('').map(d => parseInt(d,10));
    const d1 = calc(nums.slice(0,9), 10);
    const d2 = calc(nums.slice(0,10), 11);
    return d1 === nums[9] && d2 === nums[10];
  }

  private generatePhone(): string {
    const ddd = ['11','21','31','41','61','71','81','51','85'];
    const prefix = ddd[Math.floor(Math.random()*ddd.length)];
    const first = 9;
    const num = Math.floor(10000000 + Math.random()*89999999);
    return `(${prefix}) ${first}${String(num).padStart(8,'0')}`;
  }

  private randomDateBetween(startYear = 1950, endYear = 2005): string {
    const start = new Date(startYear,0,1).getTime();
    const end = new Date(endYear,11,31).getTime();
    const d = new Date(start + Math.floor(Math.random() * (end - start)));
    return d.toISOString().split('T')[0];
  }

  private generateAddress(): any {
    const ceps = ['01000-000','20000-000','30000-000','40000-000','70000-000','80000-000','60000-000'];
    const streets = ['Rua das Flores', 'Avenida Central', 'Rua do Comércio', 'Praça da Sé', 'Travessa Alegre', 'Rua Nova'];
    const cidades = [{cidade:'São Paulo',uf:'SP'},{cidade:'Rio de Janeiro',uf:'RJ'},{cidade:'Belo Horizonte',uf:'MG'},{cidade:'Salvador',uf:'BA'},{cidade:'Brasília',uf:'DF'},{cidade:'Curitiba',uf:'PR'},{cidade:'Fortaleza',uf:'CE'}];
    const c = cidades[Math.floor(Math.random()*cidades.length)];
    return {
      cep: ceps[Math.floor(Math.random()*ceps.length)],
      logradouro: `${streets[Math.floor(Math.random()*streets.length)]}, ${1 + Math.floor(Math.random()*999)}`,
      bairro: 'Centro',
      cidade: c.cidade,
      uf: c.uf,
      complemento: Math.random() < 0.3 ? `Apto ${1 + Math.floor(Math.random()*200)}` : ''
    };
  }

  private categorizeByValor(valor: number): string {
    if (valor >= this.CATEGORIES['VIP'].minimo) return 'VIP';
    if (valor >= this.CATEGORIES['Premium'].minimo) return 'Premium';
    return 'Regular';
  }

  private seedClientsIfEmpty(count: number = 350) {
    if (!this.state.clientes || this.state.clientes.length === 0) {
      const firstNames = ['Maria','João','Ana','Carlos','Patrícia','Roberto','Letícia','Marcos','Sônia','Ricardo','Fernanda','Rafael','Bruna','Lucas','Beatriz'];
      const lastNames = ['Silva','Santos','Oliveira','Souza','Costa','Pereira','Alves','Araújo','Ferreira','Gomes','Ribeiro'];
      this.state.clientes = [];
      let id = 1;
      for (let i = 0; i < count; i++) {
        const nome = `${firstNames[Math.floor(Math.random()*firstNames.length)]} ${lastNames[Math.floor(Math.random()*lastNames.length)]}${Math.random()<0.3? ' ' + lastNames[Math.floor(Math.random()*lastNames.length)]: ''}`;
        const cpf = this.generateCpf();
        const email = nome.toLowerCase().replace(/\s+/g,'.').normalize('NFD').replace(/[^a-z\.-]/g,'') + '@email.com';
        const telefone = this.generatePhone();
        const dataNascimento = this.randomDateBetween(1950, 2005);
        const endereco = this.generateAddress();
        const dataCadastro = new Date(Date.now() - Math.floor(Math.random()*1000*60*60*24*365)).toISOString().split('T')[0];
        const totalCompras = Math.floor(Math.random()*50);
        const valorTotalGasto = Number((Math.random()*5000).toFixed(2));
        const categoria = this.categorizeByValor(valorTotalGasto);
        const pontosFidelidade = Math.floor(valorTotalGasto * 0.1);
        const ultimaCompra = Math.random() < 0.5 ? new Date(Date.now() - Math.floor(Math.random()*1000*60*60*24*200)).toISOString().split('T')[0] : null;
        const aniversario = new Date(dataNascimento);
        const aniversarioMes = aniversario.getMonth() + 1;
        const tags = [] as string[];
        if (Math.random() < 0.05) tags.push('vip');
        if (Math.random() < 0.2) tags.push('frequente');
        if (Math.random() < 0.1) tags.push('newsletter');

        const historico = [] as any[];
        const histCount = Math.floor(Math.random()*5);
        for (let h=0; h<histCount; h++) {
          historico.push({ data: new Date(Date.now() - Math.floor(Math.random()*1000*60*60*24*365)).toISOString().split('T')[0], valor: Number((Math.random()*500).toFixed(2)), itens: 1 + Math.floor(Math.random()*5) });
        }

        this.state.clientes.push({
          id,
          nome,
          email,
          telefone,
          cpf,
          dataNascimento,
          genero: Math.random() < 0.5 ? 'Masculino' : 'Feminino',
          endereco,
          categoria,
          pontosFidelidade,
          totalCompras,
          valorTotalGasto,
          ultimaCompra,
          dataCadastro,
          ativo: true,
          observacoes: Math.random() < 0.05 ? 'Cliente preferencial' : '',
          aniversarioMes,
          tags,
          historicoCompras: historico
        });

        id++;
      }

      this.save();
    }
  }

  listarClientes(page: number = 0, size: number = 20, search?: string, categoria?: string, ativo?: boolean): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    let all = (this.state.clientes || []).slice().reverse();
    if (search) {
      const term = String(search).toLowerCase();
      all = all.filter((c: any) => (c.nome && c.nome.toLowerCase().includes(term)) || (c.email && c.email.toLowerCase().includes(term)) || (c.cpf && c.cpf.includes(term)) );
    }
    if (categoria) all = all.filter((c: any) => c.categoria === categoria);
    if (typeof ativo === 'boolean') all = all.filter((c: any) => !!c.ativo === ativo);

    const totalElements = all.length;
    const start = page * size;
    const content = all.slice(start, start + size);

    const resp = { content, totalElements, totalPages: Math.ceil(totalElements/size), number: page, size };
    return of(resp).pipe(delay(d));
  }

  buscarClientePorId(id: number): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const c = (this.state.clientes || []).find((x: any) => Number(x.id) === Number(id));
    return of<any>(c).pipe(delay(d));
  }

  criarCliente(clienteReq: any): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const nextId = (this.state.clientes || []).reduce((m: number, c: any) => Math.max(m, Number(c.id || 0)), 0) + 1;
    const now = new Date().toISOString().split('T')[0];
    const cliente = Object.assign({
      id: nextId,
      dataCadastro: now,
      pontosFidelidade: clienteReq.pontosFidelidade || 0,
      totalCompras: clienteReq.totalCompras || 0,
      valorTotalGasto: clienteReq.valorTotalGasto || 0,
      ativo: clienteReq.ativo !== undefined ? clienteReq.ativo : true,
      historicoCompras: clienteReq.historicoCompras || []
    }, clienteReq);

    cliente.categoria = this.categorizeByValor(Number(cliente.valorTotalGasto || 0));

    this.state.clientes = this.state.clientes || [];
    this.state.clientes.push(cliente);
    this.save();
    return of<any>(cliente).pipe(delay(d));
  }

  atualizarCliente(id: number, clienteReq: any): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const idx = (this.state.clientes || []).findIndex((c: any) => Number(c.id) === Number(id));
    if (idx === -1) return of<any>(null).pipe(delay(d));
    const merged = Object.assign({}, this.state.clientes[idx], clienteReq);
    merged.categoria = this.categorizeByValor(Number(merged.valorTotalGasto || 0));
    this.state.clientes[idx] = merged;
    this.save();
    return of<any>(merged).pipe(delay(d));
  }

  deletarCliente(id: number): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const idx = (this.state.clientes || []).findIndex((c: any) => Number(c.id) === Number(id));
    if (idx === -1) return of<any>({ success: false, message: 'Cliente não encontrado' }).pipe(delay(d));
    this.state.clientes[idx].ativo = false;
    this.save();
    return of<any>({ success: true }).pipe(delay(d));
  }

  buscarPorCpf(cpf: string): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const clean = String(cpf).replace(/\D/g,'');
    const c = (this.state.clientes || []).find((x: any) => (x.cpf || '').replace(/\D/g,'') === clean);
    return of<any>(c).pipe(delay(d));
  }

  buscarCep(cep: string): Observable<any> {
    const d = randomDelay();
    this.state['cepCache'] = this.state['cepCache'] || {};
    const key = String(cep).replace(/\D/g,'');
    if (this.state['cepCache'][key]) return of<any>(this.state['cepCache'][key]).pipe(delay(d));
    const addr = this.generateAddress();
    this.state['cepCache'][key] = addr;
    this.save();
    return of<any>(addr).pipe(delay(d));
  }

  adicionarPontosCliente(clienteId: number, pontos: number, descricao?: string): Observable<any> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const idx = (this.state.clientes || []).findIndex((c: any) => Number(c.id) === Number(clienteId));
    if (idx === -1) return of<any>(null).pipe(delay(d));
    this.state.clientes[idx].pontosFidelidade = (this.state.clientes[idx].pontosFidelidade || 0) + Number(pontos);
    this.save();
    return of<any>(this.state.clientes[idx]).pipe(delay(d));
  }

  contarClientes(): Observable<number> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    return of<number>((this.state.clientes || []).length).pipe(delay(d));
  }

  exportClientesCSV(): Observable<string> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const cols = ['id','nome','email','telefone','cpf','dataNascimento','categoria','pontosFidelidade','totalCompras','valorTotalGasto','ultimaCompra','dataCadastro','ativo','tags'];
    const rows = (this.state.clientes || []).map((c: any) => cols.map(col => JSON.stringify(c[col] || '')).join(','));
    const csv = [cols.join(','), ...rows].join('\n');
    return of(csv).pipe(delay(d));
  }

  obterAniversariantes(mes?: number): Observable<any[]> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const m = mes || (new Date()).getMonth() + 1;
    const arr = (this.state.clientes || []).filter((c: any) => Number(c.aniversarioMes) === Number(m));
    return of(arr).pipe(delay(d));
  }

  buscarClientesPorTag(tag: string): Observable<any[]> {
    const d = randomDelay();
    this.seedClientsIfEmpty();
    const arr = (this.state.clientes || []).filter((c: any) => Array.isArray(c.tags) && c.tags.includes(tag));
    return of(arr).pipe(delay(d));
  }

  // Atualiza cliente quando uma venda é criada
  private touchClienteOnVenda(clienteIdent: any, venda: any) {
    if (!clienteIdent) return;
    this.seedClientsIfEmpty();
    let cliente: any;
    if (clienteIdent.id) cliente = (this.state.clientes || []).find((c: any) => Number(c.id) === Number(clienteIdent.id));
    if (!cliente && clienteIdent.cpf) cliente = (this.state.clientes || []).find((c: any) => (c.cpf||'').replace(/\D/g,'') === String(clienteIdent.cpf).replace(/\D/g,''));
    if (!cliente && clienteIdent.email) cliente = (this.state.clientes || []).find((c: any) => (c.email||'').toLowerCase() === String(clienteIdent.email).toLowerCase());
    if (!cliente) return;
    cliente.totalCompras = (cliente.totalCompras || 0) + 1;
    cliente.valorTotalGasto = Number(((cliente.valorTotalGasto || 0) + Number(venda.total || 0)).toFixed(2));
    cliente.ultimaCompra = venda.dataVenda;
    cliente.pontosFidelidade = (cliente.pontosFidelidade || 0) + (venda.pontosFidelidade || 0);
    cliente.historicoCompras = cliente.historicoCompras || [];
    cliente.historicoCompras.push({ data: venda.dataVenda, valor: venda.total, itens: (venda.itens || []).length });
    cliente.categoria = this.categorizeByValor(Number(cliente.valorTotalGasto || 0));
    this.save();
  }

  // =========================
  // VENDAS (mock)
  // =========================

  private seedVendasIfEmpty(count: number = 250) {
    if (!this.state.vendas || this.state.vendas.length === 0) {
      this.ensureProductsSeeded();
      const now = new Date();
      const statusOptions = ['PENDENTE', 'CONFIRMADA', 'ENTREGUE', 'CANCELADA'];
      const formasPagamento = ['PIX', 'Cartão de Crédito', 'Cartão de Débito', 'Dinheiro', 'Transferência', 'Boleto'];
      const nomes = ['Maria Silva Santos','João Paulo Oliveira','Carla Mendes','Fernando Souza','Patrícia Lima','Roberto Costa','Letícia Alves','Marcos Vinicius','Sonia Pereira','Ricardo Moreira'];
      const eventos = ['Festa de Aniversário','Lançamento de Produto','Promoção de Verão','Evento Institucional','Nenhum'];

      this.state.vendas = [];
      let nextId = 1;

      for (let i = 0; i < count; i++) {
        const vendaDate = new Date(now.getTime() - Math.floor(Math.random() * 1000 * 60 * 60 * 24 * 365));
        const clienteNome = nomes[Math.floor(Math.random() * nomes.length)];
        const clienteCpf = `${Math.floor(100+Math.random()*899)}.${Math.floor(100+Math.random()*899)}.${Math.floor(100+Math.random()*899)}-${Math.floor(10+Math.random()*89)}`;
        const clienteEmail = clienteNome.toLowerCase().replace(/ /g, '.') + '@email.com';
        const clienteTelefone = `(11) 9${Math.floor(60000000 + Math.random()*30000000)}`;
        const itensCount = 1 + Math.floor(Math.random() * 3);
        const itens: any[] = [];
        let subtotal = 0;

        for (let j = 0; j < itensCount; j++) {
          const produto = this.state.produtos[Math.floor(Math.random() * this.state.produtos.length)];
          if (!produto) continue;
          const tamanho = produto.temTamanhos && produto.tamanhos && produto.tamanhos.length ? produto.tamanhos[Math.floor(Math.random()*produto.tamanhos.length)].tamanho : '';
          const quantidade = 1 + Math.floor(Math.random() * 2);
          const precoUnitario = produto.preco || 0;
          const precoTotal = Number((precoUnitario * quantidade).toFixed(2));
          subtotal += precoTotal;
          itens.push({ produtoId: produto.id, nomeProduto: produto.nome, tamanho: tamanho, cor: '', quantidade, precoUnitario, precoTotal });
        }

        const desconto = Math.random() < 0.2 ? Number((Math.random() * 30).toFixed(2)) : 0; // desconto ocasional
        const percentualDesconto = subtotal > 0 ? Number(((desconto / subtotal) * 100).toFixed(2)) : 0;
        const total = Number((subtotal - desconto).toFixed(2));
        const formaPagamento = formasPagamento[Math.floor(Math.random()*formasPagamento.length)];
        const status = statusOptions[Math.floor(Math.random()*statusOptions.length)];
        const vendedor = this.state.usuarios.find((u: any) => u.tipoUsuario === TipoUsuario.VENDEDOR) || this.state.usuarios[0];
        const pontosFidelidade = Math.floor(total * 0.1);

        const venda = {
          id: nextId++,
          dataVenda: vendaDate.toISOString().replace('T',' ').split('.')[0],
          clienteNome,
          clienteEmail,
          clienteTelefone,
          clienteCpf,
          evento: eventos[Math.floor(Math.random()*eventos.length)],
          itens,
          subtotal: Number(subtotal.toFixed(2)),
          desconto,
          percentualDesconto,
          total,
          formaPagamento,
          parcelas: formaPagamento === 'Cartão de Crédito' ? (1 + Math.floor(Math.random()*6)) : 1,
          status,
          vendedorId: vendedor?.id || null,
          vendedorNome: vendedor?.nome || 'Vendedor',
          observacoes: Math.random() < 0.1 ? 'Pedido com entrega expressa' : '',
          pontosFidelidade,
          dataEntrega: new Date(vendaDate.getTime() + 1000*60*60*24*(1+Math.floor(Math.random()*5))).toISOString().split('T')[0],
          numeroNF: this.generateNFNumber()
        };

        this.state.vendas.push(venda);
      }

      this.save();
    }
  }

  private generateNFNumber(): string {
    const seq = (this.state._nfSeq = (this.state._nfSeq || 1000) + 1);
    const year = new Date().getFullYear();
    return `NF-${year}-${String(seq).padStart(6,'0')}`;
  }

  listarVendas(page: number = 0, size: number = 10, dataInicio?: string|Date, dataFim?: string|Date, status?: string, vendedorId?: string): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    let all = (this.state.vendas || []).slice().reverse(); // mais recentes primeiro

    if (dataInicio) {
      const di = new Date(String(dataInicio));
      all = all.filter((v: any) => new Date(v.dataVenda) >= di);
    }
    if (dataFim) {
      const df = new Date(String(dataFim));
      all = all.filter((v: any) => new Date(v.dataVenda) <= df);
    }
    if (status) {
      all = all.filter((v: any) => v.status === status);
    }
    if (vendedorId) {
      all = all.filter((v: any) => String(v.vendedorId) === String(vendedorId));
    }

      // Ajustar shape para compatibilidade máxima com o componente
      const totalElements = all.length;
      const start = page * size;
      const content = all.slice(start, start + size).map((v: any) => {
        // Buscar clienteId se possível
        let clienteId = null;
        if (v.clienteCpf) {
          const cliente = (this.state.clientes || []).find((c: any) => (c.cpf||'').replace(/\D/g,'') === String(v.clienteCpf).replace(/\D/g,''));
          if (cliente) clienteId = cliente.id;
        }
        // Mapear itens para incluir produto, descontoItem, percentualDesconto
        const itens = (v.itens || []).map((item: any) => {
          const produto = (this.state.produtos || []).find((p: any) => Number(p.id) === Number(item.produtoId));
          return {
            ...item,
            produto: produto ? { ...produto, nome: produto.nome } : undefined,
            descontoItem: item.descontoItem !== undefined ? item.descontoItem : 0,
            percentualDesconto: item.percentualDesconto !== undefined ? item.percentualDesconto : 0
          };
        });
        return {
          ...v,
          clienteId,
          valorTotal: v.total, // alias para compatibilidade
          itens
        };
      });
      const resp = {
        content,
        totalElements,
        totalPages: Math.ceil(totalElements / size),
        number: page,
        size
      };
      return of(resp).pipe(delay(d));
  }

  buscarVendaPorId(id: number): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    const v = (this.state.vendas || []).find((x: any) => Number(x.id) === Number(id));
    return of<any>(v).pipe(delay(d));
  }

  criarVenda(vendaReq: any): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    this.ensureProductsSeeded();

    const nextId = (this.state.vendas.reduce((m: number, v: any) => Math.max(m, Number(v.id || 0)), 0) || 0) + 1;
    const now = new Date();
    // Construir itens e decrementar estoque
    const itens: any[] = (vendaReq.itens || []).map((it: any) => ({
      produtoId: it.produtoId,
      nomeProduto: (this.state.produtos.find((p: any) => Number(p.id) === Number(it.produtoId)) || {}).nome || 'Produto',
      tamanho: it.tamanho || '',
      cor: it.cor || '',
      quantidade: it.quantidade,
      precoUnitario: it.precoUnitario,
      precoTotal: Number((it.precoUnitario * it.quantidade).toFixed(2))
    }));

    let subtotal = itens.reduce((s,p) => s + p.precoTotal, 0);
    const desconto = vendaReq.desconto || 0;
    const percentualDesconto = subtotal > 0 ? Number(((desconto / subtotal) * 100).toFixed(2)) : 0;
    const total = Number((subtotal - desconto).toFixed(2));

    const vendedor = this.state.usuarios.find((u: any) => u.id === vendaReq.vendedorId) || this.state.usuarios.find((u: any) => u.tipoUsuario === TipoUsuario.VENDEDOR) || this.state.usuarios[0];

    const venda = {
      id: nextId,
      dataVenda: now.toISOString().replace('T',' ').split('.')[0],
      clienteNome: vendaReq.clienteNome || 'Cliente Avulso',
      clienteEmail: vendaReq.clienteEmail || null,
      clienteTelefone: vendaReq.clienteTelefone || null,
      clienteCpf: vendaReq.clienteCpf || null,
      evento: vendaReq.evento || null,
      itens,
      subtotal: Number(subtotal.toFixed(2)),
      desconto,
      percentualDesconto,
      total,
      formaPagamento: vendaReq.formaPagamento || 'PIX',
      parcelas: vendaReq.parcelas || 1,
      status: vendaReq.status || 'PENDENTE',
      vendedorId: vendedor?.id || null,
      vendedorNome: vendedor?.nome || 'Vendedor Mock',
      observacoes: vendaReq.observacoes || '',
      pontosFidelidade: Math.floor(total * 0.1),
      dataEntrega: vendaReq.dataEntrega || null,
      numeroNF: this.generateNFNumber()
    };

    // Decrementar estoque dos produtos vendidos (respeitar quantidades por tamanho quando aplicável)
    itens.forEach(it => {
      const p = this.state.produtos.find((x: any) => Number(x.id) === Number(it.produtoId));
      if (!p) return;
      const newQty = (Number(p.quantidade || p.estoque || 0) - Number(it.quantidade || 0));
      p.quantidade = Math.max(0, newQty);
      p.estoque = p.quantidade;
      // atualizar tamanhos se existir
      if (p.tamanhos && Array.isArray(p.tamanhos) && it.tamanho) {
        const t = p.tamanhos.find((tt: any) => String(tt.tamanho) === String(it.tamanho));
        if (t) t.quantidade = Math.max(0, (Number(t.quantidade || 0) - Number(it.quantidade || 0)));
      }
    });

    this.state.vendas = this.state.vendas || [];
    this.state.vendas.push(venda);
    // atualizar cliente vinculado (se existir)
    try {
      (this as any).touchClienteOnVenda && (this as any).touchClienteOnVenda({ id: vendaReq.clienteId, cpf: vendaReq.clienteCpf, email: vendaReq.clienteEmail }, venda);
    } catch (e) {
      // ignore
    }
    this.save();

    return of<any>(venda).pipe(delay(d));
  }

  confirmarVenda(id: number): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    const idx = (this.state.vendas || []).findIndex((v: any) => Number(v.id) === Number(id));
    if (idx === -1) return of<any>({ success: false, message: 'Venda não encontrada' }).pipe(delay(d));
    this.state.vendas[idx].status = 'CONFIRMADA';
    this.save();
    return of<any>(this.state.vendas[idx]).pipe(delay(d));
  }

  cancelarVenda(id: number, motivo?: string): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    const idx = (this.state.vendas || []).findIndex((v: any) => Number(v.id) === Number(id));
    if (idx === -1) return of<any>({ success: false, message: 'Venda não encontrada' }).pipe(delay(d));
    this.state.vendas[idx].status = 'CANCELADA';
    this.state.vendas[idx].observacoes = (this.state.vendas[idx].observacoes || '') + ' | CANCELADA: ' + (motivo || 'sem motivo');
    this.save();
    return of<any>({ success: true, message: 'Venda cancelada (mock)' }).pipe(delay(d));
  }

  obterEstatisticasVendas(vendedorId?: string, dataInicio?: string|Date, dataFim?: string|Date): Observable<any> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
    const all = (this.state.vendas || []);
    const hoje = new Date();
    const vendasDia = all.filter((v: any) => new Date(v.dataVenda).toDateString() === hoje.toDateString()).length;
    const vendasMes = all.filter((v: any) => new Date(v.dataVenda).getMonth() === hoje.getMonth() && new Date(v.dataVenda).getFullYear() === hoje.getFullYear()).length;
    const faturamentoDia = all.filter((v: any) => new Date(v.dataVenda).toDateString() === hoje.toDateString()).reduce((s: number, v: any) => s + Number(v.total || 0), 0);
    const faturamentoMes = all.filter((v: any) => new Date(v.dataVenda).getMonth() === hoje.getMonth() && new Date(v.dataVenda).getFullYear() === hoje.getFullYear()).reduce((s: number, v: any) => s + Number(v.total || 0), 0);
    const ticketMedio = vendasMes > 0 ? faturamentoMes / vendasMes : 0;

    return of<any>({ vendasDia, vendasMes, vendasAno: all.length, totalVendas: all.length, faturamentoDia, faturamentoMes, faturamentoAno: faturamentoMes, ticketMedio }).pipe(delay(d));
  }

  obterVendasRecentes(limit: number = 10): Observable<any[]> {
    const d = randomDelay();
    this.seedVendasIfEmpty();
      const arr = (this.state.vendas || []).slice().reverse().slice(0, limit).map((v: any) => {
        // Buscar clienteId se possível
        let clienteId = null;
        if (v.clienteCpf) {
          const cliente = (this.state.clientes || []).find((c: any) => (c.cpf||'').replace(/\D/g,'') === String(v.clienteCpf).replace(/\D/g,''));
          if (cliente) clienteId = cliente.id;
        }
        // Mapear itens para incluir produto, descontoItem, percentualDesconto
        const itens = (v.itens || []).map((item: any) => {
          const produto = (this.state.produtos || []).find((p: any) => Number(p.id) === Number(item.produtoId));
          return {
            ...item,
            produto: produto ? { ...produto, nome: produto.nome } : undefined,
            descontoItem: item.descontoItem !== undefined ? item.descontoItem : 0,
            percentualDesconto: item.percentualDesconto !== undefined ? item.percentualDesconto : 0
          };
        });
        return {
          ...v,
          clienteId,
          valorTotal: v.total, // alias para compatibilidade
          itens
        };
      });
      return of<any[]>(arr).pipe(delay(d));
  }

  atualizarStatusVenda(id: number, status: string, observacoes?: string): Observable<any> {
    const d = randomDelay();
    const idx = (this.state.vendas || []).findIndex((v: any) => Number(v.id) === Number(id));
    if (idx === -1) return of<any>(null).pipe(delay(d));
    this.state.vendas[idx].status = status;
    if (observacoes) this.state.vendas[idx].observacoes = (this.state.vendas[idx].observacoes || '') + ' | ' + observacoes;
    this.save();
    return of<any>(this.state.vendas[idx]).pipe(delay(d));
  }

  // =========================
  // EVENTOS (mock)
  // =========================

  private TIPOS_EVENTO: string[] = [
    'Aniversário', 'Casamento', 'Formatura', 'Batizado', 'Primeira Comunhão', 'Festa Infantil', 'Chá de Bebê', 'Chá de Panela', 'Confraternização', 'Outro'
  ];

  private seedEventosIfEmpty(count: number = 60) {
    if (!this.state.eventos || this.state.eventos.length === 0) {
      this.seedClientsIfEmpty();
      this.seedVendasIfEmpty();
      const now = new Date();
      const locais = ['Salão de Festas Villa Real', 'Clube Social', 'Espaço Eventos Central', 'Igreja Matriz', 'Buffet Sol Nascente', 'Casa de Festas Jardim'];
      const nomes = ['Festa de Aniversário', 'Casamento', 'Formatura', 'Chá de Bebê', 'Confraternização', 'Lançamento'];

      this.state.eventos = [];
      let id = 1;
      for (let i = 0; i < count; i++) {
        const tipo = this.TIPOS_EVENTO[Math.floor(Math.random() * this.TIPOS_EVENTO.length)];
        const nome = `${tipo} - ${['Maria','João','Ana','Carlos','Patrícia','Roberto','Letícia'][Math.floor(Math.random()*7)]}`;
        const cliente = (this.state.clientes || [])[Math.floor(Math.random()* (this.state.clientes || []).length)] || null;
        const diasOffset = Math.floor(Math.random() * 120) - 60; // eventos passados e futuros
        const dataEvento = new Date(now.getTime() + diasOffset * 24 * 60 * 60 * 1000);
        const horaEvento = `${9 + Math.floor(Math.random() * 10)}:00`;
        const local = locais[Math.floor(Math.random()*locais.length)];
        const numeroConvidados = 20 + Math.floor(Math.random()*200);
        const orcamentoPrevisto = Number((1000 + Math.random()*15000).toFixed(2));

        // vincular algumas vendas pelo id aleatório (se existirem vendas)
        const vendasIds: number[] = [];
        if (Array.isArray(this.state.vendas) && this.state.vendas.length > 0) {
          const countV = Math.floor(Math.random() * 5);
          for (let v=0; v<countV; v++) {
            const chosen = this.state.vendas[Math.floor(Math.random()*this.state.vendas.length)];
            if (chosen && chosen.id) vendasIds.push(chosen.id);
          }
        }

        // calcular valorGasto a partir das vendas vinculadas (se houver)
        let valorGasto = 0;
        vendasIds.forEach((vid: number) => {
          const vend = (this.state.vendas || []).find((x: any) => Number(x.id) === Number(vid));
          if (vend) valorGasto += Number(vend.total || 0);
        });
        // se não houver vendas vinculadas, gerar um gasto fictício parcial
        if (valorGasto === 0) valorGasto = Number((Math.random() * orcamentoPrevisto * 0.8).toFixed(2));

        const statusOptions = ['PLANEJAMENTO','CONFIRMADO','EM_ANDAMENTO','FINALIZADO','CANCELADO'];
        const status = statusOptions[Math.floor(Math.random()*statusOptions.length)];

        const evento = {
          id,
          nome,
          descricao: `${tipo} organizado com atenção aos detalhes.`,
          cliente: cliente ? { id: cliente.id, nome: cliente.nome, telefone: cliente.telefone } : null,
          dataEvento: dataEvento.toISOString().split('T')[0],
          horaEvento,
          local,
          endereco: cliente ? `${cliente.endereco.logradouro}, ${cliente.endereco.bairro} - ${cliente.endereco.cidade}/${cliente.endereco.uf}` : local + ', Centro',
          tipoEvento: tipo,
          numeroConvidados,
          status,
          orcamentoPrevisto,
          valorGasto: Number(valorGasto.toFixed(2)),
          vendas: vendasIds,
          observacoes: Math.random() < 0.2 ? 'Tema especial solicitado' : '',
          dataCriacao: new Date(now.getTime() - Math.floor(Math.random()*1000*60*60*24*60)).toISOString().split('T')[0],
          responsavelId: this.state.usuarios && this.state.usuarios.length ? this.state.usuarios[Math.floor(Math.random()*this.state.usuarios.length)].id : null,
          responsavelNome: this.state.usuarios && this.state.usuarios.length ? this.state.usuarios[Math.floor(Math.random()*this.state.usuarios.length)].nome : 'Equipe',
          tags: [tipo.toLowerCase(), Math.random() < 0.3 ? 'importante' : ''],
          lembretes: Math.random() < 0.6 ? [
            { tipo: 'email', data: new Date(dataEvento.getTime() - 2*24*60*60*1000).toISOString().split('T')[0], enviado: false, mensagem: 'Lembrete: seu evento é em 2 dias!' }
          ] : [],
          ativo: true
        };

        this.state.eventos.push(evento);
        id++;
      }

      // store tipos
      this.state['tiposEvento'] = this.TIPOS_EVENTO.slice();

      this.save();
    }
  }

  listarEventos(page: number = 0, size: number = 50, search?: string, tipo?: string, status?: string, dataInicio?: string|Date, dataFim?: string|Date): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    let all = (this.state.eventos || []).slice().reverse();
    if (search) {
      const term = String(search).toLowerCase();
      all = all.filter((e: any) => (e.nome && e.nome.toLowerCase().includes(term)) || (e.descricao && e.descricao.toLowerCase().includes(term)));
    }
    if (tipo) all = all.filter((e: any) => e.tipoEvento === tipo);
    if (status) all = all.filter((e: any) => e.status === status);
    if (dataInicio) {
      const di = new Date(String(dataInicio));
      all = all.filter((e: any) => new Date(e.dataEvento) >= di);
    }
    if (dataFim) {
      const df = new Date(String(dataFim));
      all = all.filter((e: any) => new Date(e.dataEvento) <= df);
    }

    const totalElements = all.length;
    const start = page * size;
    const content = all.slice(start, start + size);

    const resp = { content, totalElements, totalPages: Math.ceil(totalElements/size), number: page, size };
    return of(resp).pipe(delay(d));
  }

  buscarEventoPorId(id: number): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const e = (this.state.eventos || []).find((x: any) => Number(x.id) === Number(id));
    return of<any>(e).pipe(delay(d));
  }

  criarEvento(eventoReq: any): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const nextId = (this.state.eventos || []).reduce((m: number, e: any) => Math.max(m, Number(e.id || 0)), 0) + 1;
    const now = new Date().toISOString().split('T')[0];
    const evento = Object.assign({ id: nextId, dataCriacao: now, ativo: true, vendas: eventoReq.vendas || [], lembretes: eventoReq.lembretes || [] }, eventoReq);

    // calcular valorGasto a partir de vendas vinculadas
    let valorGasto = 0;
    (evento.vendas || []).forEach((vid: number) => {
      const vend = (this.state.vendas || []).find((x: any) => Number(x.id) === Number(vid));
      if (vend) valorGasto += Number(vend.total || 0);
    });
    evento.valorGasto = Number((valorGasto || evento.valorGasto || 0).toFixed(2));

    this.state.eventos = this.state.eventos || [];
    this.state.eventos.push(evento);
    this.save();
    return of<any>(evento).pipe(delay(d));
  }

  atualizarEvento(id: number, eventoReq: any): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const idx = (this.state.eventos || []).findIndex((c: any) => Number(c.id) === Number(id));
    if (idx === -1) return of<any>(null).pipe(delay(d));
    const merged = Object.assign({}, this.state.eventos[idx], eventoReq);
    // recalcular valorGasto se vendas alteradas
    let valorGasto = 0;
    (merged.vendas || []).forEach((vid: number) => {
      const vend = (this.state.vendas || []).find((x: any) => Number(x.id) === Number(vid));
      if (vend) valorGasto += Number(vend.total || 0);
    });
    merged.valorGasto = Number((valorGasto || merged.valorGasto || 0).toFixed(2));
    this.state.eventos[idx] = merged;
    this.save();
    return of<any>(merged).pipe(delay(d));
  }

  excluirEvento(id: number): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const idx = (this.state.eventos || []).findIndex((c: any) => Number(c.id) === Number(id));
    if (idx === -1) return of<any>({ success: false, message: 'Evento não encontrado' }).pipe(delay(d));
    // soft-delete
    this.state.eventos[idx].ativo = false;
    this.save();
    return of<any>({ success: true }).pipe(delay(d));
  }

  alterarStatusEvento(id: number, status: string): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const idx = (this.state.eventos || []).findIndex((c: any) => Number(c.id) === Number(id));
    if (idx === -1) return of<any>(null).pipe(delay(d));
    this.state.eventos[idx].status = status;
    this.save();
    return of<any>(this.state.eventos[idx]).pipe(delay(d));
  }

  buscarPorTipo(tipo: string): Observable<any[]> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const arr = (this.state.eventos || []).filter((e: any) => e.tipoEvento === tipo && e.ativo !== false);
    return of<any[]>(arr).pipe(delay(d));
  }

  buscarPorPeriodo(dataInicio: string|Date, dataFim: string|Date): Observable<any[]> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const di = new Date(String(dataInicio));
    const df = new Date(String(dataFim));
    const arr = (this.state.eventos || []).filter((e: any) => {
      const dt = new Date(e.dataEvento);
      return dt >= di && dt <= df;
    });
    return of<any[]>(arr).pipe(delay(d));
  }

  getTiposEvento(): Observable<string[]> {
    const d = randomDelay();
    const raw = this.state['tiposEvento'];
    const tipos: string[] = Array.isArray(raw) ? raw.map((x: any) => String(x)) : this.TIPOS_EVENTO.slice();
    this.state['tiposEvento'] = tipos;
    return of<string[]>(tipos).pipe(delay(d));
  }

  obterRelatorioLucratividade(eventoId: number): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const ev = (this.state.eventos || []).find((e: any) => Number(e.id) === Number(eventoId));
    if (!ev) return of<any>(null).pipe(delay(d));
    // receita baseada nas vendas vinculadas (ids) ou por nome
    let receita = 0;
    if (Array.isArray(ev.vendas) && ev.vendas.length > 0) {
      ev.vendas.forEach((vid: number) => {
        const vend = (this.state.vendas || []).find((x: any) => Number(x.id) === Number(vid));
        if (vend) receita += Number(vend.total || 0);
      });
    } else {
      // buscar vendas cujo campo evento contenha o nome do evento
      receita = (this.state.vendas || []).filter((v: any) => v.evento && String(v.evento).toLowerCase().includes(String(ev.nome || '').toLowerCase())).reduce((s: number, v: any) => s + Number(v.total || 0), 0);
    }

    const orcamento = Number(ev.orcamentoPrevisto || 0);
    const valorGasto = Number(ev.valorGasto || receita || 0);
    const lucro = Number((receita - valorGasto).toFixed(2));

    return of<any>({ eventoId, receita: Number(receita.toFixed(2)), orcamento, valorGasto, lucro }).pipe(delay(d));
  }

  listarLembretesPendentes(hoje?: string): Observable<any[]> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const hojeStr = hoje || (new Date()).toISOString().split('T')[0];
    const arr: any[] = [];
    (this.state.eventos || []).forEach((ev: any) => {
      (ev.lembretes || []).forEach((l: any, idx: number) => {
        if (!l.enviado && new Date(l.data) <= new Date(hojeStr)) {
          arr.push({ eventoId: ev.id, eventoNome: ev.nome, lembreteIndex: idx, lembrete: l });
        }
      });
    });
    return of<any[]>(arr).pipe(delay(d));
  }

  enviarLembrete(eventoId: number, lembreteIndex: number): Observable<any> {
    const d = randomDelay();
    this.seedEventosIfEmpty();
    const idx = (this.state.eventos || []).findIndex((e: any) => Number(e.id) === Number(eventoId));
    if (idx === -1) return of<any>({ success: false, message: 'Evento não encontrado' }).pipe(delay(d));
    const lemb = (this.state.eventos[idx].lembretes || [])[lembreteIndex];
    if (!lemb) return of<any>({ success: false, message: 'Lembrete não encontrado' }).pipe(delay(d));
    lemb.enviado = true;
    this.save();
    return of<any>({ success: true }).pipe(delay(d));
  }

  // =========================
  // CONFIGURAÇÕES DO SISTEMA (mock)
  // =========================

  private seedConfiguracoesIfEmpty() {
    this.state['configuracoes'] = this.state['configuracoes'] || {
      tentativasLoginMax: 5,
      tempoBloqueioPadrao: 30, // minutos
      tempoSessao: 2, // horas
      forcarTrocaSenha: false,
      validadeSenha: 90, // dias
      tamanhoMinimoSenha: 6,
      requireSenhaComplexaAdmin: true,
      requireSenhaComplexaVendedor: false,
      enviarEmailNovoUsuario: true,
      enviarEmailResetSenha: true,
      temaPadrao: 'claro',
      comportamentoExportacao: 'incluirBOM'
    };
    this.save();
  }

  getConfiguracoes(): Observable<any> {
    const d = randomDelay();
    this.seedConfiguracoesIfEmpty();
    return of<any>(this.state['configuracoes']).pipe(delay(d));
  }

  atualizarConfiguracoes(payload: any): Observable<any> {
    const d = randomDelay();
    this.seedConfiguracoesIfEmpty();
    this.state['configuracoes'] = Object.assign({}, this.state['configuracoes'], payload);
    this.save();
    return of<any>(this.state['configuracoes']).pipe(delay(d));
  }

  // =========================
  // EMAIL / NOTIFICAÇÕES (mock)
  // =========================

  enviarEmail(request: any): Observable<any> {
    const d = randomDelay();
    // Apenas logar no console e retornar sucesso
    console.info('Mock enviarEmail:', request);
    return of<any>({ success: true }).pipe(delay(d));
  }

  enviarEmailResetSenha(email: string): Observable<any> {
    const d = randomDelay();
    // reutilizar solicitarRedefinicaoSenha para gerar token e responder
    return this.solicitarRedefinicaoSenha(email).pipe(delay(d));
  }

  validarCodigoReset(codigo: string): Observable<{ valido: boolean }> {
    const d = randomDelay();
    const ok = !!(this.state['resetTokens'] && this.state['resetTokens'][codigo]);
    return of<{ valido: boolean }>({ valido: ok }).pipe(delay(d));
  }

  validarEmail(email: string): Observable<any> {
    const d = randomDelay();
    this.seedIfEmpty();
    const exists = (this.state.usuarios || []).some((u: any) => (u.email||'').toLowerCase() === String(email||'').toLowerCase());
    // Compatível com expectativas do frontend: { disponivel: boolean }
    return of<any>({ disponivel: !exists }).pipe(delay(d));
  }

  enviarNotificacaoAlteracaoPerfil(email: string, nome: string, campos: string[]): Observable<any> {
    const d = randomDelay();
    console.info('Mock enviarNotificacaoAlteracaoPerfil para', email, campos);
    return of<any>({ success: true }).pipe(delay(d));
  }

  enviarNotificacaoMudancaSenha(email: string, nomeUsuario: string, dataAlteracao: Date): Observable<any> {
    const d = randomDelay();
    console.info('Mock enviarNotificacaoMudancaSenha para', email, dataAlteracao);
    return of<any>({ success: true }).pipe(delay(d));
  }

}
