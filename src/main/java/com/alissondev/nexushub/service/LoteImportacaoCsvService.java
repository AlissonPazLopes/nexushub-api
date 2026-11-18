package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.LoteImportacaoCsv;
import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import com.alissondev.nexushub.dto.LoteImportacaoCsvResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.exception.ConflictException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.LoteImportacaoCsvRepository;
import com.alissondev.nexushub.validation.Sha256ChecksumUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LoteImportacaoCsvService {

	private final LoteImportacaoCsvRepository loteRepository;

	public LoteImportacaoCsvService(LoteImportacaoCsvRepository loteRepository) {
		this.loteRepository = loteRepository;
	}

	@Transactional
	public LoteImportacaoCsvResponse registrarNovoLote(String nomeArquivo, byte[] conteudo) {
		if (nomeArquivo == null || nomeArquivo.isBlank()) {
			throw new BusinessRuleException("O nome do arquivo CSV e obrigatorio.");
		}

		String hashSha256 = Sha256ChecksumUtils.calcularHex(conteudo);
		if (loteRepository.existsByHashSha256(hashSha256)) {
			throw new ConflictException(
					"Ja existe um lote de importacao CSV registrado com o hash SHA-256: " + hashSha256
			);
		}

		LoteImportacaoCsv novoLote = new LoteImportacaoCsv(nomeArquivo.trim(), hashSha256);
		LoteImportacaoCsv salvo = loteRepository.saveAndFlush(novoLote);
		return LoteImportacaoCsvResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public LoteImportacaoCsvResponse buscarPorId(UUID id) {
		LoteImportacaoCsv lote = loteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Lote de importacao CSV nao encontrado para o ID: " + id
				));
		return LoteImportacaoCsvResponse.fromEntity(lote);
	}

	@Transactional(readOnly = true)
	public PageResponse<LoteImportacaoCsvResponse> listar(StatusLoteCsv status, Pageable pageable) {
		Page<LoteImportacaoCsv> page = status != null
				? loteRepository.findAllByStatus(status, pageable)
				: loteRepository.findAll(pageable);

		return PageResponse.from(page.map(LoteImportacaoCsvResponse::fromEntity));
	}

}
