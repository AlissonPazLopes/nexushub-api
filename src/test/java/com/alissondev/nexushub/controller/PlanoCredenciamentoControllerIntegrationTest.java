package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.dto.PlanoCredenciamentoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanoCredenciamentoControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("Deve criar um plano de credenciamento com HTTP 201 e normalizar o codigo para maiusculas")
	void shouldCreatePlanWithStatus201AndNormalizedUppercaseCode() throws Exception {
		PlanoCredenciamentoRequest request = new PlanoCredenciamentoRequest(
				"seed_tier",
				"Plano Startup Incubada Seed",
				new BigDecimal("199.90"),
				3,
				10
		);

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.codigo").value("SEED_TIER"))
				.andExpect(jsonPath("$.nome").value("Plano Startup Incubada Seed"))
				.andExpect(jsonPath("$.valorMensalidade").value(199.90))
				.andExpect(jsonPath("$.maxBeneficiosOfertados").value(3))
				.andExpect(jsonPath("$.cotaResgatesMensal").value(10))
				.andExpect(jsonPath("$.ativo").value(true));
	}

	@Test
	@DisplayName("Deve retornar HTTP 409 Conflict ao tentar cadastrar plano com codigo duplicado")
	void shouldReturn409ConflictWhenPlanCodeAlreadyExists() throws Exception {
		PlanoCredenciamentoRequest request = new PlanoCredenciamentoRequest(
				"GROWTH",
				"Plano Scale-up Growth",
				new BigDecimal("499.00"),
				10,
				30
		);

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		PlanoCredenciamentoRequest duplicateRequest = new PlanoCredenciamentoRequest(
				"growth",
				"Outro Plano Growth",
				new BigDecimal("599.00"),
				15,
				40
		);

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(duplicateRequest)))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Deve retornar HTTP 400 Bad Request quando os dados do plano forem invalidos")
	void shouldReturn400BadRequestWhenPayloadIsInvalid() throws Exception {
		PlanoCredenciamentoRequest invalidRequest = new PlanoCredenciamentoRequest(
				"",
				"",
				new BigDecimal("-50.00"),
				0,
				-1
		);

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Deve listar planos ativos ordenados por valor de mensalidade crescente com HTTP 200 OK")
	void shouldListActivePlansOrderedByMonthlyFeeAsc() throws Exception {
		PlanoCredenciamentoRequest corporatePlan = new PlanoCredenciamentoRequest(
				"CORPORATE",
				"Corporate Sponsor",
				new BigDecimal("1500.00"),
				30,
				100
		);
		PlanoCredenciamentoRequest seedPlan = new PlanoCredenciamentoRequest(
				"SEED",
				"Incubada Seed",
				new BigDecimal("150.00"),
				2,
				5
		);

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(corporatePlan)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/planos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(seedPlan)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/planos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].codigo").value("SEED"))
				.andExpect(jsonPath("$[1].codigo").value("CORPORATE"));
	}

	@Test
	@DisplayName("Deve retornar HTTP 404 Not Found ao buscar plano por ID inexistente")
	void shouldReturn404NotFoundWhenPlanIdDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/v1/planos/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

}
