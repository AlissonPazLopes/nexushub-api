package com.alissondev.nexushub.domain.entity;

import com.alissondev.nexushub.domain.enums.StatusLoteCsv;
import com.alissondev.nexushub.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "csv_import_batches")
public class LoteImportacaoCsv {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "nome_arquivo", nullable = false, length = 255)
	private String nomeArquivo;

	@Column(name = "hash_sha256", nullable = false, unique = true, length = 64)
	private String hashSha256;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusLoteCsv status = StatusLoteCsv.RECEBIDO;

	@Column(name = "total_linhas", nullable = false)
	private Integer totalLinhas = 0;

	@Column(name = "linhas_sucesso", nullable = false)
	private Integer linhasSucesso = 0;

	@Column(name = "linhas_erro", nullable = false)
	private Integer linhasErro = 0;

	@Column(name = "resumo_erros", columnDefinition = "TEXT")
	private String resumoErros;

	@Column(name = "iniciado_em", nullable = false, updatable = false)
	private LocalDateTime iniciadoEm;

	@Column(name = "finalizado_em")
	private LocalDateTime finalizadoEm;

	public LoteImportacaoCsv() {
	}

	public LoteImportacaoCsv(String nomeArquivo, String hashSha256) {
		this.nomeArquivo = nomeArquivo != null ? nomeArquivo.trim() : null;
		this.hashSha256 = hashSha256 != null ? hashSha256.trim().toLowerCase(Locale.ROOT) : null;
		this.status = StatusLoteCsv.RECEBIDO;
		this.totalLinhas = 0;
		this.linhasSucesso = 0;
		this.linhasErro = 0;
		this.iniciadoEm = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		if (this.nomeArquivo != null) {
			this.nomeArquivo = this.nomeArquivo.trim();
		}
		if (this.hashSha256 != null) {
			this.hashSha256 = this.hashSha256.trim().toLowerCase(Locale.ROOT);
		}
		if (this.status == null) {
			this.status = StatusLoteCsv.RECEBIDO;
		}
		if (this.totalLinhas == null) {
			this.totalLinhas = 0;
		}
		if (this.linhasSucesso == null) {
			this.linhasSucesso = 0;
		}
		if (this.linhasErro == null) {
			this.linhasErro = 0;
		}
		if (this.iniciadoEm == null) {
			this.iniciadoEm = LocalDateTime.now();
		}
	}

	public void iniciarProcessamento(int totalLinhas) {
		if (this.status != StatusLoteCsv.RECEBIDO) {
			throw new BusinessRuleException(
					"Apenas lotes com status RECEBIDO podem iniciar processamento. Status atual: " + this.status
			);
		}
		if (totalLinhas < 0) {
			throw new BusinessRuleException("O total de linhas do lote nao pode ser negativo.");
		}
		this.totalLinhas = totalLinhas;
		this.status = StatusLoteCsv.PROCESSANDO;
	}

	public void registrarSucesso() {
		validarEmProcessamento();
		this.linhasSucesso = (this.linhasSucesso != null ? this.linhasSucesso : 0) + 1;
	}

	public void registrarErro(int linha, String motivo) {
		validarEmProcessamento();
		this.linhasErro = (this.linhasErro != null ? this.linhasErro : 0) + 1;
		String entradaErro = "Linha " + linha + ": " + (motivo != null ? motivo.trim() : "Erro de validacao");
		if (this.resumoErros == null || this.resumoErros.isBlank()) {
			this.resumoErros = entradaErro;
		} else {
			this.resumoErros = this.resumoErros + "\n" + entradaErro;
		}
	}

	public void finalizarProcessamento() {
		validarEmProcessamento();
		int erros = this.linhasErro != null ? this.linhasErro : 0;
		this.status = erros > 0 ? StatusLoteCsv.CONCLUIDO_COM_ERROS : StatusLoteCsv.CONCLUIDO;
		this.finalizadoEm = LocalDateTime.now();
	}

	public void registrarFalhaCritica(String motivo) {
		if (this.status != StatusLoteCsv.RECEBIDO && this.status != StatusLoteCsv.PROCESSANDO) {
			throw new BusinessRuleException(
					"Nao e possivel registrar falha critica em um lote ja finalizado. Status atual: " + this.status
			);
		}
		this.status = StatusLoteCsv.FALHA;
		String mensagemFalha = motivo != null && !motivo.isBlank()
				? motivo.trim()
				: "Falha critica no processamento do lote CSV.";
		if (this.resumoErros == null || this.resumoErros.isBlank()) {
			this.resumoErros = mensagemFalha;
		} else {
			this.resumoErros = this.resumoErros + "\n" + mensagemFalha;
		}
		this.finalizadoEm = LocalDateTime.now();
	}

	private void validarEmProcessamento() {
		if (this.status != StatusLoteCsv.PROCESSANDO) {
			throw new BusinessRuleException(
					"A operacao exige que o lote esteja em status PROCESSANDO. Status atual: " + this.status
			);
		}
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getNomeArquivo() {
		return nomeArquivo;
	}

	public void setNomeArquivo(String nomeArquivo) {
		this.nomeArquivo = nomeArquivo;
	}

	public String getHashSha256() {
		return hashSha256;
	}

	public void setHashSha256(String hashSha256) {
		this.hashSha256 = hashSha256;
	}

	public StatusLoteCsv getStatus() {
		return status;
	}

	public void setStatus(StatusLoteCsv status) {
		this.status = status;
	}

	public Integer getTotalLinhas() {
		return totalLinhas;
	}

	public void setTotalLinhas(Integer totalLinhas) {
		this.totalLinhas = totalLinhas;
	}

	public Integer getLinhasSucesso() {
		return linhasSucesso;
	}

	public void setLinhasSucesso(Integer linhasSucesso) {
		this.linhasSucesso = linhasSucesso;
	}

	public Integer getLinhasErro() {
		return linhasErro;
	}

	public void setLinhasErro(Integer linhasErro) {
		this.linhasErro = linhasErro;
	}

	public String getResumoErros() {
		return resumoErros;
	}

	public void setResumoErros(String resumoErros) {
		this.resumoErros = resumoErros;
	}

	public LocalDateTime getIniciadoEm() {
		return iniciadoEm;
	}

	public void setIniciadoEm(LocalDateTime iniciadoEm) {
		this.iniciadoEm = iniciadoEm;
	}

	public LocalDateTime getFinalizadoEm() {
		return finalizadoEm;
	}

	public void setFinalizadoEm(LocalDateTime finalizadoEm) {
		this.finalizadoEm = finalizadoEm;
	}

}
