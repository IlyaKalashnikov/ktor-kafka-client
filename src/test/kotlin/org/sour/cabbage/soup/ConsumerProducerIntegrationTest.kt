package org.sour.cabbage.soup

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName.parse
import java.time.Duration
import java.util.concurrent.*

class ConsumerProducerIntegrationTest {

    companion object {
        lateinit var kafka: KafkaContainer

        @JvmStatic
        @BeforeAll
        fun setUp() {
            kafka = KafkaContainer(parse("confluentinc/cp-kafka:latest")).apply {
                withKraft()
                withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "true")
                start()
            }
        }
    }

    @Test
    @DisplayName("Test should initiate Kafka topic, producer should be able to produce message, consumer should be able to consume it")
    fun producerProducesConsumerConsumes() = testApplication {
        val topic = "test"

        application {
            install(Kafka) {
                this.kafkaConfig = mapOf<String, Any>(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers
                )
                this.topics = listOf(
                    NewTopic(topic, 1, 1)
                )
            }

            val producer = producer<String, String>(
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
                )
            )

            val consumer = consumer<String, String>(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
                    ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to 1000,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-group",
                    ConsumerConfig.CLIENT_ID_CONFIG to "test-consumer-client"
                ),
                listOf(topic)
            )

            producer.send(ProducerRecord(topic, "testKey", "testVal"))
            producer.flush()

            val actual: MutableList<String> = CopyOnWriteArrayList()
            val service: ExecutorService = Executors.newSingleThreadExecutor()
            val consumingTask: Future<*> = service.submit {
                while (!Thread.currentThread().isInterrupted) {
                    val records: ConsumerRecords<String, String> =
                        consumer.poll(Duration.ofMillis(1000))
                    for (rec in records) {
                        actual.add(rec.value())
                    }
                }
            }

            try {
                await()
                    .atMost(120, TimeUnit.SECONDS)
                    .until { listOf("testVal") == actual }
            } finally {
                consumingTask.cancel(true)
                service.awaitTermination(200, TimeUnit.MILLISECONDS)
            }
        }
    }
}
