package com.mymicroservice.orderservice.scheduler;

import com.mymicroservice.orderservice.util.Uuid7PartitionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 5 0 * * ?")
    @SchedulerLock(name = "createPartitionTask",
            lockAtMostFor = "PT5M",
            lockAtLeastFor = "PT1M")
    public void createPartition() {

        LocalDate today = LocalDate.now();
        UUID start = Uuid7PartitionUtils.getStartOfDayUuid(today);
        UUID end = Uuid7PartitionUtils.getEndOfDayUuid(today);

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        String partitionName = "orders_" +
                tomorrow.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        /*String partitionName = "orders_" +
                today.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));*/

        System.out.println("start: " + start);
        System.out.println("end: " + end);

        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s
                PARTITION OF orders
                FOR VALUES FROM ('%s') TO ('%s')
                """,
                partitionName,
                start,
                end
        );

        jdbcTemplate.execute(sql);

        log.info("Partition {} created", partitionName);
    }
}
