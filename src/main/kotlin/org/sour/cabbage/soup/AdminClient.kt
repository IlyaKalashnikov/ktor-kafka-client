package org.sour.cabbage.soup

import io.ktor.server.config.*
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*

fun buildKafkaAdmin(config: ApplicationConfig): Admin {

    val logger = LoggerFactory.getLogger(Kafka::class.java)

    val adminProperties: Properties = Properties().apply {
        try {
            put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.propertyOrNull("ktor.kafka.bootstrap-servers")?.getString()
            )
        } catch (npe: NullPointerException) {
            logger.error("Please specify 'ktor.kafka.bootstrap-servers' properly, configuration was not provided")
        }
    }
    return Admin.create(adminProperties)
}