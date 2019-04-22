package com.gumbocoin.cli

import io.rsocket.RSocket
import io.rsocket.util.DefaultPayload
import mu.KLogger
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.base.*
import java.nio.charset.Charset


fun Mono<Status>.printStatus(message :String = "", loggger : KLogger = logger): Mono<Status> = this.map {
    if(it.failed){
        message
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn(w) }
        it.errorMessage
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn("error message : $w") }
        it.extraData
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn("extra data    : $w") }
    }
    it
}

fun Flux<Status>.printStatus(message :String = "", loggger : KLogger = logger): Flux<Status> = this.map {
    if(it.failed){
        message
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn(w) }
        it.errorMessage
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn("error message : $w") }
        it.extraData
            .takeIf { w -> w.isNotBlank() }
            ?.let { w -> loggger.warn("extra data    : $w") }
    }
    it
}
fun String.trimAESPadding():String{
    var i = this.lastIndex
    while(i - 1 < length && this[i - 1] == (0).toChar())
        i--
    return this.substring(0,i)
}



inline fun <reified T:Sendable> Flux<ByteArray>.mapFromSendable():Flux<T> = map { it.toString(Charset.forName("UTF-8")) }.map { Sendable.deserialize<T>(it) }
inline fun <reified T:Sendable> Mono<ByteArray>.mapFromSendable():Mono<T> = map { it.toString(Charset.forName("UTF-8")) }.map { Sendable.deserialize<T>(it) }


fun Flux<ByteArray>.stringValues() : Flux<String> = map { it.toString(Charset.forName("UTF-8")) }
fun Mono<ByteArray>.stringValues() : Mono<String> = map { it.toString(Charset.forName("UTF-8")) }


fun Flux<String>.println() : Flux<String> = map { response.info(it);it }
fun Mono<String>.println() : Mono<String> = map { response.info(it);it }


fun RSocket.requestStream(req : Request.Stream, data: Sendable) : Flux<ByteArray> =
    Mono.fromCallable { DataBlob(req.intent,data.send()) }
        .map { gson.toJson(it) }
        .map { Person.encryptAES(it.toByteArray(Charset.forName("UTF-8")), server) }
        .map { encrypted : EncryptedBytes -> Message(
            clientID = clientID,
            encryptedData = encrypted.toStrings(),
            signature = me.sign(encrypted.concat()).toBase64()
        ) }
        .map { message: Message -> gson.toJson(message) }
        .map { it.toByteArray(Charset.forName("UTF-8")) }
        .map { DefaultPayload.create(it) }
        .flatMapMany { requestStream(it) }
        .map { it.dataUtf8 }
        .map { gson.fromJson(it, EncryptedString::class.java) }
        .map { it.toBytes() }
        .map { me.decryptAESAndTestDefaultKey(it) }
        .onErrorMap { Exceptions.addSuppressed(IllegalAccessException("Can not decrypt data from the server - unauthorized?"), it) }




fun RSocket.requestResponse(req : Request.Response, data : Sendable) : Mono<ByteArray> =
    Mono.fromCallable { DataBlob(req.intent, data.send()) }
        .map { gson.toJson(it) }
        .map { logger.info(it);it }
        .map { Person.encryptAES(it.toByteArray(Charset.forName("UTF-8")), server) }
        .map { encrypted : EncryptedBytes -> Message(
            clientID = clientID,
            encryptedData = encrypted.toStrings(),
            signature = me.sign(encrypted.concat()).toBase64()
        ) }
        .map { message: Message -> gson.toJson(message) }
        .map { logger.debug("Making request: $it");it }
        .map { it.toByteArray(Charset.forName("UTF-8")) }
        .map { DefaultPayload.create(it) }
        .flatMap { requestResponse(it) }
        .map { it.dataUtf8 }
        .map { logger.debug(it);it }
        .map { gson.fromJson(it, EncryptedString::class.java) }
        .map { it.toBytes() }
        .map { me.decryptAESAndTestDefaultKey(it) }
        .onErrorMap { Exceptions.addSuppressed(IllegalAccessException("Can not decrypt data from the server - unauthorized?"), it) }

