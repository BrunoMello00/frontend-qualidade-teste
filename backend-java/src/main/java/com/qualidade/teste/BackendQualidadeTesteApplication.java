package com.qualidade.teste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Aplicação principal Spring Boot para o Backend de Qualidade e Teste
 * 
 * Esta aplicação expõe APIs REST para consumo do frontend Angular,
 * utilizando as 3 classes de serviço criadas para o projeto acadêmico.
 * 
 * @author Projeto Qualidade e Teste de Software
 * @version 1.0.0
 */
@SpringBootApplication
@CrossOrigin(origins = "http://localhost:4200") // Permite CORS para o Angular
public class BackendQualidadeTesteApplication {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("🚀 Iniciando Backend Qualidade e Teste");
        System.out.println("📊 Serviços disponíveis:");
        System.out.println("  - Cálculo de Descontos");
        System.out.println("  - Validação de Estoque");
        System.out.println("  - Relatórios de Vendas");
        System.out.println("🌐 Server rodará em: http://localhost:8080");
        System.out.println("🔗 API docs: http://localhost:8080/api/status");
        System.out.println("===========================================");
        
        SpringApplication.run(BackendQualidadeTesteApplication.class, args);
    }
}