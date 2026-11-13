package com.alissondev.nexushub.validation;

public final class CnpjUtils {

	private static final int[] WEIGHTS_FIRST_DIGIT = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] WEIGHTS_SECOND_DIGIT = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

	private CnpjUtils() {
	}

	public static String sanitize(String cnpj) {
		if (cnpj == null) {
			return null;
		}
		return cnpj.replaceAll("\\D", "");
	}

	public static boolean isValid(String cnpj) {
		if (cnpj == null) {
			return false;
		}
		String digits = sanitize(cnpj);
		if (digits.length() != 14) {
			return false;
		}
		if (digits.chars().distinct().count() == 1) {
			return false;
		}

		int firstCheckDigit = calculateDigit(digits, WEIGHTS_FIRST_DIGIT);
		int secondCheckDigit = calculateDigit(digits, WEIGHTS_SECOND_DIGIT);

		return (digits.charAt(12) - '0') == firstCheckDigit
				&& (digits.charAt(13) - '0') == secondCheckDigit;
	}

	private static int calculateDigit(String digits, int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++) {
			sum += (digits.charAt(i) - '0') * weights[i];
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}

}
