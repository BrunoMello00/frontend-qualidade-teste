package com.tcc.estoque.security;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Sistema centralizado de controle de acesso baseado em perfis de usuário
 * 
 * REGRAS DE NEGÓCIO:
 * - VENDEDOR: Acesso a Catálogo, Venda, Gerenciar Clientes, Eventos, Sistema de Pontuação e Estoque
 * - ADMIN: Acesso a TODAS as páginas exceto Colaboradores (UsuarioController)
 * - OWNER: Acesso a TUDO sem restrições
 */
public class AccessControlManager {
    
    // ========================================
    // ========================================
    
    public enum Pagina {
        CATALOGO("catalogo", "Catálogo de Produtos"),
        VENDAS("vendas", "Gerenciamento de Vendas"),
        CLIENTES("clientes", "Gerenciamento de Clientes"), 
        EVENTOS("eventos", "Sistema de Eventos"),
        SISTEMA_PONTUACAO("sistema-pontuacao", "Sistema de Pontuação"),
        
        DASHBOARD("dashboard", "Dashboard/Painel Principal"),
        PERFIL("perfil", "Perfil do Usuário"),
        
        PRODUTOS("produtos", "Gerenciamento de Produtos"),
        ESTOQUE("estoque", "Controle de Estoque"),
        RELATORIOS("relatorios", "Relatórios Gerenciais"),
        CONFIGURACOES("configuracoes", "Configurações do Sistema"),
        AUDITORIA("auditoria", "Logs de Auditoria"),
        SEGURANCA("seguranca", "Configurações de Segurança"),
        
        COLABORADORES("colaboradores", "Gerenciamento de Colaboradores");
        
        private final String codigo;
        private final String descricao;
        
        Pagina(String codigo, String descricao) {
            this.codigo = codigo;
            this.descricao = descricao;
        }
        
        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
    }
    
    // ========================================
    // ========================================
    
    private static final Set<Pagina> PAGINAS_VENDEDOR = Set.of(
        Pagina.CATALOGO,
        Pagina.VENDAS,
        Pagina.CLIENTES,
        Pagina.EVENTOS,
        Pagina.SISTEMA_PONTUACAO,
        Pagina.DASHBOARD,
        Pagina.PERFIL,
        Pagina.ESTOQUE  // ADICIONADO: Permitir vendedor acessar estoque
    );
    
    private static final Set<Pagina> PAGINAS_ESTOQUISTA = Set.of(
        Pagina.PRODUTOS,
        Pagina.ESTOQUE,
        Pagina.DASHBOARD,
        Pagina.PERFIL
    );
    
    private static final Set<Pagina> PAGINAS_ADMIN = Set.of(
        Pagina.CATALOGO,
        Pagina.VENDAS,
        Pagina.CLIENTES,
        Pagina.EVENTOS,
        Pagina.SISTEMA_PONTUACAO,
        Pagina.DASHBOARD,
        Pagina.PERFIL,
        Pagina.PRODUTOS,
        Pagina.ESTOQUE,
        Pagina.RELATORIOS,
        Pagina.CONFIGURACOES,
        Pagina.AUDITORIA,
        Pagina.SEGURANCA
    );
    
    private static final Set<Pagina> PAGINAS_OWNER = Set.of(
        Pagina.values()
    );
    
    // ========================================
    // ========================================
    
    /**
     * Verifica se um perfil de usuário tem acesso a uma página específica
     */
    public static boolean temAcessoPagina(String tipoUsuario, String paginaCodigo) {
        if (tipoUsuario == null || paginaCodigo == null) {
            return false;
        }
        
        Pagina pagina = Arrays.stream(Pagina.values())
            .filter(p -> p.getCodigo().equalsIgnoreCase(paginaCodigo))
            .findFirst()
            .orElse(null);
            
        if (pagina == null) {
            return false; // Página não encontrada
        }
        
        return temAcessoPagina(tipoUsuario, pagina);
    }
    
