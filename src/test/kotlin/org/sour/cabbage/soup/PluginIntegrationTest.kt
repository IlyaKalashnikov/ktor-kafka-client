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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.utility.DockerImageName.parse
import java.time.Duration
import java.util.concurrent.*

class PluginIntegrationTest {

    companion object {
        lateinit var kafka: KafkaContainer

        @JvmStatic
        @BeforeAll
        fun setUp() {
            kafka = KafkaContainer(parse("confluentinc/cp-kafka:7.0.1")).apply {
                withKraft()
                withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "true")
                start()
            }
        }
    }

    @AfterEach
    fun tearDown() {
    }


    @Test
    @DisplayName("should start KafkaProducer send to topic and KafkaConsumer consume from topic")
    fun testDevEnvironment() = testApplication {
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

            var producer = producer<String, String>(
                mapOf(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
                )
            )
            var consumer = consumer<String, String>(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.GROUP_ID_CONFIG to "test-consumer-group",
                    ConsumerConfig.CLIENT_ID_CONFIG to "test-consumer-client"
                )
            )

            producer.send(ProducerRecord(topic, "testKey", "testVal"))
            producer.flush()


            val actual: MutableList<String> = CopyOnWriteArrayList()
            val service: ExecutorService = Executors.newSingleThreadExecutor()
            val consumingTask: Future<*> = service.submit {
                while (!Thread.currentThread().isInterrupted) {
                    val records: ConsumerRecords<String, String> =
                        consumer.poll(Duration.ofMillis(100))
                    for (rec in records) {
                        actual.add(rec.value())
                    }
                }
            }

            try {
                await()
                    .atMost(5, TimeUnit.SECONDS)
                    .until { listOf("testVal") == actual }
            } finally {
                consumingTask.cancel(true)
                service.awaitTermination(200, TimeUnit.MILLISECONDS)
            }
        }
    }
}
