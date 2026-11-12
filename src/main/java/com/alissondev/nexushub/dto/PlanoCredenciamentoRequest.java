package com.alissondev.nexushub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanoCredenciamentoRequest(
		@NotBlank(message = "O codigo do plano e obrigatorio.")
		@Size(max = 50, message = "O codigo do plano deve ter no maximo 50 caracteres.")
		String codigo,

		@NotBlank(message = "O nome do plano e obrigatorio.")
		@Size(max = 120, message = "O nome do plano deve ter no maximo 120 caracteres.")
		String nome,

		@NotNull(message = "O valor da mensalidade e obrigatorio.")
		@PositiveOrZero(message = "O valor da mensalidade nao pode ser negativo.")
		BigDecimal valorMensalidade,

		@NotNull(message = "O limite maximo de beneficios ofertados e obrigatorio.")
		@Positive(message = "O limite maximo de beneficios ofertados deve ser maior que zero.")
		Integer maxBeneficiosOfertados,

		@NotNull(message = "A cota mensal de resgates e obrigatoria.")
		@Positive(message = "A cota mensal de resgates deve ser maior que zero.")
		Integer cotaResgatesMensal
) {
}
