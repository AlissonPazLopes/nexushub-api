package com.alissondev.nexushub.concurrency;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.ResgateBeneficioRequest;
import com.alissondev.nexushub.repository.BeneficioRedeRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeneficioRedeConcorrenciaTest {

	private static final int TOTAL_THREADS = 20;
	private static final int COTA_TOTAL = 5;
	private static final int[] WEIGHTS_FIRST = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

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

	private UUID beneficioId;
	private final List<UUID> resgatantesIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		limparBase();

		PlanoCredenciamento plano = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"GROWTH_CONC",
				"Plano Growth Concorrencia",
				new BigDecimal("499.00"),
				10,
				50
		));

		OrganizacaoParceira ofertante = new OrganizacaoParceira(
				"11222333000181",
				"Ofertante Concorrencia",
				"Ofertante Concorrencia S.A.",
				"ofertante@hub.io",
				VerticalTech.DEVTOOLS,
				plano
		);
		ofertante.ativar();
		ofertante = organizacaoRepository.saveAndFlush(ofertante);

		BeneficioRede beneficio = beneficioRepository.saveAndFlush(new BeneficioRede(
				ofertante,
				plano,
				"Cota Disputada Concorrentemente",
				"Apenas 5 unidades disponiveis para 20 requisicoes simultaneas.",
				COTA_TOTAL,
				LocalDate.now().plusDays(30)
		));
		beneficioId = beneficio.getId();

		resgatantesIds.clear();
		for (int i = 1; i <= TOTAL_THREADS; i++) {
			OrganizacaoParceira resgatante = new OrganizacaoParceira(
					gerarCnpjValido(200 + i),
					"Resgatante " + i,
					"Resgatante " + i + " Ltda",
					"resgatante" + i + "@hub.io",
					VerticalTech.FINTECH,
					plano
			);
			resgatante.ativar();
			resgatantesIds.add(organizacaoRepository.saveAndFlush(resgatante).getId());
		}
	}

	@AfterEach
	void tearDown() {
		limparBase();
	}

	private void limparBase() {
		beneficioRepository.deleteAll();
		organizacaoRepository.deleteAll();
		planoRepository.deleteAll();
	}

	@Test
	@DisplayName("Deve impedir over-claim de cota sob 20 resgates simultaneos com Optimistic Locking (@Version)")
	void shouldPreventQuotaOverClaimUnderParallelRedeems() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREADS);
		CountDownLatch readyLatch = new CountDownLatch(TOTAL_THREADS);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(TOTAL_THREADS);

		AtomicInteger sucessos200 = new AtomicInteger(0);
		AtomicInteger conflitos409 = new AtomicInteger(0);
		AtomicInteger rejeicoes422 = new AtomicInteger(0);
		AtomicInteger outrosStatus = new AtomicInteger(0);

		try {
			for (int i = 0; i < TOTAL_THREADS; i++) {
				final UUID resgatanteId = resgatantesIds.get(i);
				executor.submit(() -> {
					readyLatch.countDown();
					try {
						startLatch.await();
						String payload = objectMapper.writeValueAsString(new ResgateBeneficioRequest(resgatanteId));
						MvcResult result = mockMvc.perform(post("/api/v1/beneficios/" + beneficioId + "/resgatar")
										.contentType(MediaType.APPLICATION_JSON)
										.content(payload))
								.andReturn();

						int status = result.getResponse().getStatus();
						if (status == 200) {
							sucessos200.incrementAndGet();
						} else if (status == 409) {
							conflitos409.incrementAndGet();
						} else if (status == 422) {
							rejeicoes422.incrementAndGet();
						} else {
							outrosStatus.incrementAndGet();
						}
					} catch (Exception e) {
						outrosStatus.incrementAndGet();
					} finally {
						doneLatch.countDown();
					}
				});
			}

			assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
			startLatch.countDown();
			assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
		} finally {
			executor.shutdownNow();
		}

		BeneficioRede estadoFinal = beneficioRepository.findById(beneficioId).orElseThrow();

		assertThat(outrosStatus.get()).isZero();
		assertThat(sucessos200.get() + conflitos409.get() + rejeicoes422.get()).isEqualTo(TOTAL_THREADS);
		assertThat(sucessos200.get()).isBetween(1, COTA_TOTAL);
		assertThat(estadoFinal.getCotaUtilizada()).isEqualTo(sucessos200.get());
		assertThat(estadoFinal.getCotaUtilizada()).isLessThanOrEqualTo(COTA_TOTAL);
		assertThat(estadoFinal.getVersion()).isEqualTo((long) sucessos200.get());
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
