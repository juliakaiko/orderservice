package com.mymicroservice.orderservice.model.enums;

public enum OutboxEventStatus {
    INITIAL,    // Event created, awaiting processing
    PROCESSED,  // Picked up by scheduler, being processed
    SENT,       // Successfully published to Kafka
    FAILED      // Permanent failure, no further attempts
}
