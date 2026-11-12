package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.dto.PlanoCredenciamentoRequest;
import com.alissondev.nexushub.dto.PlanoCredenciamentoResponse;
import com.alissondev.nexushub.exception.ConflictException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlanoCredenciamentoService {

	private final PlanoCredenciamentoRepository planoRepository;

	public PlanoCredenciamentoService(PlanoCredenciamentoRepository planoRepository) {
		this.planoRepository = planoRepository;
	}

	@Transactional
	public PlanoCredenciamentoResponse criarPlano(PlanoCredenciamentoRequest request) {
		String codigoNormalizado = request.codigo().trim().toUpperCase(Locale.ROOT);
		if (planoRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
			throw new ConflictException("Ja existe um plano de credenciamento cadastrado com o codigo: " + codigoNormalizado);
		}

		PlanoCredenciamento novoPlano = new PlanoCredenciamento(
				codigoNormalizado,
				request.nome(),
				request.valorMensalidade(),
				request.maxBeneficiosOfertados(),
				request.cotaResgatesMensal()
		);

		PlanoCredenciamento salvo = planoRepository.save(novoPlano);
		return PlanoCredenciamentoResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public List<PlanoCredenciamentoResponse> listarPlanosAtivos() {
		return planoRepository.findAllByAtivoTrueOrderByValorMensalidadeAsc()
				.stream()
				.map(PlanoCredenciamentoResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public PlanoCredenciamentoResponse buscarPorId(UUID id) {
		PlanoCredenciamento plano = planoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Plano de credenciamento nao encontrado para o ID: " + id));
		return PlanoCredenciamentoResponse.fromEntity(plano);
	}

}
