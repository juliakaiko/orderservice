package com.mymicroservice.orderservice.scheduler;

import com.mymicroservice.orderservice.service.impl.PartitionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionScheduler {

    private final PartitionServiceImpl partitionService;

    @Scheduled(cron = "0 29 12 * * ?")
    @SchedulerLock(name = "createPartitionTask",
            lockAtMostFor = "PT5M",
            lockAtLeastFor = "PT1M")
    public void createPartition() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        partitionService.createPartitionForDate(tomorrow);

        log.info("Partition creation job finished");
    }
}
