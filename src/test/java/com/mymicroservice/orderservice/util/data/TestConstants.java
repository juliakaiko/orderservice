package com.mymicroservice.orderservice.util.data;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class TestConstants {

    public static final String SOURCE_SERVICE = "orderservice";
    public static final String SERVICE_NAME = "paymentservice";

    public static final String ENTITY_ID = "1";
    public static final String SECOND_ENTITY_ID = "2";
    public static final String PAYMENT_ID = "test-payment-1";
    public static final String TRACE_ID = "test-trace-id";
    public static final String IDEMPOTENCE_ID = "test-idempotence-id";

    public static final String CREATE_ORDER_EVENT_TYPE = "CREATE_ORDER";
    public static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";
    public static final String PAID_STATUS = "PAID";
    public static final String FAILED_STATUS = "FAILED";

    public static final Long TEST_USER_ID = 1L;
    public static final Long TEST_ITEM_ID = 2L;
    public static final String TEST_USER_EMAIL = "test@test.by";

    public static final UUID TEST_ORDER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static final String CREATE_ORDER_TOPIC = "create-order";
    public static final String CREATE_PAYMENT_TOPIC = "create-payment";

}
