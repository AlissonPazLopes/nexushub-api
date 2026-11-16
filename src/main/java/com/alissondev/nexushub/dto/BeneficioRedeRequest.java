package com.alissondev.nexushub.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record BeneficioRedeRequest(
		@NotNull(message = "O ID da organizacao ofertante e obrigatorio.")
		UUID organizacaoOfertanteId,

		@NotNull(message = "O ID do plano minimo e obrigatorio.")
		UUID planoMinimoId,

		@NotBlank(message = "O titulo do beneficio e obrigatorio.")
		@Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres.")
		String titulo,

		@NotBlank(message = "A descricao do beneficio e obrigatoria.")
		String descricao,

		@NotNull(message = "A cota total do beneficio e obrigatoria.")
		@Positive(message = "A cota total deve ser maior que zero.")
		Integer cotaTotal,

		@NotNull(message = "A data de validade e obrigatoria.")
		@Future(message = "A data de validade deve estar no futuro.")
		LocalDate validoAte
) {
}
