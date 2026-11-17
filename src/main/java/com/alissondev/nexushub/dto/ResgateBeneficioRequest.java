package com.alissondev.nexushub.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResgateBeneficioRequest(
		@NotNull(message = "O ID da organizacao resgatante e obrigatorio.")
		UUID organizacaoResgatanteId
) {
}
