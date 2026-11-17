package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;

import java.time.Instant;
import java.util.UUID;

public record ResgateBeneficioResponse(
		UUID beneficioId,
		String titulo,
		Integer cotaUtilizada,
		Integer cotaDisponivel,
		StatusBeneficio status,
		Long version,
		Instant resgatadoEm
) {

	public static ResgateBeneficioResponse fromEntity(BeneficioRede entity) {
		return new ResgateBeneficioResponse(
				entity.getId(),
				entity.getTitulo(),
				entity.getCotaUtilizada(),
				entity.cotaDisponivel(),
				entity.getStatus(),
				entity.getVersion(),
				Instant.now()
		);
	}

}
