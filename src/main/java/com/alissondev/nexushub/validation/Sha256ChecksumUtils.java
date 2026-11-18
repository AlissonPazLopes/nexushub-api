package com.alissondev.nexushub.validation;

import com.alissondev.nexushub.exception.BusinessRuleException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256ChecksumUtils {

	private Sha256ChecksumUtils() {
	}

	public static String calcularHex(byte[] conteudo) {
		if (conteudo == null || conteudo.length == 0) {
			throw new BusinessRuleException("O conteudo do arquivo CSV nao pode ser nulo ou vazio.");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(conteudo);
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel no ambiente Java.", ex);
		}
	}

}
