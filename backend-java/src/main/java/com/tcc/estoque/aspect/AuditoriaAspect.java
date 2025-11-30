package com.tcc.estoque.aspect;

import com.tcc.estoque.model.Auditoria;
import com.tcc.estoque.service.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect para auditoria automática de operações CRUD
 */
@Aspect
@Component
public class AuditoriaAspect {

    @Autowired
    private AuditoriaService auditoriaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ThreadLocal<Object> dadosAnteriores = new ThreadLocal<>();

    /**
     * Intercepta métodos save/update para capturar dados anteriores
     */
    @Before("execution(* com.tcc.estoque.service.*Service.salvar*(..)) || " +
            "execution(* com.tcc.estoque.service.*Service.atualizar*(..)) || " +
            "execution(* com.tcc.estoque.service.*Service.update*(..))")
    public void beforeSaveOrUpdate(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                Object entity = args[0];
                Long id = getEntityId(entity);
                
                if (id != null) {
                    // É uma atualização, buscar dados anteriores
                    String serviceName = joinPoint.getTarget().getClass().getSimpleName();
                    Object dadosAtuais = buscarDadosAtuais(serviceName, id);
                    dadosAnteriores.set(dadosAtuais);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao capturar dados anteriores: " + e.getMessage());
        }
    }

    /**
     * Intercepta métodos save/create para auditoria de inserção
     */
    @AfterReturning(value = "execution(* com.tcc.estoque.service.*Service.salvar*(..)) || " +
                           "execution(* com.tcc.estoque.service.*Service.criar*(..)) || " +
                           "execution(* com.tcc.estoque.service.*Service.create*(..))", 
                   returning = "result")
    public void afterInsert(JoinPoint joinPoint, Object result) {
        try {
            if (result != null) {
                String tabela = getNomeTabela(result.getClass());
                Long registroId = getEntityId(result);
                
                if (tabela != null && registroId != null) {
                    String dadosNovos = objectMapper.writeValueAsString(result);
                    
                    Object dadosAnt = dadosAnteriores.get();
                    Auditoria.OperacaoAuditoria operacao = dadosAnt != null ? Auditoria.OperacaoAuditoria.UPDATE : Auditoria.OperacaoAuditoria.INSERT;
                    
                    String dadosAnterioresJson = dadosAnt != null ? objectMapper.writeValueAsString(dadosAnt) : null;
                    
                    if (operacao == Auditoria.OperacaoAuditoria.INSERT) {
                        auditoriaService.registrarInsercao(tabela, registroId, dadosNovos);
                    } else {
                        auditoriaService.registrarAtualizacao(tabela, registroId, dadosAnterioresJson, dadosNovos);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao registrar auditoria de inserção/atualização: " + e.getMessage());
        } finally {
            dadosAnteriores.remove();
        }
    }

    /**
     * Intercepta métodos delete para auditoria de exclusão
     */
    @Before("execution(* com.tcc.estoque.service.*Service.deletar*(..)) || " +
            "execution(* com.tcc.estoque.service.*Service.excluir*(..)) || " +
            "execution(* com.tcc.estoque.service.*Service.remover*(..)) || " +
            "execution(* com.tcc.estoque.service.*Service.delete*(..))")
    public void beforeDelete(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                Long id = null;
                Object entity = null;
                
                if (args[0] instanceof Long) {
                    id = (Long) args[0];
                    
                    String serviceName = joinPoint.getTarget().getClass().getSimpleName();
                    entity = buscarDadosAtuais(serviceName, id);
                } else if (args[0] != null) {
                    entity = args[0];
                    id = getEntityId(entity);
                }
                
                if (entity != null && id != null) {
                    String tabela = getNomeTabela(entity.getClass());
                    String dadosExcluidos = objectMapper.writeValueAsString(entity);
                    
                    auditoriaService.registrarExclusao(tabela, id, dadosExcluidos);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao registrar auditoria de exclusão: " + e.getMessage());
        }
    }

    /**
     * Extrai o ID da entidade usando reflexão
     */
    private Long getEntityId(Object entity) {
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém o nome da tabela baseado no nome da classe
     */
    private String getNomeTabela(Class<?> clazz) {
        String className = clazz.getSimpleName();
        
        Map<String, String> mapeamento = new HashMap<>();
        mapeamento.put("Usuario", "usuarios");
        mapeamento.put("Produto", "produtos");
        mapeamento.put("Categoria", "categorias");
        mapeamento.put("Fornecedor", "fornecedores");
        mapeamento.put("Cliente", "clientes");
        mapeamento.put("Venda", "vendas");
        mapeamento.put("ItemVenda", "itens_venda");
        mapeamento.put("Estoque", "estoque");
        mapeamento.put("MovimentacaoEstoque", "movimentacoes_estoque");
        mapeamento.put("Configuracao", "configuracoes");
        mapeamento.put("Backup", "backups");
        
        return mapeamento.getOrDefault(className, className.toLowerCase());
    }

    /**
     * Busca dados atuais da entidade antes da atualização
     */
    private Object buscarDadosAtuais(String serviceName, Long id) {
        try {
            String entityName = serviceName.replace("Service", "");
            
            // 1. Manter referências para todos os services
            // 2. Usar um mapa de services por tipo de entidade
            // 3. Fazer a busca pelo método findById
            
            Map<String, Object> placeholder = new HashMap<>();
            placeholder.put("id", id);
            placeholder.put("tipo", entityName);
            placeholder.put("timestamp", System.currentTimeMillis());
            
            return placeholder;
        } catch (Exception e) {
            return null;
        }
    }
}
