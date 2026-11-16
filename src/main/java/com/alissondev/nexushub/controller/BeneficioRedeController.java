package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.dto.BeneficioRedeRequest;
import com.alissondev.nexushub.dto.BeneficioRedeResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.service.BeneficioRedeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beneficios")
@Tag(name = "Beneficios da Rede", description = "Publicacao e catalogo de beneficios B2B com controle de cota por plano")
public class BeneficioRedeController {

	private static final int MAX_PAGE_SIZE = 50;

	private final BeneficioRedeService beneficioService;

	public BeneficioRedeController(BeneficioRedeService beneficioService) {
		this.beneficioService = beneficioService;
	}

	@PostMapping
	@Operation(summary = "Publicar novo beneficio B2B", description = "Publica beneficio para organizacao ATIVA respeitando o limite maxBeneficiosOfertados do seu plano.")
	public ResponseEntity<BeneficioRedeResponse> publicar(
			@Valid @RequestBody BeneficioRedeRequest request
	) {
		BeneficioRedeResponse response = beneficioService.publicar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar catalogo paginado de beneficios ativos e vigentes")
	public ResponseEntity<PageResponse<BeneficioRedeResponse>> listarCatalogo(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "criadoEm"));
		return ResponseEntity.ok(beneficioService.listarCatalogo(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar beneficio da rede por ID")
	public ResponseEntity<BeneficioRedeResponse> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(beneficioService.buscarPorId(id));
	}

}
