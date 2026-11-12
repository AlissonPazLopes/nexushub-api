package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.dto.PlanoCredenciamentoRequest;
import com.alissondev.nexushub.dto.PlanoCredenciamentoResponse;
import com.alissondev.nexushub.service.PlanoCredenciamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planos")
public class PlanoCredenciamentoController {

	private final PlanoCredenciamentoService planoService;

	public PlanoCredenciamentoController(PlanoCredenciamentoService planoService) {
		this.planoService = planoService;
	}

	@PostMapping
	public ResponseEntity<PlanoCredenciamentoResponse> criarPlano(
			@Valid @RequestBody PlanoCredenciamentoRequest request
	) {
		PlanoCredenciamentoResponse response = planoService.criarPlano(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<PlanoCredenciamentoResponse>> listarPlanosAtivos() {
		return ResponseEntity.ok(planoService.listarPlanosAtivos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<PlanoCredenciamentoResponse> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(planoService.buscarPorId(id));
	}

}
