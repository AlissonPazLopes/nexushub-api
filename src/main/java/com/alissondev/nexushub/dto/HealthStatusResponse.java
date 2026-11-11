package com.alissondev.nexushub.dto;

import java.time.Instant;

public record HealthStatusResponse(
		String status,
		String service,
		String javaVersion,
		Instant timestamp
) {
}
