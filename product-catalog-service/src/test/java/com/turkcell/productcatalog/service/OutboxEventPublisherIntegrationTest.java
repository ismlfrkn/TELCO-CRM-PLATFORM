package com.turkcell.productcatalog.service;

import com.turkcell.productcatalog.dto.request.TariffCreateRequest;
import com.turkcell.productcatalog.dto.response.TariffResponse;
import com.turkcell.productcatalog.entity.OutboxEvent;
import com.turkcell.productcatalog.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * outbox_events tablosuna yazilan bir satirin OutboxEventPublisher tarafindan gercek bir Kafka
 * broker'a (Testcontainers, docker-compose.yml'deki ile ayni apache/kafka:3.7.0 image'i) basildigini
 * ve satirin PUBLISHED'e cevrildigini dogrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OutboxEventPublisherIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("product_catalog_db_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
        registry.add("spring.cloud.stream.bindings.catalogEvents-out-0.destination", () -> "telco.catalog.events");
        registry.add("spring.cloud.stream.bindings.catalogEvents-out-0.content-type", () -> "application/json");
    }

    @Autowired
    private TariffService tariffService;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Consumer<String, String> kafkaConsumer;

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList("telco.catalog.events"));
    }

    @AfterEach
    void tearDownConsumer() {
        kafkaConsumer.close();
    }

    @Test
    void publishesPendingOutboxEventToKafkaAndMarksPublished() {
        TariffCreateRequest request = new TariffCreateRequest();
        request.setCode("TRF-" + UUID.randomUUID().toString().substring(0, 8));
        request.setName("Test Tariff");
        request.setType("POSTPAID");
        request.setMonthlyFee(new BigDecimal("99.90"));
        request.setMinutesIncluded(1000);
        request.setSmsIncluded(500);
        request.setDataMbIncluded(10000);
        request.setStatus("ACTIVE");

        TariffResponse created = tariffService.createTariff(request);

        outboxEventPublisher.pollAndPublish();

        ConsumerRecord<String, String> record = pollForRecord();
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(created.getId().toString());
        assertThat(record.value()).contains("\"eventType\":\"TariffCreated\"");
        assertThat(record.value()).contains(created.getId().toString());

        Optional<OutboxEvent> saved = outboxEventRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(created.getId()))
                .findFirst();
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(OutboxEvent.STATUS_PUBLISHED);
        assertThat(saved.get().getPublishedAt()).isNotNull();
    }

    private ConsumerRecord<String, String> pollForRecord() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                return record;
            }
        }
        return null;
    }
}
