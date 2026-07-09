package com.mymicroservice.orderservice.unit.config;

import com.mymicroservice.orderservice.config.FeignConfig;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FeignConfigTest {

    private final FeignConfig feignConfig = new FeignConfig();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void internalCallInterceptor_ShouldAddInternalCallHeader_WhenApplied() {
        RequestInterceptor interceptor = feignConfig.internalCallInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("true", template.headers().get("X-Internal-Call").iterator().next());
    }

    @Test
    void internalCallInterceptor_ShouldPropagateTraceId_WhenPresentInMdc() {
        MDC.put("traceId", "trace-123");
        RequestInterceptor interceptor = feignConfig.internalCallInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("trace-123", template.headers().get("X-Trace-Id").iterator().next());
    }

    @Test
    void internalCallInterceptor_ShouldSkipTraceHeader_WhenMdcIsEmpty() {
        RequestInterceptor interceptor = feignConfig.internalCallInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get("X-Trace-Id"));
    }
}