    /**
     * Verifica se um perfil de usuário tem acesso a uma página específica
     */
    public static boolean temAcessoPagina(String tipoUsuario, Pagina pagina) {
        if (tipoUsuario == null || pagina == null) {
            return false;
        }
        
        switch (tipoUsuario.toUpperCase()) {
            case "OWNER":
                return PAGINAS_OWNER.contains(pagina);
            case "ADMIN":
                return PAGINAS_ADMIN.contains(pagina);
            case "VENDEDOR":
                return PAGINAS_VENDEDOR.contains(pagina);
            case "ESTOQUISTA":
                return PAGINAS_ESTOQUISTA.contains(pagina);
            default:
                return false; // Tipo de usuário não reconhecido
        }
    }
    
    /**
     * Retorna todas as páginas acessíveis para um perfil de usuário
     */
    public static Set<Pagina> getPaginasAcessiveis(String tipoUsuario) {
        if (tipoUsuario == null) {
            return new HashSet<>();
        }
        
        switch (tipoUsuario.toUpperCase()) {
            case "OWNER":
                return new HashSet<>(PAGINAS_OWNER);
            case "ADMIN":
                return new HashSet<>(PAGINAS_ADMIN);
            case "VENDEDOR":
                return new HashSet<>(PAGINAS_VENDEDOR);
            case "ESTOQUISTA":
                return new HashSet<>(PAGINAS_ESTOQUISTA);
            default:
                return new HashSet<>();
        }
    }
    
    /**
     * Retorna os códigos das páginas acessíveis para um perfil de usuário
     */
    public static List<String> getCodigosPaginasAcessiveis(String tipoUsuario) {
        return getPaginasAcessiveis(tipoUsuario).stream()
            .map(Pagina::getCodigo)
            .sorted()
            .toList();
    }
    
    // ========================================
    // ========================================
    
    /**
     * Verifica acesso baseado no nome da classe do controller
     */
    public static boolean temAcessoController(String tipoUsuario, String nomeController) {
        if (tipoUsuario == null || nomeController == null) {
            return false;
        }
        
        String paginaCodigo = mapearControllerParaPagina(nomeController);
        if (paginaCodigo == null) {
            return false;
        }
        
        return temAcessoPagina(tipoUsuario, paginaCodigo);
    }
    
    /**
     * Mapeia nomes de controllers para códigos de páginas
     */
    private static String mapearControllerParaPagina(String nomeController) {
        String controller = nomeController.toLowerCase();
        
        if (controller.contains("produto")) return "produtos";
        if (controller.contains("estoque")) return "estoque";
        if (controller.contains("venda")) return "vendas";
        if (controller.contains("cliente")) return "clientes";
        if (controller.contains("evento")) return "eventos";
        if (controller.contains("usuario")) return "colaboradores";
        if (controller.contains("relatorio")) return "relatorios";
        if (controller.contains("dashboard")) return "dashboard";
        if (controller.contains("configuracao")) return "configuracoes";
        if (controller.contains("auditoria")) return "auditoria";
        if (controller.contains("security")) return "seguranca";
        if (controller.contains("perfil")) return "perfil";
        
        return null; // Controller não mapeado
    }
    
    // ========================================
    // ========================================
    
    /**
     * Verifica se o usuário é OWNER (acesso total)
     */
    public static boolean isOwner(String tipoUsuario) {
        return "OWNER".equalsIgnoreCase(tipoUsuario);
    }
    
    /**
     * Verifica se o usuário é ADMIN ou OWNER
     */
    public static boolean isAdminOuOwner(String tipoUsuario) {
        return "ADMIN".equalsIgnoreCase(tipoUsuario) || "OWNER".equalsIgnoreCase(tipoUsuario);
    }
    
    /**
     * Verifica se o usuário tem algum acesso administrativo
     */
    public static boolean temAcessoAdministrativo(String tipoUsuario) {
        return isAdminOuOwner(tipoUsuario);
    }
}