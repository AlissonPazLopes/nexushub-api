package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import com.alissondev.nexushub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteImportacaoCsvTest {

	private static final String SAMPLE_HASH = "a".repeat(64);

	@Test
	@DisplayName("Deve iniciar lote com status RECEBIDO, contadores zerados e iniciadoEm preenchido")
	void shouldInitializeBatchWithRecebidoStatusAndZeroCounters() {
		LoteImportacaoCsv lote = new LoteImportacaoCsv("  parceiros-2026.csv  ", SAMPLE_HASH.toUpperCase());

		assertThat(lote.getNomeArquivo()).isEqualTo("parceiros-2026.csv");
		assertThat(lote.getHashSha256()).isEqualTo(SAMPLE_HASH);
		assertThat(lote.getStatus()).isEqualTo(StatusLoteCsv.RECEBIDO);
		assertThat(lote.getTotalLinhas()).isZero();
		assertThat(lote.getLinhasSucesso()).isZero();
		assertThat(lote.getLinhasErro()).isZero();
		assertThat(lote.getResumoErros()).isNull();
		assertThat(lote.getIniciadoEm()).isNotNull();
		assertThat(lote.getFinalizadoEm()).isNull();
	}

	@Test
	@DisplayName("Deve transitar RECEBIDO -> PROCESSANDO -> CONCLUIDO quando nao houver linhas com erro")
	void shouldTransitionToConcluidoWhenAllLinesSucceed() {
		LoteImportacaoCsv lote = new LoteImportacaoCsv("lote-limpo.csv", SAMPLE_HASH);

		lote.iniciarProcessamento(2);
		assertThat(lote.getStatus()).isEqualTo(StatusLoteCsv.PROCESSANDO);
		assertThat(lote.getTotalLinhas()).isEqualTo(2);

		lote.registrarSucesso();
		lote.registrarSucesso();
		lote.finalizarProcessamento();

		assertThat(lote.getStatus()).isEqualTo(StatusLoteCsv.CONCLUIDO);
		assertThat(lote.getLinhasSucesso()).isEqualTo(2);
		assertThat(lote.getLinhasErro()).isZero();
		assertThat(lote.getFinalizadoEm()).isNotNull();
	}

	@Test
	@DisplayName("Deve transitar RECEBIDO -> PROCESSANDO -> CONCLUIDO_COM_ERROS acumulando resumoErros")
	void shouldTransitionToConcluidoComErrosAndAccumulateErrorSummary() {
		LoteImportacaoCsv lote = new LoteImportacaoCsv("lote-misto.csv", SAMPLE_HASH);

		lote.iniciarProcessamento(3);
		lote.registrarSucesso();
		lote.registrarErro(2, "CNPJ invalido");
		lote.registrarErro(3, "Plano inexistente");
		lote.finalizarProcessamento();

		assertThat(lote.getStatus()).isEqualTo(StatusLoteCsv.CONCLUIDO_COM_ERROS);
		assertThat(lote.getLinhasSucesso()).isEqualTo(1);
		assertThat(lote.getLinhasErro()).isEqualTo(2);
		assertThat(lote.getResumoErros())
				.contains("Linha 2: CNPJ invalido")
				.contains("Linha 3: Plano inexistente");
		assertThat(lote.getFinalizadoEm()).isNotNull();
	}

	@Test
	@DisplayName("Deve permitir registrarFalhaCritica definindo status FALHA e finalizadoEm")
	void shouldTransitionToFalhaOnCriticalFailure() {
		LoteImportacaoCsv lote = new LoteImportacaoCsv("lote-corrompido.csv", SAMPLE_HASH);
		lote.iniciarProcessamento(10);

		lote.registrarFalhaCritica("Cabecalho CSV ausente ou malformado");

		assertThat(lote.getStatus()).isEqualTo(StatusLoteCsv.FALHA);
		assertThat(lote.getResumoErros()).contains("Cabecalho CSV ausente ou malformado");
		assertThat(lote.getFinalizadoEm()).isNotNull();
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException em transicoes de estado invalidas")
	void shouldThrowBusinessRuleExceptionOnInvalidStateTransitions() {
		LoteImportacaoCsv lote = new LoteImportacaoCsv("lote.csv", SAMPLE_HASH);

		assertThatThrownBy(lote::registrarSucesso).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> lote.registrarErro(1, "erro")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(lote::finalizarProcessamento).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> lote.iniciarProcessamento(-1)).isInstanceOf(BusinessRuleException.class);

		lote.iniciarProcessamento(1);
		assertThatThrownBy(() -> lote.iniciarProcessamento(1)).isInstanceOf(BusinessRuleException.class);

		lote.registrarSucesso();
		lote.finalizarProcessamento();

		assertThatThrownBy(lote::registrarSucesso).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> lote.registrarFalhaCritica("falha tardia")).isInstanceOf(BusinessRuleException.class);
	}

}
