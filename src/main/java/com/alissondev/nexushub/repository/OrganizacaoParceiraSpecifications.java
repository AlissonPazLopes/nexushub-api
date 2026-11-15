package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public final class OrganizacaoParceiraSpecifications {

	private OrganizacaoParceiraSpecifications() {
	}

	public static Specification<OrganizacaoParceira> comVerticalTech(VerticalTech verticalTech) {
		return (root, query, cb) ->
				verticalTech == null ? null : cb.equal(root.get("verticalTech"), verticalTech);
	}

	public static Specification<OrganizacaoParceira> comStatus(StatusOrganizacao status) {
		return (root, query, cb) ->
				status == null ? null : cb.equal(root.get("status"), status);
	}

	public static Specification<OrganizacaoParceira> comPlanoId(UUID planoId) {
		return (root, query, cb) ->
				planoId == null ? null : cb.equal(root.get("plano").get("id"), planoId);
	}

	public static Specification<OrganizacaoParceira> comNomeFantasiaContendo(String nome) {
		return (root, query, cb) -> {
			if (nome == null || nome.isBlank()) {
				return null;
			}
			String pattern = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
			return cb.like(cb.lower(root.get("nomeFantasia")), pattern);
		};
	}

	public static Specification<OrganizacaoParceira> comFiltros(
			VerticalTech verticalTech,
			StatusOrganizacao status,
			UUID planoId,
			String nome
	) {
		Specification<OrganizacaoParceira> fetchPlano = (root, query, cb) -> {
			if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
				root.fetch("plano", JoinType.LEFT);
			}
			return null;
		};

		return Specification.where(fetchPlano)
				.and(comVerticalTech(verticalTech))
				.and(comStatus(status))
				.and(comPlanoId(planoId))
				.and(comNomeFantasiaContendo(nome));
	}

}
