package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.enums.AcaoCredenciamento;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(
		@NotNull(message = "A acao de credenciamento e obrigatoria.")
		AcaoCredenciamento acao
) {
}
