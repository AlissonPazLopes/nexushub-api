package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanoCredenciamentoRepository extends JpaRepository<PlanoCredenciamento, UUID> {

	boolean existsByCodigoIgnoreCase(String codigo);

	Optional<PlanoCredenciamento> findByCodigoIgnoreCase(String codigo);

	List<PlanoCredenciamento> findAllByAtivoTrueOrderByValorMensalidadeAsc();

}
