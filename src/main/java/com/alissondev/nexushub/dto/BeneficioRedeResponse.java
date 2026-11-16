package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BeneficioRedeResponse(
		UUID id,
		UUID organizacaoOfertanteId,
		String organizacaoOfertanteNome,
		UUID planoMinimoId,
		String planoMinimoCodigo,
		String titulo,
		String descricao,
		Integer cotaTotal,
		Integer cotaUtilizada,
		Integer cotaDisponivel,
		LocalDate validoAte,
		StatusBeneficio status,
		Long version,
		LocalDateTime criadoEm
) {

	public static BeneficioRedeResponse fromEntity(BeneficioRede entity) {
		return new BeneficioRedeResponse(
				entity.getId(),
				entity.getOrganizacaoOfertante() != null ? entity.getOrganizacaoOfertante().getId() : null,
				entity.getOrganizacaoOfertante() != null ? entity.getOrganizacaoOfertante().getNomeFantasia() : null,
				entity.getPlanoMinimo() != null ? entity.getPlanoMinimo().getId() : null,
				entity.getPlanoMinimo() != null ? entity.getPlanoMinimo().getCodigo() : null,
				entity.getTitulo(),
				entity.getDescricao(),
				entity.getCotaTotal(),
				entity.getCotaUtilizada(),
				entity.cotaDisponivel(),
				entity.getValidoAte(),
				entity.getStatus(),
				entity.getVersion(),
				entity.getCriadoEm()
		);
	}

}
