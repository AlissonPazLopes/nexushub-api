package com.alissondev.nexushub.controller;

import com.alissondev.nexushub.dto.HealthStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	@GetMapping
	public ResponseEntity<HealthStatusResponse> checkHealth() {
		HealthStatusResponse response = new HealthStatusResponse(
				"UP",
				"nexushub-api",
				System.getProperty("java.version"),
				Instant.now()
		);
		return ResponseEntity.ok(response);
	}

}
