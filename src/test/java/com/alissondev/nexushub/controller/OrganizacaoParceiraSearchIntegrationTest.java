package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizacaoParceiraSearchIntegrationTest {

	private static final int[] WEIGHTS_FIRST = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PlanoCredenciamentoRepository planoRepository;

	@Autowired
	private OrganizacaoParceiraRepository organizacaoRepository;

	private PlanoCredenciamento planoSeed;
	private PlanoCredenciamento planoGrowth;

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
				10,
				25
		));
	}

	@Test
	@DisplayName("Deve filtrar organizacoes por verticalTech, status, planoId e nome parcial sem diferenciar maiusculas")
	void shouldFilterOrganizationsWithCombinedCriteria() throws Exception {
		LocalDateTime base = LocalDateTime.of(2026, 11, 15, 10, 0);

		OrganizacaoParceira alphaFin = salvarOrg("11222333000181", "Alpha FinPay", VerticalTech.FINTECH, planoSeed, base, true);
		salvarOrg("11444777000161", "Alpha Sensors", VerticalTech.IOT_HARDWARE, planoSeed, base.plusMinutes(5), false);
		salvarOrg("12345678000195", "Alpha DataOps", VerticalTech.FINTECH, planoGrowth, base.plusMinutes(10), true);
		salvarOrg("45997418000153", "Beta FinCore", VerticalTech.FINTECH, planoSeed, base.plusMinutes(15), true);

		mockMvc.perform(get("/api/v1/organizacoes")
						.param("verticalTech", "FINTECH")
						.param("status", "ATIVA")
						.param("planoId", planoSeed.getId().toString())
						.param("nome", "aLpHa"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(alphaFin.getId().toString()))
				.andExpect(jsonPath("$.content[0].nomeFantasia").value("Alpha FinPay"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20));

		mockMvc.perform(get("/api/v1/organizacoes").param("nome", "ALPHA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	@DisplayName("Deve paginar resultados ordenando por criadoEm decrescente por padrao")
	void shouldPaginateAndSortByCriadoEmDescByDefault() throws Exception {
		LocalDateTime base = LocalDateTime.of(2026, 11, 15, 12, 0);

		salvarOrg("11222333000181", "Primeira Criada", VerticalTech.AGRITECH, planoSeed, base.minusMinutes(20), false);
		salvarOrg("11444777000161", "Segunda Criada", VerticalTech.AI_DATA, planoSeed, base.minusMinutes(10), false);
		salvarOrg("12345678000195", "Terceira Criada", VerticalTech.DEVTOOLS, planoSeed, base, false);

		mockMvc.perform(get("/api/v1/organizacoes")
						.param("page", "0")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].nomeFantasia").value("Terceira Criada"))
				.andExpect(jsonPath("$.content[1].nomeFantasia").value("Segunda Criada"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/v1/organizacoes")
						.param("page", "1")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].nomeFantasia").value("Primeira Criada"))
				.andExpect(jsonPath("$.page").value(1));
	}

	@Test
	@DisplayName("Deve limitar o tamanho maximo da pagina em 50 itens mesmo quando size maior for solicitado")
	void shouldCapPageSizeAt50() throws Exception {
		LocalDateTime base = LocalDateTime.of(2026, 11, 15, 14, 0);
		for (int i = 1; i <= 55; i++) {
			String cnpj = gerarCnpjValido(i);
			salvarOrg(cnpj, "Startup Lote " + i, VerticalTech.AI_DATA, planoSeed, base.plusSeconds(i), false);
		}

		mockMvc.perform(get("/api/v1/organizacoes")
						.param("page", "0")
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50))
				.andExpect(jsonPath("$.content", hasSize(50)))
				.andExpect(jsonPath("$.totalElements").value(55))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	private OrganizacaoParceira salvarOrg(
			String cnpj,
			String nomeFantasia,
			VerticalTech vertical,
			PlanoCredenciamento plano,
			LocalDateTime criadoEm,
			boolean ativar
	) {
		OrganizacaoParceira org = new OrganizacaoParceira(
				cnpj,
				nomeFantasia,
				nomeFantasia + " S.A.",
				"contato@hub.io",
				vertical,
				plano
		);
		org.setCriadoEm(criadoEm);
		if (ativar) {
			org.ativar();
		}
		return organizacaoRepository.saveAndFlush(org);
	}

	private String gerarCnpjValido(int sequencia) {
		String base = String.format("%08d0001", sequencia);
		int d1 = calcularDigito(base, WEIGHTS_FIRST);
		int d2 = calcularDigito(base + d1, WEIGHTS_SECOND);
		return base + d1 + d2;
	}

	private int calcularDigito(String digits, int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += (digits.charAt(i) - '0') * weights[i];
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

}
