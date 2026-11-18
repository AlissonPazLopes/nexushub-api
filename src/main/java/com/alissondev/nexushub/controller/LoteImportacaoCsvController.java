package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import com.alissondev.nexushub.dto.LoteImportacaoCsvResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.service.LoteImportacaoCsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lotes-csv")
@Tag(name = "Lotes de Importacao CSV", description = "Monitoramento e consulta de lotes de importacao B2B com idempotencia SHA-256")
public class LoteImportacaoCsvController {

	private static final int MAX_PAGE_SIZE = 50;

	private final LoteImportacaoCsvService loteService;

	public LoteImportacaoCsvController(LoteImportacaoCsvService loteService) {
		this.loteService = loteService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar progresso e resultado de um lote CSV por ID")
	public ResponseEntity<LoteImportacaoCsvResponse> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(loteService.buscarPorId(id));
	}

	@GetMapping
	@Operation(summary = "Listar lotes de importacao CSV com filtro opcional por status")
	public ResponseEntity<PageResponse<LoteImportacaoCsvResponse>> listar(
			@RequestParam(required = false) StatusLoteCsv status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "iniciadoEm"));
		return ResponseEntity.ok(loteService.listar(status, pageable));
	}

}
