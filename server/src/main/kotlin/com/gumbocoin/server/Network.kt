package com.gumbocoin.server

import io.rsocket.*
import io.rsocket.util.DefaultPayload
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuple3
import reactor.util.function.Tuple4
import reactor.util.function.Tuples
import systems.carson.base.*
import java.nio.charset.Charset


//fun toPayload(string :String):Payload = DefaultPayload.create(string)
fun String.toPayload():Payload = DefaultPayload.create(this)


fun isValid(message: Message):Boolean{
    val people = blockchain.users.map { it.id to it.person }.toMap()
    if(!people.containsKey(message.clientID))
        return false
    val data = message.encryptedData.toBytes().concat()
    val sig = Signature.fromBase64(message.signature)
    val person = people.getOrElse(message.clientID) { error("fuk") }
    return Person.verify(person,sig,data)
}


operator fun <T1,T2> Tuple2<T1,T2>.component1():T1{
    return t1
}
operator fun <T1,T2> Tuple2<T1,T2>.component2():T2{
    return t2
}
operator fun <T1,T2,T3> Tuple3<T1,T2,T3>.component3():T3{
    return t3
}
operator fun <T1,T2,T3,T4> Tuple4<T1, T2, T3, T4>.component4():T4{
    return t4
}

val networkLogger = KotlinLogging.logger {}

private fun Mono<Tuple2<Payload,String>>.encryptBackToPerson():Mono<Payload> =
    map { (payload, clientID) ->
        Tuples.of(if(payload.hasMetadata()) payload.metadataUtf8 else "",payload.data.array(),clientID) }

        .map { tuple ->
            networkLogger.debug("data:" + tuple.t2.toString(Charset.forName("UTF-8")));tuple }

        .map { (meta, data,clientID) ->
            Tuples.of(meta,Person.encryptAES(data,blockchain.users.map { it.id to it.person }.toMap().getOrDefault(clientID,Person.default))) }

        .map { (meta,encrypted) ->
            Tuples.of(meta,gson.toJson(encrypted.toStrings())) }

        .map { tuple -> networkLogger.debug("encrypted:$tuple");tuple }

        .map { tuple -> networkLogger.debug(tuple.t2);tuple }

        .map { (meta, data) ->
            if(meta == "")
                DefaultPayload.create(data)
            else
                DefaultPayload.create(data,Charset.forName("UTF-8"),meta,Charset.forName("UTF-8"))
        }


private fun Flux<Tuple2<Payload,String>>.encryptBackToPerson():Flux<Payload> = flatMap { Mono.just(it).encryptBackToPerson() }//I hate myself but how

class MasterHandler :SocketAcceptor {
    override fun accept(setup: ConnectionSetupPayload?, sendingSocket: RSocket?): Mono<RSocket> {
        return Mono.just(object :AbstractRSocket(){
            override fun requestResponse(payload: Payload?): Mono<Payload> {
                return Mono.just(payload!!)//TODO make null safe
                    .map { it!! }
                    .map { gson.fromJson(it.dataUtf8,Message::class.java) }
                    .map { message -> Tuples.of(message.clientID,KeyManager.server.decryptAES(message.encryptedData.toBytes()),isValid(message)) }
                    .map { (clientID, plaintextBlob, isValid) ->  Tuples.of(clientID,plaintextBlob.toString(Charset.forName("UTF-8")),isValid) }
                    .map { (clientID, blob, isValid) -> Tuples.of(clientID,gson.fromJson(blob,DataBlob::class.java),isValid) }
                    .map { (clientID, blob, isValid) -> Tuples.of(RequestDataBlob(
                        clientID = clientID,
                        intent = Request.Response.values().first { value -> value.intent == blob.intent },// TODO("firstOrNull") ?: error("Can't find handler for intent ${blob.intent}"),
                        data = ReceivedData(blob.data),
                        isVerified = isValid
                    ),clientID) }
                    .map { networkLogger.info("request   :" + gson.toJson(it.t1));it }
                    .map { (data, id) -> Tuples.of(getResponseHandler(data).invoke(data),id) }
                    .encryptBackToPerson()
                    .map { itt -> networkLogger.debug("Sending back:${itt.dataUtf8}");itt}

            }

            override fun requestStream(payload: Payload?): Flux<Payload> {
                return Mono.just(payload!!)
                    .map { it!! }
                    .map { pay :Payload -> gson.fromJson(pay.dataUtf8,Message::class.java) }
                    .map { message :Message -> Tuples.of(message.clientID,KeyManager.server.decryptAES(message.encryptedData.toBytes()),isValid(message)) }
                    .map { (clientID,plaintextBlob,isValid) -> Tuples.of(clientID,plaintextBlob.toString(Charset.forName("UTF-8")),isValid) }
                    .map { (clientID, blob,isValid)-> Tuples.of(clientID,gson.fromJson(blob,DataBlob::class.java),isValid) }
                    .map { (clientID, blob, isValid)  -> Tuples.of(StreamDataBlob(
                        clientID = clientID,
                        intent = Request.Stream.values().first { value -> value.intent == blob.intent },// TODO("firstOrNull") ?: error("Can't find handler for intent ${blob.intent}"),
                        data = ReceivedData(blob.data),
                        isVerified = isValid
                    ),clientID) }
                    .map { networkLogger.info("stream    :" + gson.toJson(it.t1));it }
                    .flatMapMany { (blob: StreamDataBlob, clientID :String) -> getStreamHandler(blob).invoke(blob).map { Tuples.of(it,clientID) } }
                    .encryptBackToPerson()
            }
        })
    }
}


