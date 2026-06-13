package com.mymicroservice.orderservice.repository;

import com.mymicroservice.orderservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /*
        FOR UPDATE - пессимистическая блокировка
        SKIP LOCKED - инстанс не будет ждать, пока освободится лок, а пойдет искать свободные строки в  таблице
        Таким образом, таблица outbox будет вычитываться несколькими инстансами
     */
    @Query(value = "SELECT * FROM outbox_table " +
            "WHERE status IN (:statuses) " +
            "ORDER BY created_at " +
            "FOR UPDATE SKIP LOCKED LIMIT :limit",
            nativeQuery = true)
    List<OutboxEvent> findEventsForProcessing(@Param("statuses") List<String> statuses,
                                              @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE outbox_table SET status = 'FAILED', updated_at = NOW() " +
            "WHERE status = 'PROCESSING' AND updated_at < :threshold",
            nativeQuery = true)
    int resetStaleProcessingEvents(@Param("threshold") LocalDateTime threshold);
}
