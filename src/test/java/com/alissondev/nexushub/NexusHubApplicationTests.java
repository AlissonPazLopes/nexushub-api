package com.alissondev.nexushub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NexusHubApplicationTests {

	@Test
	@DisplayName("Deve carregar o contexto da aplicacao NexusHub API no perfil de teste")
	void contextLoads() {
	}

}
