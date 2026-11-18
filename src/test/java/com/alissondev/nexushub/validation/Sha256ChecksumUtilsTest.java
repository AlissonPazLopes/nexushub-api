package com.alissondev.nexushub.validation;

import com.alissondev.nexushub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256ChecksumUtilsTest {

	@Test
	@DisplayName("Deve gerar hash SHA-256 deterministico com 64 caracteres hexadecimais minusculos")
	void shouldGenerateDeterministic64CharHexSha256() {
		byte[] payload = "cnpj,nome_fantasia\n11222333000181,Nexus Cloud".getBytes(StandardCharsets.UTF_8);

		String hash1 = Sha256ChecksumUtils.calcularHex(payload);
		String hash2 = Sha256ChecksumUtils.calcularHex(payload);

		assertThat(hash1)
				.hasSize(64)
				.matches("^[0-9a-f]{64}$")
				.isEqualTo(hash2);
	}

	@Test
	@DisplayName("Deve produzir hashes distintos para conteudos de arquivo diferentes")
	void shouldGenerateDistinctHashesForDifferentPayloads() {
		byte[] payloadA = "linha1".getBytes(StandardCharsets.UTF_8);
		byte[] payloadB = "linha2".getBytes(StandardCharsets.UTF_8);

		String hashA = Sha256ChecksumUtils.calcularHex(payloadA);
		String hashB = Sha256ChecksumUtils.calcularHex(payloadB);

		assertThat(hashA).isNotEqualTo(hashB);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando conteudo for nulo ou vazio")
	void shouldThrowBusinessRuleExceptionWhenPayloadIsNullOrEmpty() {
		assertThatThrownBy(() -> Sha256ChecksumUtils.calcularHex(null))
				.isInstanceOf(BusinessRuleException.class);

		assertThatThrownBy(() -> Sha256ChecksumUtils.calcularHex(new byte[0]))
				.isInstanceOf(BusinessRuleException.class);
	}

}
