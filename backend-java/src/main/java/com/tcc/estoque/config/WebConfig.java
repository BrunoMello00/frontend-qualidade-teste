package com.tcc.estoque.config;

import com.tcc.estoque.security.interceptor.PageAccessInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web MVC EXPLÍCITA para garantir que controllers tenham prioridade sobre recursos estáticos
 * + Configuração do interceptor de controle de acesso baseado em páginas
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PageAccessInterceptor pageAccessInterceptor;

    public WebConfig(PageAccessInterceptor pageAccessInterceptor) {
        this.pageAccessInterceptor = pageAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("🔧 WebConfig: Adicionando PageAccessInterceptor");
        registry.addInterceptor(pageAccessInterceptor)
                .addPathPatterns("/api/**") // Aplicar apenas aos endpoints da API
                .excludePathPatterns(
                    "/api/auth/**",      // Excluir endpoints de autenticação
                    "/api/public/**",    // Excluir endpoints públicos
                    "/api/health",       // Excluir health check
                    "/api/*/teste*"      // Excluir endpoints de teste
                );
        log.info("✅ WebConfig: PageAccessInterceptor adicionado com sucesso");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        registry.addResourceHandler("/h2-console/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/h2-database/")
                .setCachePeriod(0);
        
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .setCachePeriod(0);
        
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(3600);
        
    }
}