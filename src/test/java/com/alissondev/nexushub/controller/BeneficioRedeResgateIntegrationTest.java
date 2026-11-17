package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.ResgateBeneficioRequest;
import com.alissondev.nexushub.repository.BeneficioRedeRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BeneficioRedeResgateIntegrationTest {

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
	private PlanoCredenciamento planoGrowth;
	private OrganizacaoParceira ofertanteGrowth;
	private OrganizacaoParceira resgatanteGrowth;
	private OrganizacaoParceira resgatanteSeed;
	private OrganizacaoParceira resgatantePendente;
	private BeneficioRede beneficioGrowth;

	@BeforeEach
	void setUp() {
		planoSeed = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"SEED",
				"Plano Seed",
				new BigDecimal("199.00"),
				3,
				10
		));
		planoGrowth = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth",
				new BigDecimal("499.00"),
				5,
				20
		));

		ofertanteGrowth = criarOrganizacao("11222333000181", "FinPulse Ofertante", planoGrowth, true);
		resgatanteGrowth = criarOrganizacao("11444777000161", "Nexus Cloud Resgatante", planoGrowth, true);
		resgatanteSeed = criarOrganizacao("12345678000195", "AgroSense Seed", planoSeed, true);
		resgatantePendente = criarOrganizacao("45997418000153", "Pending Lab", planoGrowth, false);

		beneficioGrowth = beneficioRepository.saveAndFlush(new BeneficioRede(
				ofertanteGrowth,
				planoGrowth,
				"Voucher Infraestrutura Dedicada",
				"Acesso dedicado para parceiros Growth+",
				2,
				LocalDate.now().plusDays(30)
		));
	}

	private OrganizacaoParceira criarOrganizacao(
			String cnpj,
			String nomeFantasia,
			PlanoCredenciamento plano,
			boolean ativa
	) {
		OrganizacaoParceira org = new OrganizacaoParceira(
				cnpj,
				nomeFantasia,
				nomeFantasia + " Ltda",
				"contato@hub.io",
				VerticalTech.DEVTOOLS,
				plano
		);
		if (ativa) {
			org.ativar();
		}
		return organizacaoRepository.saveAndFlush(org);
	}

	@Test
	@DisplayName("Deve resgatar beneficio com HTTP 200 atualizando cotaUtilizada, cotaDisponivel, status e version")
	void shouldRedeemBenefitReturning200WithUpdatedQuotaAndVersion() throws Exception {
		ResgateBeneficioRequest request = new ResgateBeneficioRequest(resgatanteGrowth.getId());

		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.beneficioId").value(beneficioGrowth.getId().toString()))
				.andExpect(jsonPath("$.titulo").value("Voucher Infraestrutura Dedicada"))
				.andExpect(jsonPath("$.cotaUtilizada").value(1))
				.andExpect(jsonPath("$.cotaDisponivel").value(1))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.resgatadoEm").isNotEmpty());

		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cotaUtilizada").value(2))
				.andExpect(jsonPath("$.cotaDisponivel").value(0))
				.andExpect(jsonPath("$.status").value("ESGOTADO"))
				.andExpect(jsonPath("$.version").value(2));
	}

	@Test
	@DisplayName("Deve retornar HTTP 422 para tier insuficiente, auto-resgate, resgatante pendente ou beneficio esgotado")
	void shouldReturn422ForBusinessRuleViolations() throws Exception {
		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResgateBeneficioRequest(resgatanteSeed.getId()))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));

		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResgateBeneficioRequest(ofertanteGrowth.getId()))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));

		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResgateBeneficioRequest(resgatantePendente.getId()))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));
	}

	@Test
	@DisplayName("Deve retornar HTTP 404 quando beneficio ou organizacao resgatante nao existir")
	void shouldReturn404WhenBenefitOrRedeemerNotFound() throws Exception {
		mockMvc.perform(post("/api/v1/beneficios/" + UUID.randomUUID() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResgateBeneficioRequest(resgatanteGrowth.getId()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));

		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResgateBeneficioRequest(UUID.randomUUID()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Deve retornar HTTP 400 quando o corpo da requisicao nao informar organizacaoResgatanteId")
	void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
		mockMvc.perform(post("/api/v1/beneficios/" + beneficioGrowth.getId() + "/resgatar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

}
