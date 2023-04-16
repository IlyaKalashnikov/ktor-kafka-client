package org.sour.cabbage.soup

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

fun <K, V> consumer(consumerConfig: Map<String, Any>, topics: List<String>): KafkaConsumer<K, V> {
    val consumer = KafkaConsumer<K, V>(consumerProperties(consumerConfig))
    consumer.subscribe(topics)
    return consumer
}

fun consumerProperties(config: Map<String, Any>): Properties = Properties().apply { putAll(config) }
