package com.tcc.estoque.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em páginas/módulos
 * Substitui o uso de @PreAuthorize com uma abordagem mais simples e centralizata
 * 
 * Uso:
 * @RequirePageAccess("produtos") - Verifica se o usuário tem acesso à página de produtos
 * @RequirePageAccess("colaboradores") - Apenas OWNER tem acesso
 * @RequirePageAccess("vendas") - VENDEDOR, ADMIN e OWNER têm acesso
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePageAccess {
    /**
     * Código da página que deve ser verificada
     * Deve corresponder aos códigos definidos em AccessControlManager.Pagina
     */
    String value();
    
    /**
     * Permite que o próprio usuário acesse mesmo sem permissão da página
     * Útil para endpoints de perfil onde o usuário pode editar seus próprios dados
     */
    boolean allowSelfAccess() default false;
}