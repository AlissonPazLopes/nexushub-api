package com.alissondev.nexushub.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "accreditation_plans")
public class PlanoCredenciamento {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "codigo", nullable = false, unique = true, length = 50)
	private String codigo;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "valor_mensalidade", nullable = false, precision = 12, scale = 2)
	private BigDecimal valorMensalidade;

	@Column(name = "max_beneficios_ofertados", nullable = false)
	private Integer maxBeneficiosOfertados;

	@Column(name = "cota_resgates_mensal", nullable = false)
	private Integer cotaResgatesMensal;

	@Column(name = "ativo", nullable = false)
	private Boolean ativo = true;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	public PlanoCredenciamento() {
	}

	public PlanoCredenciamento(
			String codigo,
			String nome,
			BigDecimal valorMensalidade,
			Integer maxBeneficiosOfertados,
			Integer cotaResgatesMensal
	) {
		this.codigo = codigo != null ? codigo.trim().toUpperCase(Locale.ROOT) : null;
		this.nome = nome != null ? nome.trim() : null;
		this.valorMensalidade = valorMensalidade;
		this.maxBeneficiosOfertados = maxBeneficiosOfertados;
		this.cotaResgatesMensal = cotaResgatesMensal;
		this.ativo = true;
		this.criadoEm = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		normalizarCampos();
		if (this.ativo == null) {
			this.ativo = true;
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
		if (this.codigo != null) {
			this.codigo = this.codigo.trim().toUpperCase(Locale.ROOT);
		}
		if (this.nome != null) {
			this.nome = this.nome.trim();
		}
	}

	public void desativar() {
		this.ativo = false;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo != null ? codigo.trim().toUpperCase(Locale.ROOT) : null;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public BigDecimal getValorMensalidade() {
		return valorMensalidade;
	}

	public void setValorMensalidade(BigDecimal valorMensalidade) {
		this.valorMensalidade = valorMensalidade;
	}

	public Integer getMaxBeneficiosOfertados() {
		return maxBeneficiosOfertados;
	}

	public void setMaxBeneficiosOfertados(Integer maxBeneficiosOfertados) {
		this.maxBeneficiosOfertados = maxBeneficiosOfertados;
	}

	public Integer getCotaResgatesMensal() {
		return cotaResgatesMensal;
	}

	public void setCotaResgatesMensal(Integer cotaResgatesMensal) {
		this.cotaResgatesMensal = cotaResgatesMensal;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}

}
