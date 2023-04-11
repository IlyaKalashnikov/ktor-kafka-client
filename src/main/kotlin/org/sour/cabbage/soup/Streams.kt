package org.sour.cabbage.soup

import io.ktor.server.application.*
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import java.util.*


class KafkaStreamsConfig<K, V>(
    val topic: String,
    val topologyBuilder: KStream<K, V>.() -> Unit,
    val keySerde: Serde<K>,
    val valueSerde: Serde<V>,
    val context: ApplicationEnvironment,
    val builder: StreamsBuilder
)

fun <K, V> kafkaStreams(configuration: KafkaStreamsConfig<K, V>): KafkaStreams {
    val stream = configuration.builder.stream<K, V>(
        configuration.topic,
        Consumed.with(
            configuration.keySerde,
            configuration.valueSerde
        )
    )
    stream.buildTopology(configuration.topologyBuilder)

    return KafkaStreams(configuration.builder.build(), buildConfigs(configuration))
}

private fun <K, V> buildConfigs(configuration: KafkaStreamsConfig<K, V>): Properties { //TODO: make it more configurable
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] =
        configuration.context.config.propertyOrNull("ktor.kafka.application-id")?.getString()
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] =
        configuration.context.config.propertyOrNull("ktor.kafka.bootstrap-servers")?.getString()
    return props
}

private fun <K, V> KStream<K, V>.buildTopology(builderBlock: KStream<K, V>.() -> Unit): KStream<K, V> {
    this.builderBlock()
    return this
}