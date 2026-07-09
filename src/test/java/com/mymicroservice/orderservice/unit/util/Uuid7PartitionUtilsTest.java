package com.mymicroservice.orderservice.unit.util;

import com.mymicroservice.orderservice.util.Uuid7PartitionUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Uuid7PartitionUtilsTest {

    @Test
    void getStartOfDayUuid_ShouldReturnUuid_WhenDateIsValid() {
        UUID start = Uuid7PartitionUtils.getStartOfDayUuid(LocalDate.of(2025, 1, 15));

        assertNotNull(start);
    }

    @Test
    void getEndOfDayUuid_ShouldReturnUuidAfterStart_WhenDateIsValid() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        UUID start = Uuid7PartitionUtils.getStartOfDayUuid(date);
        UUID end = Uuid7PartitionUtils.getEndOfDayUuid(date);

        assertNotNull(end);
        assertTrue(end.compareTo(start) > 0);
    }

    @Test
    void getEndOfDayUuid_ShouldHandleLsbZeroBranch_WhenDecrementNeeded() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        UUID end = Uuid7PartitionUtils.getEndOfDayUuid(date);

        assertNotNull(end);
    }
}
