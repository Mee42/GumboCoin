package com.gumbocoin.server

import io.rsocket.Payload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.base.*
import java.nio.charset.Charset

private fun Flux<String>.toPayloads() :Flux<Payload> = this.map { it.toPayload() }


fun getStreamHandler(payloadInitial : StreamDataBlob) :(StreamDataBlob) -> Flux<Payload> = when(payloadInitial.intent){
    Request.Stream.NUMBERS -> { pay ->
        Mono.fromCallable { pay.data.fromJson<SendableInteger>().value }
            .filter { it != null }
            .map { it!! }
            .flatMapMany { count -> Flux.range(0,count) }
            .map { it.toString() }
            .toPayloads()
    }
}

fun getResponseHandler(payloadInitial : RequestDataBlob):(RequestDataBlob) -> Payload {
    return when (payloadInitial.intent) {
        Request.Response.PING -> { _ -> "pong".toPayload() }
        Request.Response.DECRYPT -> { pay ->
            val plain = server.decryptAES(pay.data.fromJson<EncryptedString>().toBytes())
            "cli:\"${plain.toString(Charset.forName("UTF-8"))}\"".toPayload()
        }
        Request.Response.VERIFIED -> { pay -> SendableBoolean(pay.isVerified).send().toPayload() }
        Request.Response.PUBLIC_KEY -> { pay ->
            val str = pay.data.fromJson<SendableString>()
            val person = Person.deserialize(str.value)
            (if(people.containsKey(pay.clientID))
                Status(failed = true,
                    errorMessage = "Server already has a key registered under that clientID",
                    extraData = "clientID:${pay.clientID}")
            else{
                people[pay.clientID] = person
                Status(failed = false)
            }).send().toPayload()
        }
    }
}
