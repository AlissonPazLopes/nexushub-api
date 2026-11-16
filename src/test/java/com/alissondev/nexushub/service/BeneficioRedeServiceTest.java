package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.BeneficioRedeRequest;
import com.alissondev.nexushub.dto.BeneficioRedeResponse;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.exception.ResourceNotFoundException;
import com.alissondev.nexushub.repository.BeneficioRedeRepository;
import com.alissondev.nexushub.repository.OrganizacaoParceiraRepository;
import com.alissondev.nexushub.repository.PlanoCredenciamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class BeneficioRedeServiceTest {

	@Mock
	private BeneficioRedeRepository beneficioRepository;

	@Mock
	private OrganizacaoParceiraRepository organizacaoRepository;

	@Mock
	private PlanoCredenciamentoRepository planoRepository;

	@InjectMocks
	private BeneficioRedeService service;

	private PlanoCredenciamento criarPlano(UUID id, String codigo, int maxBeneficios, boolean ativo) {
		PlanoCredenciamento plano = new PlanoCredenciamento(
				codigo,
				"Plano " + codigo,
				new BigDecimal("299.00"),
				maxBeneficios,
				20
		);
		plano.setId(id);
		if (!ativo) {
			plano.desativar();
		}
		return plano;
	}

	private OrganizacaoParceira criarOfertante(UUID id, PlanoCredenciamento plano, boolean ativa) {
		OrganizacaoParceira org = new OrganizacaoParceira(
				"11.222.333/0001-81",
				"FinPulse",
				"FinPulse Pagamentos S.A.",
				"contato@finpulse.io",
				VerticalTech.FINTECH,
				plano
		);
		org.setId(id);
		if (ativa) {
			org.ativar();
		}
		return org;
	}

	private BeneficioRedeRequest criarRequest(UUID ofertanteId, UUID planoMinimoId) {
		return new BeneficioRedeRequest(
				ofertanteId,
				planoMinimoId,
				"Mentoria Arquitetura Java 21",
				"Duas horas de revisao tecnica para startups da rede.",
				10,
				LocalDate.now().plusDays(30)
		);
	}

	@Test
	@DisplayName("Deve publicar beneficio com sucesso quando ofertante for ATIVA, plano minimo ativo e dentro do limite")
	void shouldPublishBenefitSuccessfully() {
		UUID ofertanteId = UUID.randomUUID();
		UUID planoId = UUID.randomUUID();
		PlanoCredenciamento plano = criarPlano(planoId, "GROWTH", 5, true);
		OrganizacaoParceira ofertante = criarOfertante(ofertanteId, plano, true);
		BeneficioRedeRequest request = criarRequest(ofertanteId, planoId);

		when(organizacaoRepository.findById(ofertanteId)).thenReturn(Optional.of(ofertante));
		when(planoRepository.findById(planoId)).thenReturn(Optional.of(plano));
		when(beneficioRepository.countByOrganizacaoOfertanteIdAndStatus(ofertanteId, StatusBeneficio.ATIVO))
				.thenReturn(2L);
		when(beneficioRepository.saveAndFlush(any(BeneficioRede.class))).thenAnswer(inv -> {
			BeneficioRede entity = inv.getArgument(0);
			entity.setId(UUID.randomUUID());
			entity.setVersion(0L);
			return entity;
		});

		BeneficioRedeResponse response = service.publicar(request);

		assertThat(response.id()).isNotNull();
		assertThat(response.status()).isEqualTo(StatusBeneficio.ATIVO);
		assertThat(response.cotaTotal()).isEqualTo(10);
		assertThat(response.cotaUtilizada()).isZero();
		assertThat(response.cotaDisponivel()).isEqualTo(10);
		assertThat(response.version()).isZero();
		verify(beneficioRepository).saveAndFlush(any(BeneficioRede.class));
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando organizacao ofertante nao estiver ATIVA")
	void shouldThrowBusinessRuleExceptionWhenPartnerIsNotActive() {
		UUID ofertanteId = UUID.randomUUID();
		UUID planoId = UUID.randomUUID();
		PlanoCredenciamento plano = criarPlano(planoId, "SEED", 3, true);
		OrganizacaoParceira ofertantePendente = criarOfertante(ofertanteId, plano, false);
		BeneficioRedeRequest request = criarRequest(ofertanteId, planoId);

		when(organizacaoRepository.findById(ofertanteId)).thenReturn(Optional.of(ofertantePendente));

		assertThatThrownBy(() -> service.publicar(request))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(planoRepository);
		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar ResourceNotFoundException quando plano minimo nao existir")
	void shouldThrowResourceNotFoundExceptionWhenMinimumPlanDoesNotExist() {
		UUID ofertanteId = UUID.randomUUID();
		UUID planoMinimoId = UUID.randomUUID();
		PlanoCredenciamento planoOfertante = criarPlano(UUID.randomUUID(), "GROWTH", 5, true);
		OrganizacaoParceira ofertante = criarOfertante(ofertanteId, planoOfertante, true);
		BeneficioRedeRequest request = criarRequest(ofertanteId, planoMinimoId);

		when(organizacaoRepository.findById(ofertanteId)).thenReturn(Optional.of(ofertante));
		when(planoRepository.findById(planoMinimoId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.publicar(request))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando plano minimo estiver inativo")
	void shouldThrowBusinessRuleExceptionWhenMinimumPlanIsInactive() {
		UUID ofertanteId = UUID.randomUUID();
		UUID planoMinimoId = UUID.randomUUID();
		PlanoCredenciamento planoOfertante = criarPlano(UUID.randomUUID(), "GROWTH", 5, true);
		PlanoCredenciamento planoInativo = criarPlano(planoMinimoId, "LEGACY", 2, false);
		OrganizacaoParceira ofertante = criarOfertante(ofertanteId, planoOfertante, true);
		BeneficioRedeRequest request = criarRequest(ofertanteId, planoMinimoId);

		when(organizacaoRepository.findById(ofertanteId)).thenReturn(Optional.of(ofertante));
		when(planoRepository.findById(planoMinimoId)).thenReturn(Optional.of(planoInativo));

		assertThatThrownBy(() -> service.publicar(request))
				.isInstanceOf(BusinessRuleException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando limite maxBeneficiosOfertados do plano for atingido")
	void shouldThrowBusinessRuleExceptionWhenPlanBenefitLimitReached() {
		UUID ofertanteId = UUID.randomUUID();
		UUID planoId = UUID.randomUUID();
		PlanoCredenciamento planoSeed = criarPlano(planoId, "SEED", 2, true);
		OrganizacaoParceira ofertante = criarOfertante(ofertanteId, planoSeed, true);
		BeneficioRedeRequest request = criarRequest(ofertanteId, planoId);

		when(organizacaoRepository.findById(ofertanteId)).thenReturn(Optional.of(ofertante));
		when(planoRepository.findById(planoId)).thenReturn(Optional.of(planoSeed));
		when(beneficioRepository.countByOrganizacaoOfertanteIdAndStatus(ofertanteId, StatusBeneficio.ATIVO))
				.thenReturn(2L);

		assertThatThrownBy(() -> service.publicar(request))
				.isInstanceOf(BusinessRuleException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar ResourceNotFoundException ao buscar beneficio inexistente por ID")
	void shouldThrowResourceNotFoundExceptionWhenBenefitIdDoesNotExist() {
		UUID beneficioId = UUID.randomUUID();
		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(beneficioId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
