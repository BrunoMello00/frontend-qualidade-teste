package com.tcc.estoque.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcc.estoque.model.enums.TipoUsuario;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioDTO {

    public static class UsuarioRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        private String nome;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ter um formato válido")
        @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        private String senha;

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        private String telefone;

        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        private String cpf;

        @NotNull(message = "Tipo de usuário é obrigatório")
        private TipoUsuario tipoUsuario;

        private Boolean ativo = true;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
        public TipoUsuario getTipoUsuario() { return tipoUsuario; }
        public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    }

    public static class UsuarioResponse {
        private Long id;
        private String nome;
        private String email;
        private String telefone;
        private String cpf;
        private TipoUsuario tipoUsuario;
        private String tipoDescricao;
        private String tipoDetalhe;
        private String tipoCor;
        private Boolean ativo;
        private LocalDateTime dataCadastro;
        private LocalDateTime ultimoAcesso;
        private Integer tentativasLogin;
        private Boolean bloqueado;
        private LocalDateTime dataBloqueio;
        private List<String> permissoes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
        public TipoUsuario getTipoUsuario() { return tipoUsuario; }
        public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
        public String getTipoDescricao() { return tipoDescricao; }
        public void setTipoDescricao(String tipoDescricao) { this.tipoDescricao = tipoDescricao; }
        public String getTipoDetalhe() { return tipoDetalhe; }
        public void setTipoDetalhe(String tipoDetalhe) { this.tipoDetalhe = tipoDetalhe; }
        public String getTipoCor() { return tipoCor; }
        public void setTipoCor(String tipoCor) { this.tipoCor = tipoCor; }
        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }
        public LocalDateTime getDataCadastro() { return dataCadastro; }
        public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
        public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
        public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
        public Integer getTentativasLogin() { return tentativasLogin; }
        public void setTentativasLogin(Integer tentativasLogin) { this.tentativasLogin = tentativasLogin; }
        public Boolean getBloqueado() { return bloqueado; }
        public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
        public LocalDateTime getDataBloqueio() { return dataBloqueio; }
        public void setDataBloqueio(LocalDateTime dataBloqueio) { this.dataBloqueio = dataBloqueio; }
        public List<String> getPermissoes() { return permissoes; }
        public void setPermissoes(List<String> permissoes) { this.permissoes = permissoes; }
    }

    public static class UsuarioResumo {
        private Long id;
        private String nome;
        private String email;
        private TipoUsuario tipoUsuario;
        private String tipoDescricao;
        private String tipoCor;
        private Boolean ativo;
        private LocalDateTime ultimoAcesso;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public TipoUsuario getTipoUsuario() { return tipoUsuario; }
        public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
        public String getTipoDescricao() { return tipoDescricao; }
        public void setTipoDescricao(String tipoDescricao) { this.tipoDescricao = tipoDescricao; }
        public String getTipoCor() { return tipoCor; }
        public void setTipoCor(String tipoCor) { this.tipoCor = tipoCor; }
        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }
        public LocalDateTime getUltimoAcesso() { return ultimoAcesso; }
        public void setUltimoAcesso(LocalDateTime ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
    }

    public static class AlterarSenhaRequest {
        @NotBlank(message = "Senha atual é obrigatória")
        private String senhaAtual;

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, max = 100, message = "Nova senha deve ter entre 6 e 100 caracteres")
        private String novaSenha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmacaoSenha;

        public String getSenhaAtual() { return senhaAtual; }
        public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
        public String getConfirmacaoSenha() { return confirmacaoSenha; }
        public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
    }

    public static class FiltroUsuarios {
        private String termo; // Nome ou email
        private TipoUsuario tipoUsuario;
        private Boolean ativo;
        private String orderBy = "dataCadastro";
        private String orderDirection = "DESC";

        public String getTermo() { return termo; }
        public void setTermo(String termo) { this.termo = termo; }
        public TipoUsuario getTipoUsuario() { return tipoUsuario; }
        public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
        public Boolean getAtivo() { return ativo; }
        public void setAtivo(Boolean ativo) { this.ativo = ativo; }
        public String getOrderBy() { return orderBy; }
        public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
        public String getOrderDirection() { return orderDirection; }
        public void setOrderDirection(String orderDirection) { this.orderDirection = orderDirection; }
    }

    public static class PermissaoResponse {
        private String permissao;
        private String modulo;
        private String acao;
        private String descricao;

        public PermissaoResponse(String permissao) {
            this.permissao = permissao;
            String[] partes = permissao.split("\\.");
            if (partes.length == 2) {
                this.modulo = partes[0];
                this.acao = partes[1];
                this.descricao = formatarDescricao(modulo, acao);
            }
        }

        private String formatarDescricao(String modulo, String acao) {
            String moduloDesc;
            switch (modulo) {
                case "usuarios":
                    moduloDesc = "Usuários";
                    break;
                case "produtos":
                    moduloDesc = "Produtos";
                    break;
                case "vendas":
                    moduloDesc = "Vendas";
                    break;
                case "clientes":
                    moduloDesc = "Clientes";
                    break;
                case "eventos":
                    moduloDesc = "Eventos";
                    break;
                case "relatorios":
                    moduloDesc = "Relatórios";
                    break;
                case "dashboard":
                    moduloDesc = "Dashboard";
                    break;
                case "configuracoes":
                    moduloDesc = "Configurações";
                    break;
                case "auditoria":
                    moduloDesc = "Auditoria";
                    break;
                case "sistema":
                    moduloDesc = "Sistema";
                    break;
                default:
                    moduloDesc = modulo;
                    break;
            }

            String acaoDesc;
            switch (acao) {
                case "criar":
                    acaoDesc = "Criar";
                    break;
                case "editar":
                    acaoDesc = "Editar";
                    break;
                case "excluir":
                    acaoDesc = "Excluir";
                    break;
                case "visualizar":
                    acaoDesc = "Visualizar";
                    break;
                case "configurar":
                    acaoDesc = "Configurar";
                    break;
                case "backup":
                    acaoDesc = "Backup";
                    break;
                default:
                    acaoDesc = acao;
                    break;
            }

            return acaoDesc + " " + moduloDesc;
        }

        public String getPermissao() { return permissao; }
        public void setPermissao(String permissao) { this.permissao = permissao; }
        public String getModulo() { return modulo; }
        public void setModulo(String modulo) { this.modulo = modulo; }
        public String getAcao() { return acao; }
        public void setAcao(String acao) { this.acao = acao; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
    }

    public static class TipoUsuarioInfo {
        private TipoUsuario tipo;
        private String descricao;
        private String detalhe;
        private String cor;
        private List<PermissaoResponse> permissoes;

        public TipoUsuario getTipo() { return tipo; }
        public void setTipo(TipoUsuario tipo) { this.tipo = tipo; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getDetalhe() { return detalhe; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
        public List<PermissaoResponse> getPermissoes() { return permissoes; }
        public void setPermissoes(List<PermissaoResponse> permissoes) { this.permissoes = permissoes; }
    }
}
