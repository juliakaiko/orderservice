package com.mymicroservice.orderservice.integration.repository;

import com.mymicroservice.orderservice.configuration.AbstractContainerTest;
import com.mymicroservice.orderservice.model.OutboxEvent;
import com.mymicroservice.orderservice.model.enums.OutboxEventStatus;
import com.mymicroservice.orderservice.repository.OutboxEventRepository;
import com.mymicroservice.orderservice.util.OutboxEventGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.mymicroservice.orderservice.util.data.TestConstants.ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxEventRepositoryTest extends AbstractContainerTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void findEventsForProcessing_ShouldReturnInitialEvents_WhenStatusMatches() {
        var initialEvent = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.INITIAL);
        var sentEvent = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.SENT);
        outboxEventRepository.save(initialEvent);
        outboxEventRepository.save(sentEvent);

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                List.of(OutboxEventStatus.INITIAL.name(), OutboxEventStatus.FAILED.name()), 10);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(OutboxEventStatus.INITIAL);
    }

    @Test
    void findEventsForProcessing_ShouldReturnFailedEvents_WhenRetryIsNeeded() {
        var failedEvent = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.FAILED);
        outboxEventRepository.save(failedEvent);

        List<OutboxEvent> events = outboxEventRepository.findEventsForProcessing(
                List.of(OutboxEventStatus.INITIAL.name(), OutboxEventStatus.FAILED.name()), 10);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    @Transactional
    void resetStaleProcessingEvents_ShouldMoveProcessingToFailed_WhenUpdatedAtIsStale() {
        OutboxEvent staleEvent = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.PROCESSING);
        staleEvent.setUpdatedAt(LocalDateTime.now().minusMinutes(10));
        outboxEventRepository.save(staleEvent);

        OutboxEvent freshEvent = OutboxEventGenerator.generateOutboxEvent(
                UUID.randomUUID(), ENTITY_ID, OutboxEventStatus.PROCESSING);
        freshEvent.setUpdatedAt(LocalDateTime.now());
        outboxEventRepository.save(freshEvent);

        int updated = outboxEventRepository.resetStaleProcessingEvents(LocalDateTime.now().minusMinutes(5));

        assertThat(updated).isEqualTo(1);
        assertThat(outboxEventRepository.findById(staleEvent.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);
        assertThat(outboxEventRepository.findById(freshEvent.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSING);
    }
}
