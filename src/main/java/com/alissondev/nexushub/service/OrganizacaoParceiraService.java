package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.AcaoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.OrganizacaoParceiraRequest;
import com.alissondev.nexushub.dto.OrganizacaoParceiraResponse;
import com.alissondev.nexushub.dto.PageResponse;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.exception.ConflictException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraSpecifications;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import com.alissondev.nexushub.validation.CnpjUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizacaoParceiraService {

	private final OrganizacaoParceiraRepository organizacaoRepository;
	private final PlanoCredenciamentoRepository planoRepository;

	public OrganizacaoParceiraService(
			OrganizacaoParceiraRepository organizacaoRepository,
			PlanoCredenciamentoRepository planoRepository
	) {
		this.organizacaoRepository = organizacaoRepository;
		this.planoRepository = planoRepository;
	}

	@Transactional
	public OrganizacaoParceiraResponse cadastrar(OrganizacaoParceiraRequest request) {
		String cnpjSanitizado = CnpjUtils.sanitize(request.cnpj());
		if (organizacaoRepository.existsByCnpj(cnpjSanitizado)) {
			throw new ConflictException(
					"Ja existe uma organizacao parceira cadastrada com o CNPJ: " + cnpjSanitizado
			);
		}

		PlanoCredenciamento plano = planoRepository.findById(request.planoId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Plano de credenciamento nao encontrado para o ID: " + request.planoId()
				));

		if (!Boolean.TRUE.equals(plano.getAtivo())) {
			throw new BusinessRuleException(
					"Nao e possivel credenciar organizacao em um plano inativo: " + plano.getCodigo()
			);
		}

		OrganizacaoParceira novaOrganizacao = new OrganizacaoParceira(
				cnpjSanitizado,
				request.nomeFantasia(),
				request.razaoSocial(),
				request.emailContato(),
				request.verticalTech(),
				plano
		);

		OrganizacaoParceira salva = organizacaoRepository.save(novaOrganizacao);
		return OrganizacaoParceiraResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public PageResponse<OrganizacaoParceiraResponse> buscar(
			VerticalTech verticalTech,
			StatusOrganizacao status,
			UUID planoId,
			String nome,
			Pageable pageable
	) {
		Page<OrganizacaoParceiraResponse> page = organizacaoRepository
				.findAll(OrganizacaoParceiraSpecifications.comFiltros(verticalTech, status, planoId, nome), pageable)
				.map(OrganizacaoParceiraResponse::fromEntity);
		return PageResponse.from(page);
	}

	@Transactional(readOnly = true)
	public OrganizacaoParceiraResponse buscarPorId(UUID id) {
		OrganizacaoParceira organizacao = findOrganizacaoOrThrow(id);
		return OrganizacaoParceiraResponse.fromEntity(organizacao);
	}

	@Transactional
	public OrganizacaoParceiraResponse alterarStatus(UUID id, AcaoCredenciamento acao) {
		if (acao == null) {
			throw new BusinessRuleException("A acao de credenciamento e obrigatoria.");
		}

		OrganizacaoParceira organizacao = findOrganizacaoOrThrow(id);
		switch (acao) {
			case ATIVAR -> organizacao.ativar();
			case SUSPENDER -> organizacao.suspender();
			case REATIVAR -> organizacao.reativar();
			case DESCREDENCIAR -> organizacao.descredenciar();
		}

		OrganizacaoParceira atualizada = organizacaoRepository.save(organizacao);
		return OrganizacaoParceiraResponse.fromEntity(atualizada);
	}

	private OrganizacaoParceira findOrganizacaoOrThrow(UUID id) {
		return organizacaoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Organizacao parceira nao encontrada para o ID: " + id
				));
	}

}
