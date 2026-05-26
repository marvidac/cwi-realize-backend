package br.com.realize.digitalbank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI digitalBankOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Bank API")
                        .description("API REST simplificada para banco digital - Teste técnico Realize")
                        .version("1.0.0"));
    }
}
