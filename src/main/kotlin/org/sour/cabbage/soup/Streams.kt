package org.sour.cabbage.soup

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.*


class KafkaStreamsConfig(
    val topologyBuilder: StreamsBuilder.() -> Unit,
    val streamsConfig: Map<String, Any>,
    val builder: StreamsBuilder
)

fun kafkaStreams(configuration: KafkaStreamsConfig): KafkaStreams {
    configuration.builder.buildTopology(configuration.topologyBuilder)
    return KafkaStreams(configuration.builder.build(), buildConfigs(configuration.streamsConfig))
}

private fun buildConfigs(streamsConfig: Map<String, Any>): Properties {
    val props = Properties()
    props.putAll(streamsConfig)
    return props
}

private fun StreamsBuilder.buildTopology(builderBlock: StreamsBuilder.() -> Unit) : StreamsBuilder {
    this.builderBlock()
    return this
}