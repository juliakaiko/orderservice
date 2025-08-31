package com.mymicroservice.orderservice;

import com.mymicroservice.orderservice.configuration.AbstractContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
class OrderserviceApplicationTests extends AbstractContainerTest{

	@Test
	void contextLoads() {
	}

}
