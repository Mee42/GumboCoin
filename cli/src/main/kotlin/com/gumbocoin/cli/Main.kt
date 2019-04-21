package com.gumbocoin.cli

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import systems.carson.base.*
import java.nio.charset.Charset
import java.time.Duration


private class Timer{
    private var start :Long = -1
    private var end :Long = -1
    fun start(){ start = System.nanoTime() }
    fun end(){ end = System.nanoTime() }
    fun elapsed():Duration { return Duration.ofNanos(end - start) }
    override fun toString() :String {
        val t = elapsed()
        val hours = t.toHoursPart()
        val minutes = t.toMinutesPart()
        val seconds = t.toSecondsPart()
        val ms = t.toMillisPart()
        var s = ""
        if(hours != 0) s+= "" + hours + "h "
        if(minutes != 0) s+= "" + minutes + "m "
        if(seconds != 0) s+= "" + seconds + "s "
        if(ms != 0) s+= "" + ms + "ms "
        if(s == "")
            s = "No time elapsed"
        return s.trim()
    }

}

val server = Person.fromPublicKey(ServerKey.publicKey())


fun main() {
    val socket = time("RSocket created") {
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", PORT))
            .start()
            .block()!!
    }
    time("Ping test 1"){
        socket.requestResponse(Request.Response.PING,NoData)
            .stringValues()
            .println()
            .block()
    }

    println()

    time("All AES tests") {
        for (i in 0 until 10) {
            time("AES test #$i") {
                val data = "Hello, World: $i".toByteArray(Charset.forName("UTF-8"))
                socket.requestResponse(Request.Response.DECRYPT, Person.encryptAES(data, server).toStrings())
                    .stringValues()
//                    .println()
                    .block()
            }
        }
    }

    println()

    time("Ping test 1"){
        socket.requestResponse(Request.Response.PING,NoData)
            .stringValues()
            .println()
            .block()
    }




}

private fun <T> time(print :String, closure :() -> T):T{
    val timer = Timer()
    timer.start()
    val t = closure()
    timer.end()
    println("$print: $timer")
    return t
}


private fun Flux<ByteArray>.stringValues() :Flux<String> = map { it.toString(Charset.forName("UTF-8")) }
private fun Mono<ByteArray>.stringValues() :Mono<String> = map { it.toString(Charset.forName("UTF-8")) }

private fun Flux<String>.println() :Flux<String> = map { println(it);it }
private fun Mono<String>.println() :Mono<String> = map { println(it);it }


fun RSocket.requestStream(req :Request.Stream, data: Sendable) : Flux<ByteArray> = Mono.fromCallable {
    DataBlob(
        intent = req.intent,
        data = data.send()
//        clientID = "cli"
    )
}.thenMany { TODO();arrayOf<Byte>().toByteArray() }
//    .map { gson.toJson(it) }
//    .map { it.toByteArray(Charset.forName("UTF-8")) }
//    .map { Person.encryptAES(it,server) }
//    .map { it :ByteArray -> DefaultPayload.create(it) }
//    .flatMapMany { requestStream(it) }
//    .map { it.data.array() }

val me = Person.generateNew()

fun RSocket.requestResponse(req : Request.Response, data :Sendable) :Mono<ByteArray> =
    Mono.fromCallable { DataBlob(req.intent, data.send()) }
        .map { gson.toJson(it) }
        .map { Person.encryptAES(it.toByteArray(Charset.forName("UTF-8")),server) }
        .map { encrypted :EncryptedBytes -> Message(
            clientID = "cli",
            encryptedData = encrypted.toStrings(),
            signature = me.sign(encrypted.concat()).toBase64()
        ) }
        .map { message: Message -> gson.toJson(message) }
        .map { it.toByteArray(Charset.forName("UTF-8")) }
        .map { DefaultPayload.create(it) }
        .flatMap { requestResponse(it) }
        .map { it.data.array() }
