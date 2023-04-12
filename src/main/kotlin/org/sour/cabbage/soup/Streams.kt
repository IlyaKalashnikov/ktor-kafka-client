package org.sour.cabbage.soup

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import java.util.*


class KafkaStreamsConfig<K, V>(
    val topic: String,
    val topologyBuilder: KStream<K, V>.() -> Unit,
    val streamsConfig: Map<String, Any>,
    val builder: StreamsBuilder
)

fun <K, V> kafkaStreams(configuration: KafkaStreamsConfig<K, V>): KafkaStreams {
    val stream = configuration.builder.stream<K, V>(configuration.topic)
    stream.buildTopology(configuration.topologyBuilder)

    return KafkaStreams(configuration.builder.build(), buildConfigs(configuration.streamsConfig))
}

private fun buildConfigs(streamsConfig: Map<String, Any>): Properties {
    val props = Properties()
    props.putAll(streamsConfig)
    return props
}

private fun <K, V> KStream<K, V>.buildTopology(builderBlock: KStream<K, V>.() -> Unit): KStream<K, V> {
    this.builderBlock()
    return this
}