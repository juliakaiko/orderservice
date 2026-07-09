package com.mymicroservice.orderservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonConstants {

    public static final String IDEMPOTENCE_ID = "X-Idempotence-Id";
    public static final String EVENT_TYPE = "X-Event-Type";
    public static final String TRACE_ID = "X-Trace-Id";
    public static final String SOURCE_SERVICE = "X-Source-Service";

    public static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    public static final String SOURCE_SERVICE_HEADER = "X-Source-Service";
    public static final String GATEWAY_SERVICE_NAME = "gateway";
}
