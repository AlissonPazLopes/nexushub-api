package com.alissondev.nexushub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI nexusHubOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("NexusHub API")
						.version("1.0.0")
						.description("Motor Back-end B2B (SaaS API-First) para Polos Tecnologicos e Redes de Empresas Parceiras: "
								+ "credenciamento por tiers, catalogo de beneficios concorrente (@Version) e ingestao CSV assincrona.")
						.contact(new Contact()
								.name("Jose Alisson Paz Lopes")
								.url("https://github.com/AlissonPazLopes/nexushub-api")));
	}

}
