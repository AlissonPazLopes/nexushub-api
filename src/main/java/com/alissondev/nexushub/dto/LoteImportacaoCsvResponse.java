package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.entity.LoteImportacaoCsv;
import com.alissondev.nexushub.domain.enums.StatusLoteCsv;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoteImportacaoCsvResponse(
		UUID id,
		String nomeArquivo,
		String hashSha256,
		StatusLoteCsv status,
		Integer totalLinhas,
		Integer linhasSucesso,
		Integer linhasErro,
		String resumoErros,
		LocalDateTime iniciadoEm,
		LocalDateTime finalizadoEm
) {

	public static LoteImportacaoCsvResponse fromEntity(LoteImportacaoCsv entity) {
		return new LoteImportacaoCsvResponse(
				entity.getId(),
				entity.getNomeArquivo(),
				entity.getHashSha256(),
				entity.getStatus(),
				entity.getTotalLinhas(),
				entity.getLinhasSucesso(),
				entity.getLinhasErro(),
				entity.getResumoErros(),
				entity.getIniciadoEm(),
				entity.getFinalizadoEm()
		);
	}

}
