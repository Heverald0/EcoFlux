package com.example.EcoFlux.configuracao;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class DocumentacaoApi {
    @Bean
    public OpenAPI documentacaoEcoFlux() {
        return new OpenAPI().info(new Info().title("EcoFlux API").version("1.0.0")
                .description("Coleta seletiva, logistica, materiais e indicadores ambientais."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
