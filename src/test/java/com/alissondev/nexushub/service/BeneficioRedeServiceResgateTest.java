package com.alissondev.nexushub.service;

import com.alissondev.nexushub.domain.entity.BeneficioRede;
import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.dto.ResgateBeneficioResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficioRedeServiceResgateTest {

	@Mock
	private BeneficioRedeRepository beneficioRepository;

	@Mock
	private OrganizacaoParceiraRepository organizacaoRepository;

	@Mock
	private PlanoCredenciamentoRepository planoRepository;

	@InjectMocks
	private BeneficioRedeService service;

	private PlanoCredenciamento criarPlano(String codigo, String valorMensalidade) {
		PlanoCredenciamento plano = new PlanoCredenciamento(
				codigo,
				"Plano " + codigo,
				new BigDecimal(valorMensalidade),
				5,
				20
		);
		plano.setId(UUID.randomUUID());
		return plano;
	}

	private OrganizacaoParceira criarOrganizacao(UUID id, String cnpj, PlanoCredenciamento plano, boolean ativa) {
		OrganizacaoParceira org = new OrganizacaoParceira(
				cnpj,
				"Empresa " + cnpj,
				"Empresa " + cnpj + " Ltda",
				"contato@empresa.io",
				VerticalTech.FINTECH,
				plano
		);
		org.setId(id);
		if (ativa) {
			org.ativar();
		}
		return org;
	}

	@Test
	@DisplayName("Deve resgatar cota com sucesso quando resgatante for ATIVA, diferente do ofertante e tiver tier suficiente")
	void shouldRedeemBenefitSuccessfully() {
		PlanoCredenciamento planoGrowth = criarPlano("GROWTH", "499.00");
		PlanoCredenciamento planoCorporate = criarPlano("CORPORATE", "999.00");

		OrganizacaoParceira ofertante = criarOrganizacao(UUID.randomUUID(), "11222333000181", planoGrowth, true);
		OrganizacaoParceira resgatante = criarOrganizacao(UUID.randomUUID(), "11444777000161", planoCorporate, true);

		UUID beneficioId = UUID.randomUUID();
		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoGrowth,
				"Creditos Cloud",
				"Creditos para infraestrutura",
				2,
				LocalDate.now().plusDays(15)
		);
		beneficio.setId(beneficioId);
		beneficio.setVersion(0L);

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.of(beneficio));
		when(organizacaoRepository.findById(resgatante.getId())).thenReturn(Optional.of(resgatante));
		when(beneficioRepository.saveAndFlush(any(BeneficioRede.class))).thenAnswer(inv -> {
			BeneficioRede entity = inv.getArgument(0);
			entity.setVersion(1L);
			return entity;
		});

		ResgateBeneficioResponse response = service.resgatar(beneficioId, resgatante.getId());

		assertThat(response.beneficioId()).isEqualTo(beneficioId);
		assertThat(response.cotaUtilizada()).isEqualTo(1);
		assertThat(response.cotaDisponivel()).isEqualTo(1);
		assertThat(response.status()).isEqualTo(StatusBeneficio.ATIVO);
		assertThat(response.version()).isEqualTo(1L);
		assertThat(response.resgatadoEm()).isNotNull();
		verify(beneficioRepository).saveAndFlush(beneficio);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando plano do resgatante tiver valor inferior ao plano minimo")
	void shouldThrowBusinessRuleExceptionWhenTierIsInsufficient() {
		PlanoCredenciamento planoSeed = criarPlano("SEED", "199.00");
		PlanoCredenciamento planoGrowth = criarPlano("GROWTH", "499.00");

		OrganizacaoParceira ofertante = criarOrganizacao(UUID.randomUUID(), "11222333000181", planoGrowth, true);
		OrganizacaoParceira resgatanteSeed = criarOrganizacao(UUID.randomUUID(), "11444777000161", planoSeed, true);

		UUID beneficioId = UUID.randomUUID();
		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoGrowth,
				"Mentoria Scale-up",
				"Exclusivo para Growth ou superior",
				5,
				LocalDate.now().plusDays(15)
		);
		beneficio.setId(beneficioId);

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.of(beneficio));
		when(organizacaoRepository.findById(resgatanteSeed.getId())).thenReturn(Optional.of(resgatanteSeed));

		assertThatThrownBy(() -> service.resgatar(beneficioId, resgatanteSeed.getId()))
				.isInstanceOf(BusinessRuleException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando ofertante tentar resgatar o proprio beneficio")
	void shouldThrowBusinessRuleExceptionOnSelfRedeem() {
		PlanoCredenciamento planoGrowth = criarPlano("GROWTH", "499.00");
		OrganizacaoParceira ofertante = criarOrganizacao(UUID.randomUUID(), "11222333000181", planoGrowth, true);

		UUID beneficioId = UUID.randomUUID();
		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoGrowth,
				"Beneficio Proprio",
				"Descricao",
				5,
				LocalDate.now().plusDays(15)
		);
		beneficio.setId(beneficioId);

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.of(beneficio));
		when(organizacaoRepository.findById(ofertante.getId())).thenReturn(Optional.of(ofertante));

		assertThatThrownBy(() -> service.resgatar(beneficioId, ofertante.getId()))
				.isInstanceOf(BusinessRuleException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException quando organizacao resgatante nao estiver ATIVA")
	void shouldThrowBusinessRuleExceptionWhenRedeemerIsNotActive() {
		PlanoCredenciamento planoGrowth = criarPlano("GROWTH", "499.00");
		OrganizacaoParceira ofertante = criarOrganizacao(UUID.randomUUID(), "11222333000181", planoGrowth, true);
		OrganizacaoParceira resgatantePendente = criarOrganizacao(UUID.randomUUID(), "11444777000161", planoGrowth, false);

		UUID beneficioId = UUID.randomUUID();
		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoGrowth,
				"Beneficio Rede",
				"Descricao",
				5,
				LocalDate.now().plusDays(15)
		);
		beneficio.setId(beneficioId);

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.of(beneficio));
		when(organizacaoRepository.findById(resgatantePendente.getId())).thenReturn(Optional.of(resgatantePendente));

		assertThatThrownBy(() -> service.resgatar(beneficioId, resgatantePendente.getId()))
				.isInstanceOf(BusinessRuleException.class);

		verify(beneficioRepository, never()).saveAndFlush(any());
	}

	@Test
	@DisplayName("Deve lancar ResourceNotFoundException quando beneficio ou organizacao resgatante nao existir")
	void shouldThrowResourceNotFoundExceptionWhenBenefitOrOrganizationNotFound() {
		UUID beneficioId = UUID.randomUUID();
		UUID resgatanteId = UUID.randomUUID();

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resgatar(beneficioId, resgatanteId))
				.isInstanceOf(ResourceNotFoundException.class);

		PlanoCredenciamento planoGrowth = criarPlano("GROWTH", "499.00");
		OrganizacaoParceira ofertante = criarOrganizacao(UUID.randomUUID(), "11222333000181", planoGrowth, true);
		BeneficioRede beneficio = new BeneficioRede(
				ofertante,
				planoGrowth,
				"Beneficio Rede",
				"Descricao",
				5,
				LocalDate.now().plusDays(15)
		);
		beneficio.setId(beneficioId);

		when(beneficioRepository.findById(beneficioId)).thenReturn(Optional.of(beneficio));
		when(organizacaoRepository.findById(resgatanteId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resgatar(beneficioId, resgatanteId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
