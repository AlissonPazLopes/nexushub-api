package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizacaoParceiraRepository extends JpaRepository<OrganizacaoParceira, UUID> {

	boolean existsByCnpj(String cnpj);

	Optional<OrganizacaoParceira> findByCnpj(String cnpj);

}
