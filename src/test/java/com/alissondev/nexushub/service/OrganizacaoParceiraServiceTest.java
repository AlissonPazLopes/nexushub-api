package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.AcaoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.OrganizacaoParceiraRequest;
import com.alissondev.nexushub.dto.OrganizacaoParceiraResponse;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.exception.ConflictException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizacaoParceiraServiceTest {

	@Mock
	private OrganizacaoParceiraRepository organizacaoRepository;

	@Mock
	private PlanoCredenciamentoRepository planoRepository;

	@InjectMocks
	private OrganizacaoParceiraService service;

	private PlanoCredenciamento criarPlanoAtivo(UUID planoId) {
		PlanoCredenciamento plano = new PlanoCredenciamento(
				"GROWTH",
				"Plano Scale-up Growth",
				new BigDecimal("499.00"),
				10,
				25
		);
		plano.setId(planoId);
		return plano;
	}

	private OrganizacaoParceiraRequest criarRequest(UUID planoId) {
		return new OrganizacaoParceiraRequest(
				"11.222.333/0001-81",
				"Nexus Cloud",
				"Nexus Cloud Tecnologia Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				planoId
		);
	}

	@Test
	@DisplayName("Deve cadastrar organizacao com CNPJ sanitizado, plano ativo e status PENDENTE")
	void shouldRegisterOrganizationSuccessfully() {
		UUID planoId = UUID.randomUUID();
		PlanoCredenciamento plano = criarPlanoAtivo(planoId);
		OrganizacaoParceiraRequest request = criarRequest(planoId);

		when(organizacaoRepository.existsByCnpj("11222333000181")).thenReturn(false);
		when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));
		when(organizacaoRepository.save(any(OrganizacaoParceira.class))).thenAnswer(invocation -> {
			OrganizacaoParceira entity = invocation.getArgument(0);
			entity.setId(UUID.randomUUID());
			return entity;
		});

		OrganizacaoParceiraResponse response = service.cadastrar(request);

		ArgumentCaptor<OrganizacaoParceira> captor = ArgumentCaptor.forClass(OrganizacaoParceira.class);
		verify(organizacaoRepository).save(captor.capture());

		assertThat(captor.getValue().getCnpj()).isEqualTo("11222333000181");
		assertThat(response.id()).isNotNull();
		assertThat(response.cnpj()).isEqualTo("11222333000181");
		assertThat(response.status()).isEqualTo(StatusOrganizacao.PENDENTE);
		assertThat(response.planoId()).isEqualTo(planoId);
		assertThat(response.planoCodigo()).isEqualTo("GROWTH");
		assertThat(response.saldoResgatesCiclo()).isEqualTo(25);
	}

	@Test
	@DisplayName("Deve lancar ConflictException quando CNPJ ja estiver cadastrado")
	void shouldThrowConflictExceptionWhenCnpjAlreadyExists() {
		UUID planoId = UUID.randomUUID();
		OrganizacaoParceiraRequest request = criarRequest(planoId);

		when(organizacaoRepository.existsByCnpj("11222333000181")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar(request))
				.isInstanceOf(ConflictException.class);

		verifyNoInteractions(planoRepository);
		verify(organizacaoRepository, never()).save(any());
	}

	@Test
	@DisplayName("Deve lancar ResourceNotFoundException quando plano informado nao existir")
	void shouldThrowResourceNotFoundExceptionWhenPlanDoesNotExist() {
		UUID planoId = UUID.randomUUID();
		OrganizacaoParceiraRequest request = criarRequest(planoId);

		when(organizacaoRepository.existsByCnpj("11222333000181")).thenReturn(false);
		when(planoRepository.findById(planoId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cadastrar(request))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(organizacaoRepository, never()).save(any());
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando plano informado estiver inativo")
	void shouldThrowBusinessRuleExceptionWhenPlanIsInactive() {
		UUID planoId = UUID.randomUUID();
		PlanoCredenciamento planoInativo = criarPlanoAtivo(planoId);
		planoInativo.desativar();
		OrganizacaoParceiraRequest request = criarRequest(planoId);

		when(organizacaoRepository.existsByCnpj("11222333000181")).thenReturn(false);
		when(planoRepository.findById(planoId)).thenReturn(Optional.of(planoInativo));

		assertThatThrownBy(() -> service.cadastrar(request))
				.isInstanceOf(BusinessRuleException.class);

		verify(organizacaoRepository, never()).save(any());
	}

	@Test
	@DisplayName("Deve executar transicao de status valida e persistir alteracao")
	void shouldAlterStatusWhenTransitionIsValid() {
		UUID orgId = UUID.randomUUID();
		PlanoCredenciamento plano = criarPlanoAtivo(UUID.randomUUID());
		OrganizacaoParceira org = new OrganizacaoParceira(
				"11222333000181",
				"Nexus Cloud",
				"Nexus Cloud Tecnologia Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				plano
		);
		org.setId(orgId);

		when(organizacaoRepository.findById(orgId)).thenReturn(Optional.of(org));
		when(organizacaoRepository.save(any(OrganizacaoParceira.class))).thenAnswer(inv -> inv.getArgument(0));

		OrganizacaoParceiraResponse response = service.alterarStatus(orgId, AcaoCredenciamento.ATIVAR);

		assertThat(response.status()).isEqualTo(StatusOrganizacao.ATIVA);
		verify(organizacaoRepository).save(org);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException ao tentar transicao de status invalida")
	void shouldThrowBusinessRuleExceptionWhenTransitionIsInvalid() {
		UUID orgId = UUID.randomUUID();
		PlanoCredenciamento plano = criarPlanoAtivo(UUID.randomUUID());
		OrganizacaoParceira org = new OrganizacaoParceira(
				"11222333000181",
				"Nexus Cloud",
				"Nexus Cloud Tecnologia Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				plano
		);
		org.setId(orgId);

		when(organizacaoRepository.findById(orgId)).thenReturn(Optional.of(org));

		assertThatThrownBy(() -> service.alterarStatus(orgId, AcaoCredenciamento.SUSPENDER))
				.isInstanceOf(BusinessRuleException.class);

		verify(organizacaoRepository, never()).save(any());
	}

}
