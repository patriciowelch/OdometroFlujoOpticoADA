package com.company.warehousevio.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MessageCodec {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: ProtocolMessage): String =
        json.encodeToString(message) + "\n"

    fun decode(line: String): ProtocolMessage =
        json.decodeFromString(line.trim())
}
