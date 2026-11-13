package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizacaoParceiraTest {

	private OrganizacaoParceira criarOrganizacaoPendente() {
		PlanoCredenciamento plano = new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth",
				new BigDecimal("499.00"),
				10,
				25
		);
		return new OrganizacaoParceira(
				"11.222.333/0001-81",
				"Nexus Cloud",
				"Nexus Cloud Tecnologia Ltda",
				"contato@nexuscloud.io",
				VerticalTech.DEVTOOLS,
				plano
		);
	}

	@Test
	@DisplayName("Deve inicializar organizacao com status PENDENTE e CNPJ sanitizado com 14 digitos")
	void shouldInitializeWithPendenteStatusAndSanitizedCnpj() {
		OrganizacaoParceira org = criarOrganizacaoPendente();

		assertThat(org.getStatus()).isEqualTo(StatusOrganizacao.PENDENTE);
		assertThat(org.getCnpj()).isEqualTo("11222333000181");
		assertThat(org.getSaldoResgatesCiclo()).isEqualTo(25);
	}

	@Test
	@DisplayName("Deve executar todas as transicoes validas da maquina de estados")
	void shouldAllowValidStateTransitions() {
		OrganizacaoParceira org = criarOrganizacaoPendente();

		org.ativar();
		assertThat(org.getStatus()).isEqualTo(StatusOrganizacao.ATIVA);

		org.suspender();
		assertThat(org.getStatus()).isEqualTo(StatusOrganizacao.SUSPENSA);

		org.reativar();
		assertThat(org.getStatus()).isEqualTo(StatusOrganizacao.ATIVA);

		org.descredenciar();
		assertThat(org.getStatus()).isEqualTo(StatusOrganizacao.DESCREDENCIADA);
	}

	@Test
	@DisplayName("Deve permitir descredenciar a partir de PENDENTE ou SUSPENSA")
	void shouldAllowDescredenciarFromPendenteAndSuspensa() {
		OrganizacaoParceira pendente = criarOrganizacaoPendente();
		pendente.descredenciar();
		assertThat(pendente.getStatus()).isEqualTo(StatusOrganizacao.DESCREDENCIADA);

		OrganizacaoParceira suspensa = criarOrganizacaoPendente();
		suspensa.ativar();
		suspensa.suspender();
		suspensa.descredenciar();
		assertThat(suspensa.getStatus()).isEqualTo(StatusOrganizacao.DESCREDENCIADA);
	}

	@Test
	@DisplayName("Deve lancar BusinessRuleException em transicoes invalidas de ativacao, suspensao e reativacao")
	void shouldThrowBusinessRuleExceptionOnInvalidTransitions() {
		OrganizacaoParceira org = criarOrganizacaoPendente();

		assertThatThrownBy(org::suspender).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::reativar).isInstanceOf(BusinessRuleException.class);

		org.ativar();
		assertThatThrownBy(org::ativar).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::reativar).isInstanceOf(BusinessRuleException.class);

		org.suspender();
		assertThatThrownBy(org::ativar).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::suspender).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Deve tratar DESCREDENCIADA como estado terminal bloqueando qualquer transicao")
	void shouldTreatDescredenciadaAsTerminalState() {
		OrganizacaoParceira org = criarOrganizacaoPendente();
		org.descredenciar();

		assertThatThrownBy(org::ativar).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::suspender).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::reativar).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(org::descredenciar).isInstanceOf(BusinessRuleException.class);
	}

}
