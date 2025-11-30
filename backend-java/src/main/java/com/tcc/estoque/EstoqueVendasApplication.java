package com.tcc.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EstoqueVendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstoqueVendasApplication.class, args);
    }
}
