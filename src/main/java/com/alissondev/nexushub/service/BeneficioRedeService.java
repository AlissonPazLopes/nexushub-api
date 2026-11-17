package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.dto.BeneficioRedeRequest;
import com.alissondev.nexushub.dto.BeneficioRedeResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.dto.ResgateBeneficioResponse;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.BeneficioRedeRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class BeneficioRedeService {

	private final BeneficioRedeRepository beneficioRepository;
	private final OrganizacaoParceiraRepository organizacaoRepository;
	private final PlanoCredenciamentoRepository planoRepository;

	public BeneficioRedeService(
			BeneficioRedeRepository beneficioRepository,
			OrganizacaoParceiraRepository organizacaoRepository,
			PlanoCredenciamentoRepository planoRepository
	) {
		this.beneficioRepository = beneficioRepository;
		this.organizacaoRepository = organizacaoRepository;
		this.planoRepository = planoRepository;
	}

	@Transactional
	public BeneficioRedeResponse publicar(BeneficioRedeRequest request) {
		OrganizacaoParceira ofertante = organizacaoRepository.findById(request.organizacaoOfertanteId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Organizacao ofertante nao encontrada para o ID: " + request.organizacaoOfertanteId()
				));

		if (ofertante.getStatus() != StatusOrganizacao.ATIVA) {
			throw new BusinessRuleException(
					"Apenas organizacoes com status ATIVA podem publicar beneficios. Status atual: "
							+ ofertante.getStatus()
			);
		}

		PlanoCredenciamento planoMinimo = planoRepository.findById(request.planoMinimoId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Plano minimo nao encontrado para o ID: " + request.planoMinimoId()
				));

		if (!Boolean.TRUE.equals(planoMinimo.getAtivo())) {
			throw new BusinessRuleException(
					"Nao e possivel vincular o beneficio a um plano minimo inativo: " + planoMinimo.getCodigo()
			);
		}

		int limitePlano = ofertante.getPlano().getMaxBeneficiosOfertados();
		long ativosAtuais = beneficioRepository.countByOrganizacaoOfertanteIdAndStatus(
				ofertante.getId(),
				StatusBeneficio.ATIVO
		);
		if (ativosAtuais >= limitePlano) {
			throw new BusinessRuleException(
					"Limite de beneficios ativos do plano " + ofertante.getPlano().getCodigo()
							+ " atingido (" + limitePlano + ")."
			);
		}

		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoMinimo,
				request.titulo(),
				request.descricao(),
				request.cotaTotal(),
				request.validoAte()
		);

		BeneficioRede salvo = beneficioRepository.saveAndFlush(beneficio);
		return BeneficioRedeResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public PageResponse<BeneficioRedeResponse> listarCatalogo(Pageable pageable) {
		Page<BeneficioRedeResponse> page = beneficioRepository
				.findAllByStatusAndValidoAteGreaterThanEqual(StatusBeneficio.ATIVO, LocalDate.now(), pageable)
				.map(BeneficioRedeResponse::fromEntity);
		return PageResponse.from(page);
	}

	@Transactional(readOnly = true)
	public BeneficioRedeResponse buscarPorId(UUID id) {
		BeneficioRede beneficio = beneficioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Beneficio da rede nao encontrado para o ID: " + id
				));
		return BeneficioRedeResponse.fromEntity(beneficio);
	}

	@Transactional
	public ResgateBeneficioResponse resgatar(UUID beneficioId, UUID resgatanteId) {
		BeneficioRede beneficio = beneficioRepository.findById(beneficioId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Beneficio da rede nao encontrado para o ID: " + beneficioId
				));

		OrganizacaoParceira resgatante = organizacaoRepository.findById(resgatanteId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Organizacao resgatante nao encontrada para o ID: " + resgatanteId
				));

		if (resgatante.getStatus() != StatusOrganizacao.ATIVA) {
			throw new BusinessRuleException(
					"Apenas organizacoes com status ATIVA podem resgatar beneficios. Status atual: "
							+ resgatante.getStatus()
			);
		}

		if (beneficio.getOrganizacaoOfertante().getId().equals(resgatante.getId())) {
			throw new BusinessRuleException(
					"A organizacao ofertante nao pode resgatar o proprio beneficio."
			);
		}

		BigDecimal mensalidadeResgatante = resgatante.getPlano().getValorMensalidade();
		BigDecimal mensalidadeMinima = beneficio.getPlanoMinimo().getValorMensalidade();
		if (mensalidadeResgatante.compareTo(mensalidadeMinima) < 0) {
			throw new BusinessRuleException(
					"O plano da organizacao resgatante (" + resgatante.getPlano().getCodigo()
							+ ") nao atende ao tier minimo exigido pelo beneficio ("
							+ beneficio.getPlanoMinimo().getCodigo() + ")."
			);
		}

		beneficio.resgatarCota(LocalDate.now());
		BeneficioRede atualizado = beneficioRepository.saveAndFlush(beneficio);
		return ResgateBeneficioResponse.fromEntity(atualizado);
	}

}
