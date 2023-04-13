package org.sour.cabbage.soup

import io.ktor.server.application.*
import io.ktor.util.*
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory

class Kafka(
    private val config: Configuration
) {
    private val logger = LoggerFactory.getLogger(Kafka::class.java)

    class Configuration {
        var kafkaConfig: Map<String, Any>? = null
        var topics: List<NewTopic> = listOf()
    }

    fun createTopics() {
        val admin = buildKafkaAdmin(config.kafkaConfig!!) //TODO: accurate null-safety
        admin.createTopics(config.topics)

        config.topics.forEach {
            logger.info("Created topic ${it.name()} with ${it.numPartitions()} partitions and ${it.replicationFactor()}")
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, Kafka> {
        override val key: AttributeKey<Kafka>
            get() = AttributeKey("Kafka")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): Kafka {
            val configuration = Configuration().apply(configure)
            val kafkaFeature = Kafka(configuration)

            kafkaFeature.createTopics()
            return kafkaFeature
        }
    }
}