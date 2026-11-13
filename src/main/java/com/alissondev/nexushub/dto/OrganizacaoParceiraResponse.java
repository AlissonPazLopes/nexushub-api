package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizacaoParceiraResponse(
		UUID id,
		String cnpj,
		String nomeFantasia,
		String razaoSocial,
		String emailContato,
		VerticalTech verticalTech,
		StatusOrganizacao status,
		UUID planoId,
		String planoCodigo,
		UUID loteOrigemId,
		Integer saldoResgatesCiclo,
		LocalDateTime criadoEm
) {

	public static OrganizacaoParceiraResponse fromEntity(OrganizacaoParceira entity) {
		return new OrganizacaoParceiraResponse(
				entity.getId(),
				entity.getCnpj(),
				entity.getNomeFantasia(),
				entity.getRazaoSocial(),
				entity.getEmailContato(),
				entity.getVerticalTech(),
				entity.getStatus(),
				entity.getPlano() != null ? entity.getPlano().getId() : null,
				entity.getPlano() != null ? entity.getPlano().getCodigo() : null,
				entity.getLoteOrigemId(),
				entity.getSaldoResgatesCiclo(),
				entity.getCriadoEm()
		);
	}

}
