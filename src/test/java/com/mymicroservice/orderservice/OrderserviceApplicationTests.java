package com.mymicroservice.orderservice;

import com.mymicroservice.orderservice.config.AbstractContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderserviceApplicationTests extends AbstractContainerTest{

	@Test
	void contextLoads() {
	}

}
