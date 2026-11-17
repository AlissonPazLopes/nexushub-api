package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeneficioRedeResgateTest {

	private BeneficioRede criarBeneficio(int cotaTotal, LocalDate validoAte) {
		PlanoCredenciamento plano = new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth",
				new BigDecimal("499.00"),
				5,
				20
		);
		OrganizacaoParceira ofertante = new OrganizacaoParceira(
				"11.222.333/0001-81",
				"Nexus Cloud",
				"Nexus Cloud Tecnologia Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				plano
		);
		ofertante.ativar();

		return new BeneficioRede(
				ofertante,
				plano,
				"Creditos Cloud B2B",
				"Creditos para infraestrutura cloud",
				cotaTotal,
				validoAte
		);
	}

	@Test
	@DisplayName("Deve incrementar cotaUtilizada e manter status ATIVO enquanto restarem cotas")
	void shouldIncrementUsedQuotaAndKeepActiveWhenQuotaRemains() {
		LocalDate hoje = LocalDate.of(2026, 11, 17);
		BeneficioRede beneficio = criarBeneficio(3, hoje.plusDays(10));

		beneficio.resgatarCota(hoje);

		assertThat(beneficio.getCotaUtilizada()).isEqualTo(1);
		assertThat(beneficio.cotaDisponivel()).isEqualTo(2);
		assertThat(beneficio.getStatus()).isEqualTo(StatusBeneficio.ATIVO);
	}

	@Test
	@DisplayName("Deve alterar status para ESGOTADO ao consumir a ultima cota disponivel")
	void shouldTransitionToEsgotadoWhenLastQuotaIsRedeemed() {
		LocalDate hoje = LocalDate.of(2026, 11, 17);
		BeneficioRede beneficio = criarBeneficio(2, hoje.plusDays(10));

		beneficio.resgatarCota(hoje);
		assertThat(beneficio.getStatus()).isEqualTo(StatusBeneficio.ATIVO);

		beneficio.resgatarCota(hoje);
		assertThat(beneficio.getCotaUtilizada()).isEqualTo(2);
		assertThat(beneficio.cotaDisponivel()).isZero();
		assertThat(beneficio.getStatus()).isEqualTo(StatusBeneficio.ESGOTADO);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException ao tentar resgatar beneficio esgotado ou sem cota")
	void shouldThrowBusinessRuleExceptionWhenNoQuotaAvailable() {
		LocalDate hoje = LocalDate.of(2026, 11, 17);
		BeneficioRede beneficio = criarBeneficio(1, hoje.plusDays(10));
		beneficio.resgatarCota(hoje);

		assertThatThrownBy(() -> beneficio.resgatarCota(hoje))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException ao tentar resgatar beneficio vencido")
	void shouldThrowBusinessRuleExceptionWhenBenefitIsExpiredByDate() {
		LocalDate validoAte = LocalDate.of(2026, 11, 16);
		BeneficioRede beneficio = criarBeneficio(5, validoAte);

		assertThatThrownBy(() -> beneficio.resgatarCota(LocalDate.of(2026, 11, 17)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Deve expirar beneficio ATIVO e bloquear resgate ou nova expiracao quando nao estiver ATIVO")
	void shouldExpireActiveBenefitAndRejectOperationsWhenNotActive() {
		LocalDate hoje = LocalDate.of(2026, 11, 17);
		BeneficioRede beneficio = criarBeneficio(5, hoje.plusDays(10));

		beneficio.expirar();
		assertThat(beneficio.getStatus()).isEqualTo(StatusBeneficio.EXPIRADO);

		assertThatThrownBy(() -> beneficio.resgatarCota(hoje))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(beneficio::expirar)
				.isInstanceOf(BusinessRuleException.class);
	}

}
