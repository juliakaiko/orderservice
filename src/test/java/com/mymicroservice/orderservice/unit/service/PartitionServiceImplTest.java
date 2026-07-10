package com.mymicroservice.orderservice.unit.service;

import com.mymicroservice.orderservice.service.impl.PartitionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartitionServiceImplTest {

    @InjectMocks
    private PartitionServiceImpl partitionService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPartitionForDate_ShouldExecuteSql_WhenDateIsProvided() {
        LocalDate date = LocalDate.of(2025, 3, 10);

        partitionService.createPartitionForDate(date);

        verify(jdbcTemplate).execute(anyString());
    }
}
