package com.mymicroservice.orderservice.service.impl;

import com.mymicroservice.orderservice.service.PartitionService;
import com.mymicroservice.orderservice.util.Uuid7PartitionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartitionServiceImpl implements PartitionService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void createPartitionForDate(LocalDate date) {

        UUID start = Uuid7PartitionUtils.getStartOfDayUuid(date);
        UUID end = Uuid7PartitionUtils.getEndOfDayUuid(date);

        String partitionName = "orders_" +
                date.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

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
