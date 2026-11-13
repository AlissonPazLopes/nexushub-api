package com.alissondev.nexushub.repository;

import com.alissondev.nexushub.domain.entity.OrganizacaoParceira;
import com.alissondev.nexushub.domain.entity.PlanoCredenciamento;
import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrganizacaoParceiraRepositoryTest {

	@Autowired
	private PlanoCredenciamentoRepository planoRepository;

	@Autowired
	private OrganizacaoParceiraRepository organizacaoRepository;

	@Test
	@DisplayName("Deve persistir organizacao com CNPJ sanitizado e consultar por existsByCnpj e findByCnpj")
	void shouldPersistOrganizationAndLookupByCnpj() {
		PlanoCredenciamento plano = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"SEED",
				"Plano Incubada Seed",
				new BigDecimal("199.90"),
				3,
				10
		));

		OrganizacaoParceira org = new OrganizacaoParceira(
				"11.222.333/0001-81",
				"AgroSense IoT",
				"AgroSense Hardware Ltda",
				"contato@agrosense.io",
				VerticalTech.AGRITECH,
				plano
		);

		OrganizacaoParceira saved = organizacaoRepository.saveAndFlush(org);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCnpj()).isEqualTo("11222333000181");
		assertThat(saved.getStatus()).isEqualTo(StatusOrganizacao.PENDENTE);
		assertThat(saved.getVersion()).isNotNull();
		assertThat(organizacaoRepository.existsByCnpj("11222333000181")).isTrue();

		Optional<OrganizacaoParceira> found = organizacaoRepository.findByCnpj("11222333000181");
		assertThat(found).isPresent();
		assertThat(found.get().getNomeFantasia()).isEqualTo("AgroSense IoT");
		assertThat(found.get().getPlano().getCodigo()).isEqualTo("SEED");
	}

	@Test
	@DisplayName("Deve garantir constraint UNIQUE de CNPJ na tabela partner_organizations")
	void shouldEnforceUniqueCnpjConstraint() {
		PlanoCredenciamento plano = planoRepository.saveAndFlush(new PlanoCredenciamento(
				"GROWTH",
				"Plano Growth",
				new BigDecimal("499.00"),
				10,
				25
		));

		OrganizacaoParceira primeira = new OrganizacaoParceira(
				"11.222.333/0001-81",
				"FinPulse",
				"FinPulse Pagamentos S.A.",
				"fin@finpulse.com",
				VerticalTech.FINTECH,
				plano
		);
		organizacaoRepository.saveAndFlush(primeira);

		OrganizacaoParceira duplicada = new OrganizacaoParceira(
				"11222333000181",
				"FinPulse Filial",
				"FinPulse Filial Ltda",
				"filial@finpulse.com",
				VerticalTech.FINTECH,
				plano
		);

		assertThatThrownBy(() -> organizacaoRepository.saveAndFlush(duplicada))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
