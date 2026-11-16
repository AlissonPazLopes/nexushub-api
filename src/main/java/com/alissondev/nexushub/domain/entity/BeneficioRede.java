package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusBeneficio;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "network_benefits")
public class BeneficioRede {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organizacao_ofertante_id", nullable = false)
	private OrganizacaoParceira organizacaoOfertante;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plano_minimo_id", nullable = false)
	private PlanoCredenciamento planoMinimo;

	@Column(name = "titulo", nullable = false, length = 150)
	private String titulo;

	@Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
	private String descricao;

	@Column(name = "cota_total", nullable = false)
	private Integer cotaTotal;

	@Column(name = "cota_utilizada", nullable = false)
	private Integer cotaUtilizada = 0;

	@Column(name = "valido_ate", nullable = false)
	private LocalDate validoAte;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusBeneficio status = StatusBeneficio.ATIVO;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	public BeneficioRede() {
	}

	public BeneficioRede(
			OrganizacaoParceira organizacaoOfertante,
			PlanoCredenciamento planoMinimo,
			String titulo,
			String descricao,
			Integer cotaTotal,
			LocalDate validoAte
	) {
		this.organizacaoOfertante = organizacaoOfertante;
		this.planoMinimo = planoMinimo;
		this.titulo = titulo != null ? titulo.trim() : null;
		this.descricao = descricao != null ? descricao.trim() : null;
		this.cotaTotal = cotaTotal;
		this.cotaUtilizada = 0;
		this.validoAte = validoAte;
		this.status = StatusBeneficio.ATIVO;
		this.criadoEm = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		normalizarCampos();
		if (this.cotaUtilizada == null) {
			this.cotaUtilizada = 0;
		}
		if (this.status == null) {
			this.status = StatusBeneficio.ATIVO;
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
		if (this.titulo != null) {
			this.titulo = this.titulo.trim();
		}
		if (this.descricao != null) {
			this.descricao = this.descricao.trim();
		}
	}

	public int cotaDisponivel() {
		int total = this.cotaTotal != null ? this.cotaTotal : 0;
		int utilizada = this.cotaUtilizada != null ? this.cotaUtilizada : 0;
		return Math.max(0, total - utilizada);
	}

	public boolean estaExpirado(LocalDate hoje) {
		return this.validoAte != null && hoje != null && this.validoAte.isBefore(hoje);
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public OrganizacaoParceira getOrganizacaoOfertante() {
		return organizacaoOfertante;
	}

	public void setOrganizacaoOfertante(OrganizacaoParceira organizacaoOfertante) {
		this.organizacaoOfertante = organizacaoOfertante;
	}

	public PlanoCredenciamento getPlanoMinimo() {
		return planoMinimo;
	}

	public void setPlanoMinimo(PlanoCredenciamento planoMinimo) {
		this.planoMinimo = planoMinimo;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Integer getCotaTotal() {
		return cotaTotal;
	}

	public void setCotaTotal(Integer cotaTotal) {
		this.cotaTotal = cotaTotal;
	}

	public Integer getCotaUtilizada() {
		return cotaUtilizada;
	}

	public void setCotaUtilizada(Integer cotaUtilizada) {
		this.cotaUtilizada = cotaUtilizada;
	}

	public LocalDate getValidoAte() {
		return validoAte;
	}

	public void setValidoAte(LocalDate validoAte) {
		this.validoAte = validoAte;
	}

	public StatusBeneficio getStatus() {
		return status;
	}

	public void setStatus(StatusBeneficio status) {
		this.status = status;
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
