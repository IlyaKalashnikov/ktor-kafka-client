package org.sour.cabbage.soup

import io.ktor.server.application.*
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
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

/**
 * For key serialization/deserialization process String-serializer/deserializer would be used by default
 * For value serialization/deserialization process ByteArray-serializer/deserializer would be used by default
 * Both of this configs would be overridden by provided keySerde-valueSerde in KafkaStreamsConfig
 */
private fun <K, V> buildConfigs(configuration: KafkaStreamsConfig<K, V>): Properties { //TODO: make it more configurable
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] =
        configuration.context.config.propertyOrNull("ktor.kafka.application-id")?.getString()
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] =
        configuration.context.config.propertyOrNull("ktor.kafka.bootstrap-servers")?.getString()
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.ByteArray().javaClass
    return props
}

private fun <K, V> KStream<K, V>.buildTopology(builderBlock: KStream<K, V>.() -> Unit): KStream<K, V> {
    this.builderBlock()
    return this
}