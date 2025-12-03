package com.tcc.estoque.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Utilitário para análise de mudanças em dados auditados
 */
@Component
public class AuditoriaUtil {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Compara dois objetos JSON e retorna as diferenças
     */
    public Map<String, Object> compararDados(String dadosAnteriores, String dadosNovos) {
        Map<String, Object> diferencas = new HashMap<>();
        
        try {
            if (dadosAnteriores == null || dadosNovos == null) {
                diferencas.put("erro", "Dados não disponíveis para comparação");
                return diferencas;
            }
            
            JsonNode nodeAntigo = objectMapper.readTree(dadosAnteriores);
            JsonNode nodeNovo = objectMapper.readTree(dadosNovos);
            
            compararNodes(nodeAntigo, nodeNovo, "", diferencas);
            
        } catch (Exception e) {
            diferencas.put("erro", "Erro ao comparar dados: " + e.getMessage());
        }
        
        return diferencas;
    }
    
    /**
     * Compara nós JSON recursivamente
     */
    private static void compararNodes(JsonNode nodeAntigo, JsonNode nodeNovo, String prefix, Map<String, Object> diferencas) {
        if (nodeAntigo == null && nodeNovo == null) {
            return;
        }
        
        if (nodeAntigo == null) {
            diferencas.put(prefix + "novo", nodeNovo.toString());
            return;
        }
        
        if (nodeNovo == null) {
            diferencas.put(prefix + "removido", nodeAntigo.toString());
            return;
        }
        
        if (nodeAntigo.isObject() && nodeNovo.isObject()) {
            Set<String> campos = new HashSet<>();
            nodeAntigo.fieldNames().forEachRemaining(campos::add);
            nodeNovo.fieldNames().forEachRemaining(campos::add);
            
            for (String campo : campos) {
                String novoPrefix = prefix.isEmpty() ? campo : prefix + "." + campo;
                compararNodes(nodeAntigo.get(campo), nodeNovo.get(campo), novoPrefix, diferencas);
            }
        } else if (!nodeAntigo.equals(nodeNovo)) {
            Map<String, Object> mudanca = new HashMap<>();
            mudanca.put("de", nodeAntigo.asText());
            mudanca.put("para", nodeNovo.asText());
            diferencas.put(prefix, mudanca);
        }
    }
    
    /**
     * Extrai informações relevantes de dados JSON
     */
    public Map<String, Object> extrairInformacoes(String dadosJson) {
        Map<String, Object> informacoes = new HashMap<>();
        
        try {
            if (dadosJson == null || dadosJson.trim().isEmpty()) {
                informacoes.put("erro", "Dados não disponíveis");
                return informacoes;
            }
            
            JsonNode node = objectMapper.readTree(dadosJson);
            
            if (node.has("id")) {
                informacoes.put("id", node.get("id").asLong());
            }
            
            if (node.has("nome")) {
                informacoes.put("nome", node.get("nome").asText());
            }
            
            if (node.has("email")) {
                informacoes.put("email", node.get("email").asText());
            }
            
            if (node.has("descricao")) {
                informacoes.put("descricao", node.get("descricao").asText());
            }
            
            if (node.has("valor")) {
                informacoes.put("valor", node.get("valor").asDouble());
            }
            
            if (node.has("quantidade")) {
                informacoes.put("quantidade", node.get("quantidade").asInt());
            }
            
            if (node.has("ativo")) {
                informacoes.put("ativo", node.get("ativo").asBoolean());
            }
            
            if (node.has("dataAtualizacao")) {
                informacoes.put("dataAtualizacao", node.get("dataAtualizacao").asText());
            }
            
            if (node.has("dataCriacao")) {
                informacoes.put("dataCriacao", node.get("dataCriacao").asText());
            }
            
        } catch (Exception e) {
            informacoes.put("erro", "Erro ao extrair informações: " + e.getMessage());
        }
        
        return informacoes;
    }
    
