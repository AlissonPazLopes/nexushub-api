package com.alissondev.nexushub.exception;

import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.AcaoCredenciamento;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.AlterarStatusRequest;
import com.alissondev.nexushub.dto.OrganizacaoParceiraRequest;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProblemDetailContractTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanoCredenciamentoRepository planoRepository;

	@Autowired
	private GlobalExceptionHandler globalExceptionHandler;

	private PlanoCredenciamento planoAtivo;
	private PlanoCredenciamento planoInativo;

	@BeforeEach
	void setUp() {
		planoAtivo = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth",
				new BigDecimal("499.00"),
				10,
				25
		));

		PlanoCredenciamento inativo = new PlanoCredenciamento(
				"LEGACY",
				"Plano Descontinuado",
				new BigDecimal("99.00"),
				1,
				2
		);
		inativo.desativar();
		planoInativo = planoRepository.saveAndFlush(inativo);
	}

	@Test
	@DisplayName("Deve retornar ProblemDetail RFC 7807 com lista de erros por campo para HTTP 400 de validacao")
	void shouldReturnProblemDetailContractFor400ValidationError() throws Exception {
		OrganizacaoParceiraRequest invalidRequest = new OrganizacaoParceiraRequest(
				"123",
				"",
				"",
				"email-invalido",
				null,
				null
		);

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors", not(empty())))
				.andExpect(jsonPath("$.errors[0].field").isNotEmpty())
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	@DisplayName("Deve retornar ProblemDetail RFC 7807 para HTTP 400 de payload JSON malformado")
	void shouldReturnProblemDetailContractFor400MalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"cnpj\": \"11222333000181\", "))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	@DisplayName("Deve retornar ProblemDetail RFC 7807 para HTTP 404 ResourceNotFoundException")
	void shouldReturnProblemDetailContractFor404NotFound() throws Exception {
		mockMvc.perform(get("/api/v1/organizacoes/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	@DisplayName("Deve retornar ProblemDetail RFC 7807 para HTTP 409 ConflictException e conflito otimista")
	void shouldReturnProblemDetailContractFor409Conflict() throws Exception {
		OrganizacaoParceiraRequest request = new OrganizacaoParceiraRequest(
				"11.222.333/0001-81",
				"FinPulse",
				"FinPulse Pagamentos S.A.",
				"contato@finpulse.io",
				VerticalTech.FINTECH,
				planoAtivo.getId()
		);

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty());

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("PATCH", "/api/v1/beneficios/1/resgatar");
		ResponseEntity<ProblemDetail> optimisticResponse = globalExceptionHandler.handleOptimisticLocking(
				new ObjectOptimisticLockingFailureException("BeneficioRede", UUID.randomUUID()),
				servletRequest
		);

		assertThat(optimisticResponse.getStatusCode().value()).isEqualTo(409);
		assertThat(optimisticResponse.getBody()).isNotNull();
		assertThat(optimisticResponse.getBody().getTitle()).isNotBlank();
		assertThat(optimisticResponse.getBody().getStatus()).isEqualTo(409);
		assertThat(optimisticResponse.getBody().getDetail()).isNotBlank();
	}

	@Test
	@DisplayName("Deve retornar ProblemDetail RFC 7807 para HTTP 422 BusinessRuleException")
	void shouldReturnProblemDetailContractFor422UnprocessableEntity() throws Exception {
		OrganizacaoParceiraRequest inactivePlanRequest = new OrganizacaoParceiraRequest(
				"11.444.777/0001-61",
				"AgroSense IoT",
				"AgroSense Tecnologia Ltda",
				"contato@agrosense.io",
				VerticalTech.AGRITECH,
				planoInativo.getId()
		);

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(inactivePlanRequest)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty());

		OrganizacaoParceiraRequest validRequest = new OrganizacaoParceiraRequest(
				"12.345.678/0001-95",
				"Nexus Cloud",
				"Nexus Cloud Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				planoAtivo.getId()
		);

		MvcResult created = mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(validRequest)))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
		String orgId = json.get("id").asText();

		mockMvc.perform(patch("/api/v1/organizacoes/" + orgId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.REATIVAR))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(422))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

}
