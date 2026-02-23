package com.mymicroservice.orderservice.util;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@UtilityClass
public class Uuid7PartitionUtils {

    public static UUID getStartOfDayUuid(LocalDate date) {
        long startOfDayMillis = date.atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        return UuidCreator.getTimeOrderedEpochMin(Instant.ofEpochMilli(startOfDayMillis));
    }

    public static UUID getEndOfDayUuid(LocalDate date) {
        long endOfDayMillis = date.plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        UUID nextDayStart = UuidCreator.getTimeOrderedEpochMax(Instant.ofEpochMilli(endOfDayMillis));
        return decrementUuid(nextDayStart);
    }

    private static UUID decrementUuid(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        if (lsb != 0L) {
            lsb--;
        } else {
            lsb = 0xFFFFFFFFFFFFFFFFL;
            msb--;
        }

        return new UUID(msb, lsb);
    }
}
