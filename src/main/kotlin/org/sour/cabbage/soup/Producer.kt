package org.sour.cabbage.soup

import org.apache.kafka.clients.producer.KafkaProducer
import java.util.Properties

fun <K, V> producer(producerConfig: Map<String, Any>): KafkaProducer<K, V> {
    return KafkaProducer(producerProperties(producerConfig))
}

fun producerProperties(config: Map<String, Any>): Properties {
    val props = Properties()
    props.putAll(config)
    return props
}
