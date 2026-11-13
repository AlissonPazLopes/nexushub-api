package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusOrganizacao;
import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.exception.BusinessRuleException;
import com.alissondev.nexushub.validation.CnpjUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_organizations")
public class OrganizacaoParceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "cnpj", nullable = false, unique = true, length = 14)
	private String cnpj;

	@Column(name = "nome_fantasia", nullable = false, length = 150)
	private String nomeFantasia;

	@Column(name = "razao_social", nullable = false, length = 200)
	private String razaoSocial;

	@Column(name = "email_contato", nullable = false, length = 150)
	private String emailContato;

	@Enumerated(EnumType.STRING)
	@Column(name = "vertical_tech", nullable = false, length = 40)
	private VerticalTech verticalTech;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusOrganizacao status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plano_id", nullable = false)
	private PlanoCredenciamento plano;

	@Column(name = "lote_origem_id")
	private UUID loteOrigemId;

	@Column(name = "saldo_resgates_ciclo", nullable = false)
	private Integer saldoResgatesCiclo = 0;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	public OrganizacaoParceira() {
	}

	public OrganizacaoParceira(
			String cnpj,
			String nomeFantasia,
			String razaoSocial,
			String emailContato,
			VerticalTech verticalTech,
			PlanoCredenciamento plano
	) {
		this.cnpj = CnpjUtils.sanitize(cnpj);
		this.nomeFantasia = nomeFantasia != null ? nomeFantasia.trim() : null;
		this.razaoSocial = razaoSocial != null ? razaoSocial.trim() : null;
		this.emailContato = emailContato != null ? emailContato.trim() : null;
		this.verticalTech = verticalTech;
		this.plano = plano;
		this.status = StatusOrganizacao.PENDENTE;
		this.saldoResgatesCiclo = (plano != null && plano.getCotaResgatesMensal() != null)
				? plano.getCotaResgatesMensal()
				: 0;
		this.criadoEm = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		normalizarCampos();
		if (this.status == null) {
			this.status = StatusOrganizacao.PENDENTE;
		}
		if (this.saldoResgatesCiclo == null) {
			this.saldoResgatesCiclo = 0;
		}
		if (this.criadoEm == null) {
			this.criadoEm = LocalDateTime.now();
		}
	}

	@PreUpdate
	void preUpdate() {
		normalizarCampos();
	}

	private void normalizarCampos() {
		if (this.cnpj != null) {
			this.cnpj = CnpjUtils.sanitize(this.cnpj);
		}
		if (this.nomeFantasia != null) {
			this.nomeFantasia = this.nomeFantasia.trim();
		}
		if (this.razaoSocial != null) {
			this.razaoSocial = this.razaoSocial.trim();
		}
		if (this.emailContato != null) {
			this.emailContato = this.emailContato.trim();
		}
	}

	public void ativar() {
		if (this.status != StatusOrganizacao.PENDENTE) {
			throw new BusinessRuleException(
					"Apenas organizacoes com status PENDENTE podem ser ativadas. Status atual: " + this.status
			);
		}
		this.status = StatusOrganizacao.ATIVA;
	}

	public void suspender() {
		if (this.status != StatusOrganizacao.ATIVA) {
			throw new BusinessRuleException(
					"Apenas organizacoes com status ATIVA podem ser suspensas. Status atual: " + this.status
			);
		}
		this.status = StatusOrganizacao.SUSPENSA;
	}

	public void reativar() {
		if (this.status != StatusOrganizacao.SUSPENSA) {
			throw new BusinessRuleException(
					"Apenas organizacoes com status SUSPENSA podem ser reativadas. Status atual: " + this.status
			);
		}
		this.status = StatusOrganizacao.ATIVA;
	}

	public void descredenciar() {
		if (this.status == StatusOrganizacao.DESCREDENCIADA) {
			throw new BusinessRuleException("A organizacao ja encontra-se descredenciada.");
		}
		this.status = StatusOrganizacao.DESCREDENCIADA;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = CnpjUtils.sanitize(cnpj);
	}

	public String getNomeFantasia() {
		return nomeFantasia;
	}

	public void setNomeFantasia(String nomeFantasia) {
		this.nomeFantasia = nomeFantasia;
	}

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public void setRazaoSocial(String razaoSocial) {
		this.razaoSocial = razaoSocial;
	}

	public String getEmailContato() {
		return emailContato;
	}

	public void setEmailContato(String emailContato) {
		this.emailContato = emailContato;
	}

	public VerticalTech getVerticalTech() {
		return verticalTech;
	}

	public void setVerticalTech(VerticalTech verticalTech) {
		this.verticalTech = verticalTech;
	}

	public StatusOrganizacao getStatus() {
		return status;
	}

	public void setStatus(StatusOrganizacao status) {
		this.status = status;
	}

	public PlanoCredenciamento getPlano() {
		return plano;
	}

	public void setPlano(PlanoCredenciamento plano) {
		this.plano = plano;
	}

	public UUID getLoteOrigemId() {
		return loteOrigemId;
	}

	public void setLoteOrigemId(UUID loteOrigemId) {
		this.loteOrigemId = loteOrigemId;
	}

	public Integer getSaldoResgatesCiclo() {
		return saldoResgatesCiclo;
	}

	public void setSaldoResgatesCiclo(Integer saldoResgatesCiclo) {
		this.saldoResgatesCiclo = saldoResgatesCiclo;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}

}
