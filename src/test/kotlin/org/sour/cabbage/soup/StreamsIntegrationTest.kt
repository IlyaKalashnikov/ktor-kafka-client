package org.sour.cabbage.soup

import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.awaitility.Awaitility
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.*


class StreamsIntegrationTest {
    companion object {
        lateinit var kafka: KafkaContainer

        @JvmStatic
        @BeforeAll
        fun setUp() {
            kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest")).apply {
                withKraft()
                withEnv("KAFKA_AUTO_CREATE_TOPIC_ENABLE", "true")
                start()
            }
        }
    }

    @Test
    @DisplayName(
        "Test should initiate Kafka topic, producer should be able to produce message," +
                " streams should be able to consume it, map values and store it in table"
    )
    fun producerProducesStreamsConsumeMapAndStore() = testApplication {
        println(kafka.bootstrapServers)
        val topic = "streamsTest"
        val table = "testTable"
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

            val streams = kafkaStreams(
                KafkaStreamsConfig(
                    topic = topic,
                    topologyBuilder = fun KStream<String, String>.() {
                        this.toTable(Materialized.`as`(table))
                    },
                    streamsConfig = mapOf(
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass,
                        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String().javaClass,
                        StreamsConfig.APPLICATION_ID_CONFIG to "test-app",
                        StreamsConfig.CLIENT_ID_CONFIG to "test-client",
                        StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1000,
                    ),
                    builder = StreamsBuilder()
                )
            )
            streams.start()


            //waiting for streams to be in RUNNING condition
            while (streams.state() != KafkaStreams.State.RUNNING) {
                TimeUnit.SECONDS.sleep(1)
            }

            if (streams.state() == KafkaStreams.State.RUNNING) {
                producer.send(ProducerRecord(topic, "testKey", "testVal"))
                producer.flush()
            }

            val actual: MutableList<String> = CopyOnWriteArrayList()
            val service: ExecutorService = Executors.newSingleThreadExecutor()
            val testTable: ReadOnlyKeyValueStore<String, String> = streams.store(
                StoreQueryParameters.fromNameAndType(
                    table,
                    QueryableStoreTypes.keyValueStore()
                )
            )

            val aggregationTask: Future<*> = service.submit {
                while (!Thread.currentThread().isInterrupted) {
                    val records = testTable.all()

                    for (rec in records) {
                        actual.add(rec.value)
                    }
                }
            }

            try {
                Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .until { actual[0] == "testVal" }
            } finally {
                aggregationTask.cancel(true)
                service.awaitTermination(200, TimeUnit.MILLISECONDS)
            }
        }

    }
}