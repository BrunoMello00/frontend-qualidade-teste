package com.tcc.estoque.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;

@Embeddable
public class Endereco {

    @Column(name = "rua", length = 255)
    @Size(max = 255, message = "Rua deve ter no máximo 255 caracteres")
    private String rua;

    @Column(name = "numero", length = 10)
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    @Column(name = "complemento", length = 100)
    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    @Column(name = "bairro", length = 100)
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String bairro;

    @Column(name = "cidade", length = 100)
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;

    @Column(name = "cep", length = 10)
    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    private String cep;

    @Column(name = "estado", length = 2)
    @Size(max = 2, message = "Estado deve ter no máximo 2 caracteres")
    private String estado;

    public Endereco() {}

    public Endereco(String rua, String numero, String complemento, String bairro, 
                   String cidade, String cep, String estado) {
        this.rua = rua;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.cep = cep;
        this.estado = estado;
    }

    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEnderecoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (rua != null && !rua.trim().isEmpty()) {
            sb.append(rua);
            if (numero != null && !numero.trim().isEmpty()) {
                sb.append(", ").append(numero);
            }
            if (complemento != null && !complemento.trim().isEmpty()) {
                sb.append(" - ").append(complemento);
            }
        }
        if (bairro != null && !bairro.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bairro);
        }
        if (cidade != null && !cidade.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cidade);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(estado);
        }
        if (cep != null && !cep.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("CEP: ").append(cep);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getEnderecoCompleto();
    }
}
