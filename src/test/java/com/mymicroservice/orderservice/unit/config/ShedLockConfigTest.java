package com.mymicroservice.orderservice.unit.config;

import com.mymicroservice.orderservice.config.ShedLockConfig;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShedLockConfigTest {

    @Test
    void lockProvider_ShouldCreateBean_WhenDataSourceIsAvailable() {
        ShedLockConfig config = new ShedLockConfig();
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.toString()).thenReturn("mock-ds");

        LockProvider lockProvider = config.lockProvider(dataSource);

        assertNotNull(lockProvider);
        assertNotNull(new JdbcTemplate(dataSource));
    }
}
