package com.gumbocoin.cli

import io.rsocket.RSocket
import io.rsocket.util.DefaultPayload
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.base.*
import java.nio.charset.Charset


val network = GLogger.logger("Network")

fun Mono<Status>.printStatus(message :String = "", loggger : GLogger = GLogger.logger(), debugSuccess :Boolean = false): Mono<Status> = this.map {
    if(it.failed){
        it.errorMessage
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warning("error message : $w") }
        it.extraData
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warning("extra data    : $w") }
    }else{
        if(debugSuccess)
            loggger.debug(message)
        else
            loggger.info(message)
    }
    it
}

fun Flux<Status>.printStatus(message :String = "", loggger : GLogger = GLogger.logger()): Flux<Status> = this.map {
    Mono.just(it).printStatus(message,loggger)
    it
}


inline fun <reified T> Mono<String>.mapFromJson(): Mono<T> = map { it.trimAESPadding() }
    .map { deserialize<T>(it) }

fun RSocket.requestStream(data: RequestDataBlob, keys :Person) : Flux<String> =
    Mono.just(data)
        .map { serialize(it) }
        .map { Person.encryptAES(it.toByteArray(Charset.forName("UTF-8")), server) }
        .map { encrypted : EncryptedBytes -> Message(
            clientID = data.clientID,
            encryptedData = encrypted.toStrings(),
            signature = keys.sign(encrypted.concat()).toBase64()
        ) }
        .map { message: Message -> serialize(message) }
        .map { network.info("Making request: $it");it }
        .map { it.toByteArray(Charset.forName("UTF-8")) }
        .map { DefaultPayload.create(it) }
        .flatMapMany { requestStream(it) }
        .map { it.dataUtf8 }
        .map { deserialize<EncryptedString>(it) }
        .map { it.toBytes() }
        .map { keys.decryptAESAndTestDefaultKey(it) }
        .onErrorMap { Exceptions.addSuppressed(IllegalAccessException("Can not decrypt data from the server - unauthorized?"), it) }
        .map { it.toString(Charset.forName("UTF-8")) }
        .map { network.info("Got Stream Response: $it");it }





fun RSocket.requestResponse(data :RequestDataBlob, keys :Person) : Mono<String> =
    Mono.just(data)
        .map { serialize(it) }
        .map { network.debug(it);it }
        .map { Person.encryptAES(it.toByteArray(Charset.forName("UTF-8")), server) }
        .map { encrypted : EncryptedBytes -> Message(
            clientID = data.clientID,
            encryptedData = encrypted.toStrings(),
            signature = keys.sign(encrypted.concat()).toBase64()
        ) }
        .map { message: Message -> serialize(message) }
        .map { network.info("Making request: $it");it }
        .map { it.toByteArray(Charset.forName("UTF-8")) }//make sure it's UTF-8, don't trust library
        .map { DefaultPayload.create(it) }
        .flatMap { requestResponse(it) }
        .map { it.dataUtf8 }
        .map { network.debug(it);it }
        .map { deserialize<EncryptedString>(it) }
        .map { it.toBytes() }
        .map { keys.decryptAESAndTestDefaultKey(it) }
        .onErrorMap { Exceptions.addSuppressed(IllegalAccessException("Can not decrypt data from the server - unauthorized?"), it) }
        .map { it.toString(Charset.forName("UTF-8")) }
        .map { network.info("Got Response: $it");it }