    /**
     * Formata dados JSON para exibição humana
     */
    public String formatarParaExibicao(String dadosJson) {
        try {
            if (dadosJson == null || dadosJson.trim().isEmpty()) {
                return "Dados não disponíveis";
            }
            
            JsonNode node = objectMapper.readTree(dadosJson);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            
        } catch (Exception e) {
            return "Erro ao formatar dados: " + e.getMessage();
        }
    }
    
    /**
     * Verifica se uma mudança é considerada crítica
     */
    public boolean isMudancaCritica(String tabela, Map<String, Object> diferencas) {
        if (diferencas == null || diferencas.isEmpty()) {
            return false;
        }
        
        Set<String> camposCriticos = getCamposCriticos(tabela);
        
        for (String campo : diferencas.keySet()) {
            if (camposCriticos.contains(campo)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Retorna campos considerados críticos para cada tabela
     */
    private static Set<String> getCamposCriticos(String tabela) {
        Map<String, Set<String>> camposCriticosPorTabela = new HashMap<>();
        
        camposCriticosPorTabela.put("usuarios", Set.of(
            "email", "senha", "tipoUsuario", "ativo", "administrador"
        ));
        
        camposCriticosPorTabela.put("produtos", Set.of(
            "preco", "custoUnitario", "categoria", "ativo"
        ));
        
        camposCriticosPorTabela.put("estoque", Set.of(
            "quantidade", "estoqueMinimo", "estoqueMaximo"
        ));
        
        camposCriticosPorTabela.put("vendas", Set.of(
            "total", "status", "cliente", "usuario"
        ));
        
        camposCriticosPorTabela.put("configuracoes", Set.of(
            "valor", "ativo"
        ));
        
        return camposCriticosPorTabela.getOrDefault(tabela, new HashSet<>());
    }
    
    /**
     * Gera resumo de uma operação de auditoria
     */
    public String gerarResumoOperacao(String tabela, String operacao, 
                                           String dadosAnteriores, String dadosNovos) {
        StringBuilder resumo = new StringBuilder();
        
        resumo.append("Operação: ").append(operacao).append(" em ").append(tabela);
        
        if ("INSERT".equals(operacao)) {
            Map<String, Object> info = extrairInformacoes(dadosNovos);
            resumo.append(" - Novo registro");
            if (info.containsKey("nome")) {
                resumo.append(" '").append(info.get("nome")).append("'");
            }
            if (info.containsKey("id")) {
                resumo.append(" (ID: ").append(info.get("id")).append(")");
            }
        } else if ("UPDATE".equals(operacao)) {
            Map<String, Object> diferencas = compararDados(dadosAnteriores, dadosNovos);
            resumo.append(" - ").append(diferencas.size()).append(" campo(s) alterado(s)");
            
            if (isMudancaCritica(tabela, diferencas)) {
                resumo.append(" [CRÍTICO]");
            }
        } else if ("DELETE".equals(operacao)) {
            Map<String, Object> info = extrairInformacoes(dadosAnteriores);
            resumo.append(" - Registro removido");
            if (info.containsKey("nome")) {
                resumo.append(" '").append(info.get("nome")).append("'");
            }
            if (info.containsKey("id")) {
                resumo.append(" (ID: ").append(info.get("id")).append(")");
            }
        }
        
        return resumo.toString();
    }
    
    /**
     * Sanitiza dados sensíveis antes de armazenar na auditoria
     */
    public String sanitizarDadosSensiveis(String dadosJson) {
        try {
            if (dadosJson == null || dadosJson.trim().isEmpty()) {
                return dadosJson;
            }
            
            JsonNode node = objectMapper.readTree(dadosJson);
            
            if (node.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("senha");
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("password");
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("token");
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("apiKey");
                
                if (node.has("cpf")) {
                    String cpf = node.get("cpf").asText();
                    if (cpf.length() > 6) {
                        String mascarado = cpf.substring(0, 3) + "***" + cpf.substring(cpf.length() - 2);
                        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("cpf", mascarado);
                    }
                }
            }
            
            return objectMapper.writeValueAsString(node);
            
        } catch (Exception e) {
            return dadosJson; // Retorna original em caso de erro
        }
    }
}
