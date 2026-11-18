package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.LoteImportacaoCsv;
import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoteImportacaoCsvRepository extends JpaRepository<LoteImportacaoCsv, UUID> {

	boolean existsByHashSha256(String hashSha256);

	Optional<LoteImportacaoCsv> findByHashSha256(String hashSha256);

	Page<LoteImportacaoCsv> findAllByStatus(StatusLoteCsv status, Pageable pageable);

}
