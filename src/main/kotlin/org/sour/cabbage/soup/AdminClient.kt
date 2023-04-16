package org.sour.cabbage.soup

import org.apache.kafka.clients.admin.Admin
import java.util.*

fun buildKafkaAdmin(adminConfig: Map<String, Any>): Admin {
    val adminProperties: Properties = Properties().apply { putAll(adminConfig) }
    return Admin.create(adminProperties)
}