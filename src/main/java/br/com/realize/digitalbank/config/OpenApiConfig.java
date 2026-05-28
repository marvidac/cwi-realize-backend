package br.com.realize.digitalbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI digitalBankOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Bank API")
                        .description("API REST simplificada para banco digital - Teste técnico Realize")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("API Token")
                                .description("Informe somente o token retornado pelo endpoint POST /api/auth/login. "
                                        + "O Swagger enviara automaticamente o header de autenticacao.")));
    }
}
