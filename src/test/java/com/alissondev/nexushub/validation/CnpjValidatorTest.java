package com.alissondev.nexushub.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

	private final CnpjValidator validator = new CnpjValidator();

	@Test
	@DisplayName("Deve validar CNPJ valido sem mascara e com mascara")
	void shouldAcceptValidCnpjWithAndWithoutMask() {
		assertThat(CnpjUtils.isValid("11222333000181")).isTrue();
		assertThat(CnpjUtils.isValid("11.222.333/0001-81")).isTrue();
		assertThat(validator.isValid("11222333000181", null)).isTrue();
		assertThat(validator.isValid("11.222.333/0001-81", null)).isTrue();
	}

	@Test
	@DisplayName("Deve sanitizar CNPJ removendo pontuacao e mantendo apenas os 14 digitos")
	void shouldSanitizeMaskedCnpj() {
		assertThat(CnpjUtils.sanitize("11.222.333/0001-81")).isEqualTo("11222333000181");
		assertThat(CnpjUtils.sanitize(null)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"11222333000182", "11222333000191", "11.222.333/0001-80"})
	@DisplayName("Deve rejeitar CNPJ com digito verificador incorreto")
	void shouldRejectCnpjWithInvalidCheckDigits(String invalidCnpj) {
		assertThat(CnpjUtils.isValid(invalidCnpj)).isFalse();
		assertThat(validator.isValid(invalidCnpj, null)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"00000000000000",
			"11111111111111",
			"22.222.222/2222-22",
			"99999999999999"
	})
	@DisplayName("Deve rejeitar CNPJ com 14 digitos repetidos")
	void shouldRejectRepeatedDigitCnpjs(String repeatedCnpj) {
		assertThat(CnpjUtils.isValid(repeatedCnpj)).isFalse();
		assertThat(validator.isValid(repeatedCnpj, null)).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "1234567890", "112223330001819"})
	@DisplayName("Deve rejeitar CNPJ nulo, vazio ou com tamanho invalido")
	void shouldRejectNullEmptyOrInvalidLengthCnpj(String invalidInput) {
		assertThat(CnpjUtils.isValid(invalidInput)).isFalse();
		assertThat(validator.isValid(invalidInput, null)).isFalse();
	}

}
