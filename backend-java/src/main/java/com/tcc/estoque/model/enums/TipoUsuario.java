package com.tcc.estoque.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TipoUsuario {
    OWNER("Proprietário", "Acesso total e controle sobre todos os usuários", "#8E24AA", Arrays.asList(
        "usuarios.criar", "usuarios.editar", "usuarios.excluir", "usuarios.visualizar",
        "produtos.criar", "produtos.editar", "produtos.excluir", "produtos.visualizar",
        "vendas.criar", "vendas.editar", "vendas.excluir", "vendas.visualizar", 
        "clientes.criar", "clientes.editar", "clientes.excluir", "clientes.visualizar",
        "eventos.criar", "eventos.editar", "eventos.excluir", "eventos.visualizar",
        "relatorios.vendas", "relatorios.produtos", "relatorios.clientes", "relatorios.financeiro",
        "dashboard.visualizar", "configuracoes.editar", "auditoria.visualizar",
        "sistema.backup", "sistema.configurar", "sistema.owner.controle"
    )),
    
    ADMIN("Administrador", "Acesso completo ao sistema exceto outros admins", "#FF5722", Arrays.asList(
        "usuarios.criar", "usuarios.editar", "usuarios.excluir", "usuarios.visualizar",
        "produtos.criar", "produtos.editar", "produtos.excluir", "produtos.visualizar",
        "vendas.criar", "vendas.editar", "vendas.excluir", "vendas.visualizar", 
        "clientes.criar", "clientes.editar", "clientes.excluir", "clientes.visualizar",
        "eventos.criar", "eventos.editar", "eventos.excluir", "eventos.visualizar",
        "relatorios.vendas", "relatorios.produtos", "relatorios.clientes", "relatorios.financeiro",
        "dashboard.visualizar", "configuracoes.editar", "auditoria.visualizar",
        "sistema.backup", "sistema.configurar"
    )),
    
    VENDEDOR("Vendedor", "Vendas e atendimento ao cliente", "#2196F3", Arrays.asList(
        "produtos.visualizar",
        "vendas.criar", "vendas.editar", "vendas.visualizar",
        "clientes.criar", "clientes.editar", "clientes.visualizar",
        "eventos.criar", "eventos.editar", "eventos.visualizar",
        "relatorios.vendas", "dashboard.vendas"
    )),
    
    ESTOQUISTA("Estoquista", "Gerenciamento de estoque e inventário", "#4CAF50", Arrays.asList(
        "produtos.criar", "produtos.editar", "produtos.visualizar",
        "estoque.gerenciar", "estoque.visualizar", "estoque.entrada", "estoque.saida",
        "relatorios.estoque", "dashboard.estoque"
    )),
    
    COMPRAS("Compras", "Acesso ao catálogo para compras e pedidos", "#9C27B0", Arrays.asList(
        "produtos.visualizar"
    ));

    private final String descricao;
    private final String detalhe;
    private final String cor;
    private final List<String> permissoes;

    TipoUsuario(String descricao, String detalhe, String cor, List<String> permissoes) {
        this.descricao = descricao;
        this.detalhe = detalhe;
        this.cor = cor;
        this.permissoes = permissoes;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public String getCor() {
        return cor;
    }

    public List<String> getPermissoes() {
        return permissoes;
    }

    public boolean temPermissao(String permissao) {
        return permissoes.contains(permissao);
    }

    public boolean podeAcessarModulo(String modulo) {
        return permissoes.stream().anyMatch(p -> p.startsWith(modulo + "."));
    }

    public List<String> getPermissoesModulo(String modulo) {
        return permissoes.stream()
                .filter(p -> p.startsWith(modulo + "."))
                .map(p -> p.substring(modulo.length() + 1))
                .collect(Collectors.toList());
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isVendedor() {
        return this == VENDEDOR;
    }

    public boolean isEstoquista() {
        return this == ESTOQUISTA;
    }

    public boolean isCompras() {
        return this == COMPRAS;
    }

    public boolean podeGerenciarUsuarios() {
        return this == OWNER || this == ADMIN;
    }

    public boolean podeExcluirUsuario(TipoUsuario tipoUsuarioAlvo) {
        if (this == OWNER) {
            return tipoUsuarioAlvo != OWNER;
        }
        if (this == ADMIN) {
            return tipoUsuarioAlvo == VENDEDOR || tipoUsuarioAlvo == ESTOQUISTA || tipoUsuarioAlvo == COMPRAS;
        }
        return false;
    }

    public boolean podeVerRelatoriosCompletos() {
        return this == OWNER || this == ADMIN;
    }

    public boolean podeEditarConfiguracoes() {
        return this == OWNER || this == ADMIN;
    }

    public boolean podeRealizarVendas() {
        return this == OWNER || this == ADMIN || this == VENDEDOR;
    }

    public boolean podeGerenciarClientes() {
        return this == OWNER || this == ADMIN || this == VENDEDOR;
    }

    public boolean podeGerenciarEventos() {
        return this == OWNER || this == ADMIN || this == VENDEDOR;
    }

    public boolean podeGerenciarEstoque() {
        return this == OWNER || this == ADMIN || this == ESTOQUISTA;
    }

    public boolean podeVisualizarEstoque() {
        return this == OWNER || this == ADMIN || this == ESTOQUISTA;
    }

    public static TipoUsuario fromString(String tipo) {
        try {
            return TipoUsuario.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VENDEDOR; // Padrão para tipos inválidos
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}
