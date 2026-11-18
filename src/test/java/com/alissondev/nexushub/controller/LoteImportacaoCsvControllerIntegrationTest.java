package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.entity.LoteImportacaoCsv;
import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import com.alissondev.nexushub.dto.LoteImportacaoCsvResponse;
import com.alissondev.nexushub.exception.ConflictException;
import com.alissondev.nexushub.repository.LoteImportacaoCsvRepository;
import com.alissondev.nexushub.service.LoteImportacaoCsvService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoteImportacaoCsvControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoteImportacaoCsvService loteService;

	@Autowired
	private LoteImportacaoCsvRepository loteRepository;

	@Test
	@DisplayName("Deve registrar novo lote CSV calculando SHA-256 e consultar por ID com HTTP 200 OK")
	void shouldRegisterBatchAndGetById() throws Exception {
		byte[] conteudo = "cnpj,nome_fantasia\n11222333000181,FinPulse".getBytes(StandardCharsets.UTF_8);

		LoteImportacaoCsvResponse criado = loteService.registrarNovoLote("onboarding-finpulse.csv", conteudo);

		assertThat(criado.id()).isNotNull();
		assertThat(criado.hashSha256()).hasSize(64);
		assertThat(criado.status()).isEqualTo(StatusLoteCsv.RECEBIDO);

		mockMvc.perform(get("/api/v1/lotes-csv/" + criado.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(criado.id().toString()))
				.andExpect(jsonPath("$.nomeArquivo").value("onboarding-finpulse.csv"))
				.andExpect(jsonPath("$.hashSha256").value(criado.hashSha256()))
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.totalLinhas").value(0))
				.andExpect(jsonPath("$.linhasSucesso").value(0))
				.andExpect(jsonPath("$.linhasErro").value(0));
	}

	@Test
	@DisplayName("Deve rejeitar lote com mesmo conteudo (SHA-256 duplicado) lancando ConflictException")
	void shouldRejectDuplicateSha256BatchWithConflictException() {
		byte[] conteudoIdempotente = "cnpj,nome_fantasia\n11444777000161,AgroSense".getBytes(StandardCharsets.UTF_8);

		loteService.registrarNovoLote("lote-1.csv", conteudoIdempotente);

		assertThatThrownBy(() -> loteService.registrarNovoLote("lote-1-reenvio.csv", conteudoIdempotente))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	@DisplayName("Deve retornar HTTP 404 Not Found ao consultar lote CSV inexistente")
	void shouldReturn404WhenBatchIdDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/v1/lotes-csv/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("Deve listar lotes paginados filtrando por status e limitando size em 50")
	void shouldListPaginatedBatchesFilteredByStatusAndCapSizeAt50() throws Exception {
		LoteImportacaoCsvResponse recebido = loteService.registrarNovoLote(
				"lote-recebido.csv",
				"conteudo-recebido".getBytes(StandardCharsets.UTF_8)
		);

		LoteImportacaoCsvResponse concluidoResp = loteService.registrarNovoLote(
				"lote-concluido.csv",
				"conteudo-concluido".getBytes(StandardCharsets.UTF_8)
		);
		LoteImportacaoCsv concluidoEntity = loteRepository.findById(concluidoResp.id()).orElseThrow();
		concluidoEntity.iniciarProcessamento(1);
		concluidoEntity.registrarSucesso();
		concluidoEntity.finalizarProcessamento();
		loteRepository.saveAndFlush(concluidoEntity);

		mockMvc.perform(get("/api/v1/lotes-csv")
						.param("status", "CONCLUIDO")
						.param("page", "0")
						.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(concluidoResp.id().toString()))
				.andExpect(jsonPath("$.content[0].status").value("CONCLUIDO"));

		mockMvc.perform(get("/api/v1/lotes-csv").param("status", "RECEBIDO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(recebido.id().toString()));
	}

}
