package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlanoCredenciamentoResponse(
		UUID id,
		String codigo,
		String nome,
		BigDecimal valorMensalidade,
		Integer maxBeneficiosOfertados,
		Integer cotaResgatesMensal,
		Boolean ativo,
		LocalDateTime criadoEm
) {

	public static PlanoCredenciamentoResponse fromEntity(PlanoCredenciamento entity) {
		return new PlanoCredenciamentoResponse(
				entity.getId(),
				entity.getCodigo(),
				entity.getNome(),
				entity.getValorMensalidade(),
				entity.getMaxBeneficiosOfertados(),
				entity.getCotaResgatesMensal(),
				entity.getAtivo(),
				entity.getCriadoEm()
		);
	}

}
