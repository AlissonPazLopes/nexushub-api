package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.AlterarStatusRequest;
import com.alissondev.nexushub.dto.OrganizacaoParceiraRequest;
import com.alissondev.nexushub.dto.OrganizacaoParceiraResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.service.OrganizacaoParceiraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/organizacoes")
@Tag(name = "Organizacoes Parceiras", description = "Credenciamento, consulta e transicoes de ciclo de vida B2B")
public class OrganizacaoParceiraController {

	private static final int MAX_PAGE_SIZE = 50;

	private final OrganizacaoParceiraService organizacaoService;

	public OrganizacaoParceiraController(OrganizacaoParceiraService organizacaoService) {
		this.organizacaoService = organizacaoService;
	}

	@PostMapping
	@Operation(summary = "Credenciar nova organizacao parceira", description = "Cadastra organizacao com status inicial PENDENTE vinculada a um plano ativo.")
	public ResponseEntity<OrganizacaoParceiraResponse> cadastrar(
			@Valid @RequestBody OrganizacaoParceiraRequest request
	) {
		OrganizacaoParceiraResponse response = organizacaoService.cadastrar(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	@Operation(summary = "Buscar organizacoes parceiras com filtros e paginacao", description = "Retorna pagina ordenada por criadoEm desc; parametro size limitado ao maximo de 50.")
	public ResponseEntity<PageResponse<OrganizacaoParceiraResponse>> buscar(
			@RequestParam(required = false) VerticalTech verticalTech,
			@RequestParam(required = false) StatusOrganizacao status,
			@RequestParam(required = false) UUID planoId,
			@RequestParam(required = false) String nome,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "criadoEm"));
		return ResponseEntity.ok(organizacaoService.buscar(verticalTech, status, planoId, nome, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar organizacao parceira por ID")
	public ResponseEntity<OrganizacaoParceiraResponse> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(organizacaoService.buscarPorId(id));
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Executar transicao de status da organizacao parceira")
	public ResponseEntity<OrganizacaoParceiraResponse> alterarStatus(
			@PathVariable UUID id,
			@Valid @RequestBody AlterarStatusRequest request
	) {
		return ResponseEntity.ok(organizacaoService.alterarStatus(id, request.acao()));
	}

}
