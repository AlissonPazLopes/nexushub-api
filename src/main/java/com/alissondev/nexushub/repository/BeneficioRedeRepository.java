package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BeneficioRedeRepository extends JpaRepository<BeneficioRede, UUID> {

	long countByOrganizacaoOfertanteIdAndStatus(UUID organizacaoOfertanteId, StatusBeneficio status);

	@EntityGraph(attributePaths = {"organizacaoOfertante", "planoMinimo"})
	Page<BeneficioRede> findAllByStatusAndValidoAteGreaterThanEqual(
			StatusBeneficio status,
			LocalDate dataReferencia,
			Pageable pageable
	);

}
