package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.BeneficioRedeRequest;
import com.alissondev.nexushub.repository.BeneficioRedeRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BeneficioRedeControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanoCredenciamentoRepository planoRepository;

	@Autowired
	private OrganizacaoParceiraRepository organizacaoRepository;

	@Autowired
	private BeneficioRedeRepository beneficioRepository;

	private PlanoCredenciamento planoSeed;
	private OrganizacaoParceira organizacaoAtiva;
	private OrganizacaoParceira organizacaoPendente;

	@BeforeEach
	void setUp() {
		planoSeed = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"SEED",
				"Plano Startup Seed",
				new BigDecimal("199.00"),
				2,
				10
		));

		OrganizacaoParceira ativa = new OrganizacaoParceira(
				"11.222.333/0001-81",
				"FinPulse",
				"FinPulse Pagamentos S.A.",
				"contato@finpulse.io",
				VerticalTech.FINTECH,
				planoSeed
		);
		ativa.ativar();
		organizacaoAtiva = organizacaoRepository.saveAndFlush(ativa);

		OrganizacaoParceira pendente = new OrganizacaoParceira(
				"11.444.777/0001-61",
				"AgroSense IoT",
				"AgroSense Hardware Ltda",
				"contato@agrosense.io",
				VerticalTech.AGRITECH,
				planoSeed
		);
		organizacaoPendente = organizacaoRepository.saveAndFlush(pendente);
	}

	private BeneficioRedeRequest criarRequest(UUID ofertanteId, String titulo, LocalDate validoAte) {
		return new BeneficioRedeRequest(
				ofertanteId,
				planoSeed.getId(),
				titulo,
				"Descricao tecnica do beneficio B2B para empresas do polo.",
				20,
				validoAte
		);
	}

	@Test
	@DisplayName("Deve publicar beneficio com HTTP 201, header Location, version inicial e cotaDisponivel")
	void shouldPublishBenefitReturning201WithVersionAndLocation() throws Exception {
		BeneficioRedeRequest request = criarRequest(
				organizacaoAtiva.getId(),
				"Creditos Gateway PIX B2B",
				LocalDate.now().plusDays(45)
		);

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".+/api/v1/beneficios/[0-9a-fA-F-]+")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.organizacaoOfertanteId").value(organizacaoAtiva.getId().toString()))
				.andExpect(jsonPath("$.organizacaoOfertanteNome").value("FinPulse"))
				.andExpect(jsonPath("$.planoMinimoId").value(planoSeed.getId().toString()))
				.andExpect(jsonPath("$.planoMinimoCodigo").value("SEED"))
				.andExpect(jsonPath("$.titulo").value("Creditos Gateway PIX B2B"))
				.andExpect(jsonPath("$.cotaTotal").value(20))
				.andExpect(jsonPath("$.cotaUtilizada").value(0))
				.andExpect(jsonPath("$.cotaDisponivel").value(20))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	@DisplayName("Deve retornar HTTP 400 Bad Request quando data de validade estiver no passado")
	void shouldReturn400WhenValidityDateIsInThePast() throws Exception {
		BeneficioRedeRequest invalidRequest = criarRequest(
				organizacaoAtiva.getId(),
				"Beneficio Vencido",
				LocalDate.now().minusDays(1)
		);

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors[0].field").value("validoAte"));
	}

	@Test
	@DisplayName("Deve retornar HTTP 422 Unprocessable Entity ao ultrapassar maxBeneficiosOfertados do plano")
	void shouldReturn422WhenPlanMaxBenefitsLimitIsExceeded() throws Exception {
		BeneficioRedeRequest primeiro = criarRequest(
				organizacaoAtiva.getId(),
				"Beneficio 1",
				LocalDate.now().plusDays(15)
		);
		BeneficioRedeRequest segundo = criarRequest(
				organizacaoAtiva.getId(),
				"Beneficio 2",
				LocalDate.now().plusDays(20)
		);
		BeneficioRedeRequest terceiro = criarRequest(
				organizacaoAtiva.getId(),
				"Beneficio 3 Acima do Limite",
				LocalDate.now().plusDays(25)
		);

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(primeiro)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(segundo)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(terceiro)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));
	}

	@Test
	@DisplayName("Deve retornar HTTP 422 Unprocessable Entity quando organizacao ofertante estiver PENDENTE")
	void shouldReturn422WhenOfferingOrganizationIsPending() throws Exception {
		BeneficioRedeRequest request = criarRequest(
				organizacaoPendente.getId(),
				"Consultoria IoT",
				LocalDate.now().plusDays(30)
		);

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));
	}

	@Test
	@DisplayName("Deve consultar beneficio por ID com 200 OK e retornar 404 quando inexistente")
	void shouldGetByIdAndReturn404WhenNotFound() throws Exception {
		BeneficioRedeRequest request = criarRequest(
				organizacaoAtiva.getId(),
				"Workshop Open Finance",
				LocalDate.now().plusDays(30)
		);

		MvcResult created = mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
		String beneficioId = json.get("id").asText();

		mockMvc.perform(get("/api/v1/beneficios/" + beneficioId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(beneficioId))
				.andExpect(jsonPath("$.titulo").value("Workshop Open Finance"));

		mockMvc.perform(get("/api/v1/beneficios/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Deve listar catalogo paginado apenas com beneficios ATIVOS vigentes e limitar size em 50")
	void shouldListPaginatedCatalogWithOnlyActiveValidBenefitsAndCapSizeAt50() throws Exception {
		BeneficioRedeRequest request = criarRequest(
				organizacaoAtiva.getId(),
				"Credito Sandbox API",
				LocalDate.now().plusDays(30)
		);

		mockMvc.perform(post("/api/v1/beneficios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		BeneficioRede expirado = new BeneficioRede(
				organizacaoAtiva,
				planoSeed,
				"Beneficio Ja Expirado",
				"Nao deve aparecer no catalogo vigente.",
				10,
				LocalDate.now().minusDays(2)
		);
		beneficioRepository.saveAndFlush(expirado);

		BeneficioRede esgotado = new BeneficioRede(
				organizacaoAtiva,
				planoSeed,
				"Beneficio Esgotado",
				"Tambem nao deve aparecer no catalogo ativo.",
				5,
				LocalDate.now().plusDays(10)
		);
		esgotado.setCotaUtilizada(5);
		esgotado.setStatus(StatusBeneficio.ESGOTADO);
		beneficioRepository.saveAndFlush(esgotado);

		mockMvc.perform(get("/api/v1/beneficios")
						.param("page", "0")
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].titulo").value("Credito Sandbox API"));
	}

}
