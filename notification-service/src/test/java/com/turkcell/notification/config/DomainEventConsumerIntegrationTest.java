package com.turkcell.notification.config;

import com.turkcell.notification.repository.ProcessedEventRepository;
import com.turkcell.notification.service.MockNotificationDispatcher;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * telco.customer.events'e ayni event_id ile IKI KEZ mesaj gonderilir (outbox pattern'in "en az bir
 * kez teslim" garantisinin dogal sonucu) - consumer'in bunu tek sefer isledigini (processed_events
 * tablosunda tek satir, MockNotificationDispatcher tek cagri) dogrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DomainEventConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("notification_db_test")
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
        registry.add("spring.cloud.function.definition", () -> "customerEvents");
        registry.add("spring.cloud.stream.bindings.customerEvents-in-0.destination", () -> "telco.customer.events");
        registry.add("spring.cloud.stream.bindings.customerEvents-in-0.group", () -> "notification-service-test");
        registry.add("spring.cloud.stream.bindings.customerEvents-in-0.content-type", () -> "application/json");
    }

    @MockBean
    private MockNotificationDispatcher mockNotificationDispatcher;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private Producer<String, String> kafkaProducer;

    @BeforeEach
    void setUpProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        kafkaProducer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDownProducer() {
        kafkaProducer.close();
    }

    @Test
    void duplicateEventDeliveryIsProcessedOnlyOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String envelope = """
                {"eventId":"%s","aggregateType":"Customer","aggregateId":"%s","eventType":"CustomerCreated","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(eventId, aggregateId);

        kafkaProducer.send(new ProducerRecord<>("telco.customer.events", aggregateId.toString(), envelope)).get(10, TimeUnit.SECONDS);
        kafkaProducer.send(new ProducerRecord<>("telco.customer.events", aggregateId.toString(), envelope)).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processedEventRepository.existsByEventId(eventId)).isTrue());

        // Ikinci mesajin da tuketilip elenmesi icin kisa bir ek bekleme.
        Thread.sleep(3000);

        assertThat(processedEventRepository.count()).isEqualTo(1);
        Mockito.verify(mockNotificationDispatcher, Mockito.times(1))
                .dispatch("telco.customer.events", "CustomerCreated", aggregateId.toString());
    }
}
