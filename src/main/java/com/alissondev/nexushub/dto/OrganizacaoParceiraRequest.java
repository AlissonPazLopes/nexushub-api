package com.alissondev.nexushub.dto;

import com.alissondev.nexushub.domain.enums.VerticalTech;
import com.alissondev.nexushub.validation.Cnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrganizacaoParceiraRequest(
		@NotBlank(message = "O CNPJ da organizacao e obrigatorio.")
		@Cnpj(message = "CNPJ invalido.")
		String cnpj,

		@NotBlank(message = "O nome fantasia e obrigatorio.")
		@Size(max = 150, message = "O nome fantasia deve ter no maximo 150 caracteres.")
		String nomeFantasia,

		@NotBlank(message = "A razao social e obrigatoria.")
		@Size(max = 200, message = "A razao social deve ter no maximo 200 caracteres.")
		String razaoSocial,

		@NotBlank(message = "O e-mail de contato e obrigatorio.")
		@Email(message = "O e-mail de contato deve ser valido.")
		@Size(max = 150, message = "O e-mail de contato deve ter no maximo 150 caracteres.")
		String emailContato,

		@NotNull(message = "A vertical tecnologica e obrigatoria.")
		VerticalTech verticalTech,

		@NotNull(message = "O plano de credenciamento e obrigatorio.")
		UUID planoId
) {
}
