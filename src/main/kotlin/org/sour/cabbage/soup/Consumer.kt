package org.sour.cabbage.soup

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

fun <K, V> consumer(consumerConfig: Map<String, Any>): KafkaConsumer<K, V> {
    return KafkaConsumer(consumerProperties(consumerConfig))
}

fun consumerProperties(config: Map<String, Any>): Properties = Properties().apply { putAll(config) }
