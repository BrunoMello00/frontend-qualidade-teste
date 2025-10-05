package com.qualidade.teste.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controlador REST simples para status da API
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class StatusController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "status", "ok",
            "message", "Backend Qualidade e Teste funcionando",
            "version", "1.0.0",
            "services", Map.of(
                "desconto", "Serviço de Cálculo de Descontos",
                "estoque", "Serviço de Validação de Estoque", 
                "relatorios", "Serviço de Relatórios de Vendas"
            )
        );
    }

    @GetMapping("/health")
    public Map<String, String> getHealth() {
        return Map.of(
            "status", "UP",
            "backend", "healthy"
        );
    }
}