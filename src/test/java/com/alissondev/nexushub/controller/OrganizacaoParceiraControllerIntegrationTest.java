package com.alissondev.nexushub.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizacaoParceiraControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanoCredenciamentoRepository planoRepository;

	private PlanoCredenciamento planoAtivo;

	@BeforeEach
	void setUp() {
		planoAtivo = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth Hub",
				new BigDecimal("499.00"),
				10,
				25
		));
	}

	private OrganizacaoParceiraRequest criarRequest(String cnpj, String nomeFantasia) {
		return new OrganizacaoParceiraRequest(
				cnpj,
				nomeFantasia,
				nomeFantasia + " Tecnologia Ltda",
				"contato@empresa.io",
				VerticalTech.FINTECH,
				planoAtivo.getId()
		);
	}

	private String cadastrarViaApi(String cnpj, String nomeFantasia) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(criarRequest(cnpj, nomeFantasia))))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		return json.get("id").asText();
	}

	@Test
	@DisplayName("Deve credenciar organizacao retornando HTTP 201, header Location e CNPJ sanitizado")
	void shouldRegisterOrganizationReturning201AndLocationHeader() throws Exception {
		OrganizacaoParceiraRequest request = criarRequest("11.222.333/0001-81", "FinPulse B2B");

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".+/api/v1/organizacoes/[0-9a-fA-F-]+")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.cnpj").value("11222333000181"))
				.andExpect(jsonPath("$.nomeFantasia").value("FinPulse B2B"))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.planoId").value(planoAtivo.getId().toString()))
				.andExpect(jsonPath("$.planoCodigo").value("GROWTH"))
				.andExpect(jsonPath("$.saldoResgatesCiclo").value(25));
	}

	@Test
	@DisplayName("Deve retornar HTTP 409 Conflict ao tentar cadastrar organizacao com CNPJ duplicado")
	void shouldReturn409ConflictWhenCnpjAlreadyExists() throws Exception {
		cadastrarViaApi("11.222.333/0001-81", "FinPulse Original");

		OrganizacaoParceiraRequest duplicateRequest = criarRequest("11222333000181", "FinPulse Duplicada");

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(duplicateRequest)))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Deve retornar HTTP 400 Bad Request quando o CNPJ for invalido")
	void shouldReturn400BadRequestWhenCnpjIsInvalid() throws Exception {
		OrganizacaoParceiraRequest invalidCheckDigit = criarRequest("11.222.333/0001-99", "Empresa Invalida");

		mockMvc.perform(post("/api/v1/organizacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidCheckDigit)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Deve consultar organizacao por ID com 200 OK e retornar 404 para ID inexistente")
	void shouldGetByIdAndReturn404WhenNotFound() throws Exception {
		String id = cadastrarViaApi("11.444.777/0001-61", "AgroSense IoT");

		mockMvc.perform(get("/api/v1/organizacoes/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nomeFantasia").value("AgroSense IoT"));

		mockMvc.perform(get("/api/v1/organizacoes/" + UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Deve executar transicoes validas de status via PATCH /api/v1/organizacoes/{id}/status")
	void shouldExecuteValidStatusTransitions() throws Exception {
		String id = cadastrarViaApi("12.345.678/0001-95", "Nexus DevTools");

		mockMvc.perform(patch("/api/v1/organizacoes/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.ATIVAR))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVA"));

		mockMvc.perform(patch("/api/v1/organizacoes/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.SUSPENDER))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUSPENSA"));

		mockMvc.perform(patch("/api/v1/organizacoes/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.REATIVAR))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVA"));

		mockMvc.perform(patch("/api/v1/organizacoes/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.DESCREDENCIAR))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DESCREDENCIADA"));
	}

	@Test
	@DisplayName("Deve retornar HTTP 422 Unprocessable Entity ao tentar transicao invalida via PATCH")
	void shouldReturn422WhenStatusTransitionIsInvalid() throws Exception {
		String id = cadastrarViaApi("45.997.418/0001-53", "CloudOps Hub");

		mockMvc.perform(patch("/api/v1/organizacoes/" + id + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new AlterarStatusRequest(AcaoCredenciamento.SUSPENDER))))
				.andExpect(status().isUnprocessableEntity());
	}

}
