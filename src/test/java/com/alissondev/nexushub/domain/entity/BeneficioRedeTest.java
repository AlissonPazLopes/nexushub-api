package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusBeneficio;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BeneficioRedeTest {

	private BeneficioRede criarBeneficioPadrao(LocalDate validoAte) {
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
				"  Creditos Cloud B2B  ",
				"  Pacote de creditos para startups do polo  ",
				15,
				validoAte
		);
	}

	@Test
	@DisplayName("Deve inicializar beneficio com status ATIVO, cotaUtilizada zero e cotaDisponivel igual a cotaTotal")
	void shouldInitializeWithActiveStatusAndZeroUsedQuota() {
		BeneficioRede beneficio = criarBeneficioPadrao(LocalDate.of(2026, 12, 31));

		assertThat(beneficio.getStatus()).isEqualTo(StatusBeneficio.ATIVO);
		assertThat(beneficio.getCotaUtilizada()).isZero();
		assertThat(beneficio.getCotaTotal()).isEqualTo(15);
		assertThat(beneficio.cotaDisponivel()).isEqualTo(15);
		assertThat(beneficio.getTitulo()).isEqualTo("Creditos Cloud B2B");
		assertThat(beneficio.getDescricao()).isEqualTo("Pacote de creditos para startups do polo");
	}

	@Test
	@DisplayName("Deve calcular cotaDisponivel corretamente conforme consumo sem retornar valores negativos")
	void shouldCalculateAvailableQuotaCorrectly() {
		BeneficioRede beneficio = criarBeneficioPadrao(LocalDate.of(2026, 12, 31));

		beneficio.setCotaUtilizada(6);
		assertThat(beneficio.cotaDisponivel()).isEqualTo(9);

		beneficio.setCotaUtilizada(15);
		assertThat(beneficio.cotaDisponivel()).isZero();

		beneficio.setCotaUtilizada(20);
		assertThat(beneficio.cotaDisponivel()).isZero();
	}

	@Test
	@DisplayName("Deve indicar expiracao somente quando a data de referencia for posterior a validoAte")
	void shouldEvaluateExpirationRelativeToReferenceDate() {
		LocalDate validoAte = LocalDate.of(2026, 11, 30);
		BeneficioRede beneficio = criarBeneficioPadrao(validoAte);

		assertThat(beneficio.estaExpirado(LocalDate.of(2026, 11, 29))).isFalse();
		assertThat(beneficio.estaExpirado(LocalDate.of(2026, 11, 30))).isFalse();
		assertThat(beneficio.estaExpirado(LocalDate.of(2026, 12, 1))).isTrue();
	}

}
