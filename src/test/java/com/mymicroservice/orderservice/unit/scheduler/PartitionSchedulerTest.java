package com.mymicroservice.orderservice.unit.scheduler;

import com.mymicroservice.orderservice.scheduler.PartitionScheduler;
import com.mymicroservice.orderservice.service.PartitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartitionSchedulerTest {

    @InjectMocks
    private PartitionScheduler partitionScheduler;

    @Mock
    private PartitionService partitionService;

    @Test
    void createPartition_ShouldCreatePartitionForTomorrow_WhenJobRuns() {
        partitionScheduler.createPartition();

        verify(partitionService).createPartitionForDate(eq(LocalDate.now().plusDays(1)));
    }
}
