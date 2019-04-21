package com.gumbocoin.server

import io.rsocket.*
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuple3
import reactor.util.function.Tuples
import systems.carson.base.*
import java.nio.charset.Charset


//fun toPayload(string :String):Payload = DefaultPayload.create(string)
fun String.toPayload():Payload = DefaultPayload.create(this)

val people = mutableMapOf<String,Person>()

fun isValid(message: Message):Boolean{
    if(!people.containsKey(message.clientID))
        return false
    val data = message.encryptedData.toBytes().concat()
    val sig = Signature.fromBase64(message.signature)
    return Person.verify(people[message.clientID]!!,sig,data)
}

class MasterHandler :SocketAcceptor {
    override fun accept(setup: ConnectionSetupPayload?, sendingSocket: RSocket?): Mono<RSocket> {
        return Mono.just(object :AbstractRSocket(){
            override fun requestResponse(payload: Payload?): Mono<Payload> {
                return Mono.justOrEmpty(payload)
                    .map { it!! }
                    .map { gson.fromJson(it.dataUtf8,Message::class.java) }
                    .map { message :Message -> Tuples.of(message,isValid(message)) }
                    .map { tuple :Tuple2<Message,Boolean> -> Tuples.of(tuple.t1.clientID,server.decryptAES(tuple.t1.encryptedData.toBytes()),tuple.t2) }
                    .map { tuple: Tuple3<String, ByteArray, Boolean> ->  Tuples.of(tuple.t1,tuple.t2.toString(Charset.forName("UTF-8")),tuple.t3) }
                    .map { println("request   :id=${it.t1}, verified=${it.t3} blob=${it.t2}");it }
                    .map { tuple: Tuple3<String, String, Boolean> -> Tuples.of(tuple.t1,gson.fromJson(tuple.t2,DataBlob::class.java),tuple.t3) }
                    .map { tuple :Tuple3<String,DataBlob,Boolean> -> RequestDataBlob(
                        clientID = tuple.t1,
                        intent = Request.Response.values().first { value -> value.intent == tuple.t2.intent },
                        data = ReceivedData(tuple.t2.data),
                        isVerified = tuple.t3
                    ) }
                    .map { getResponseHandler(it).invoke(it) }
            }

            override fun requestStream(payload: Payload?): Flux<Payload> {
                return Mono.justOrEmpty(payload)
                    .map { it!! }
                    .doOnNext { println("stream    :" + it.dataUtf8) }
                    .map { gson.fromJson(it.dataUtf8,Message::class.java) }
                    .map { Tuples.of(it.clientID,server.decryptAES(it.encryptedData.toBytes())) }
                    .map { Tuples.of(it.t1,it.t2.toString(Charset.forName("UTF-8")))}
                    .map { Tuples.of(it.t1,gson.fromJson(it.t2,DataBlob::class.java))}
                    .map { tuple  -> StreamDataBlob(
                        clientID = tuple.t1,
                        intent = Request.Stream.values().first { value -> value.intent == tuple.t2.intent },
                        data = ReceivedData(tuple.t2.data),
                        isVerified = false//TODO
                    ) }
                    .flatMapMany { getStreamHandler(it).invoke(it) }
            }
        })
    }
}

private fun Flux<String>.toPayloads() :Flux<Payload> = this.map { it.toPayload() }

fun getStreamHandler(payload :StreamDataBlob) :(StreamDataBlob) -> Flux<Payload> = when(payload.intent){
    Request.Stream.NUMBERS -> { pay ->
        Mono.fromCallable { pay.data.fromJson<SendableInteger>().value }
            .filter { it != null }
            .map { it!! }
            .flatMapMany { count -> Flux.range(0,count) }
            .map { it.toString() }
            .toPayloads()
    }
}

fun getResponseHandler(payloadInital :RequestDataBlob):(RequestDataBlob) -> Payload {
    return when (payloadInital.intent) {
        Request.Response.PING -> { _ -> "pong".toPayload() }
        Request.Response.DECRYPT -> { pay ->
            val plain = server.decryptAES(pay.data.fromJson<EncryptedString>().toBytes())
            "cli:\"${plain.toString(Charset.forName("UTF-8"))}\"".toPayload()
        }

    }
}

