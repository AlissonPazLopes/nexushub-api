package com.alissondev.nexushub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchemaAndHealthIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Deve validar a execucao das migrations V1 a V3 do Flyway e a criacao das 4 tabelas core")
	void shouldValidateFlywayMigrationsAndCoreTables() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND type = 'SQL'",
				Integer.class
		);
		assertThat(appliedMigrations).isEqualTo(3);

		List<String> coreTables = List.of(
				"accreditation_plans",
				"partner_organizations",
				"network_benefits",
				"csv_import_batches"
		);

		for (String tableName : coreTables) {
			Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
			assertThat(count).isZero();
		}
	}

	@Test
	@DisplayName("Deve retornar HTTP 200 OK com status UP no endpoint GET /api/v1/health")
	void shouldReturnUpStatusOnHealthEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("nexushub-api"))
				.andExpect(jsonPath("$.javaVersion").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

}
