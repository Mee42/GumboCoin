package com.gumbocoin.server

import io.rsocket.*
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuple3
import reactor.util.function.Tuple4
import reactor.util.function.Tuples
import systems.carson.base.*
import java.nio.charset.Charset


//fun toPayload(string :String):Payload = DefaultPayload.create(string)
fun String.toPayload(): Payload = DefaultPayload.create(this)


fun isValid(message: Message): Boolean {
    val people = blockchain.users.map { it.id to it.person }.toMap() +
            dataCache.filter { it.type == ActionType.SIGN_UP }
                .map { it as SignUpAction }
                .map { it.clientID to Person.fromPublicKey(it.publicKey) }
                .toMap()
    if (!people.containsKey(message.clientID))
        return false
    val data = message.encryptedData.toBytes().concat()
    val sig = Signature.fromBase64(message.signature)
    val person = people.getOrElse(message.clientID) { error("fuk") }
    return Person.verify(person, sig, data)
}


private val networkLogger = GLogger.logger("Network")

private fun Mono<Tuple2<String, Payload>>.encryptBackToPerson(): Mono<Payload> =
    map { (clientID, payload) ->
        Tuples.of(if (payload.hasMetadata()) payload.metadataUtf8 else "", payload.data.array(), clientID)
    }

        .map { tuple ->
            networkLogger.debug("data:" + tuple.t2.toString(Charset.forName("UTF-8")));tuple
        }

        .map { (meta, data, clientID) ->
            Tuples.of(
                meta,
                Person.encryptAES(
                    data,
                    blockchain.users.map { it.id to it.person }.toMap().getOrDefault(clientID, Person.default)
                )
            )
        }

        .map { (meta, encrypted) ->
            Tuples.of(meta, serialize(encrypted.toStrings()))
        }

        .map { tuple -> networkLogger.debug("encrypted:$tuple");tuple }

        .map { tuple -> networkLogger.debug(tuple.t2);tuple }

        .map { (meta, data) ->
            if (meta == "")
                DefaultPayload.create(data)
            else
                DefaultPayload.create(data, Charset.forName("UTF-8"), meta, Charset.forName("UTF-8"))
        }

private fun Flux<Tuple2<String, Payload>>.encryptBackToPerson(): Flux<Payload> =
    map { tuple -> Mono.just(tuple) }
        .flatMap { it.encryptBackToPerson() }
//            .flatMap/ { it }

fun getResponseHandler(requestDataBlob: RequestDataBlob): (RequestDataBlob) -> Payload {
    return ResponseHandler.values()
        .firstOrNull {
//            println("it.request.intent == requestDataBlob.intent | " +
//                    "${it.request.intent} == ${requestDataBlob.intent} | " +
//                    (it.request.intent == requestDataBlob.intent) )
            it.request.intent == requestDataBlob.intent
        }
        ?.handler ?: error("Can't handle ${requestDataBlob.intent}")
}

fun getStreamHandler(requestDataBlob: RequestDataBlob): (RequestDataBlob) -> Flux<Payload> {
    return StreamHandler.values()
        .firstOrNull { it.request == Request.Stream.values().first { w -> w.intent == requestDataBlob.intent } }
        ?.handler ?: error("Can't handle $requestDataBlob")
}


class MasterHandler : SocketAcceptor {
    override fun accept(setup: ConnectionSetupPayload?, sendingSocket: RSocket?): Mono<RSocket> {
        return Mono.just(object : AbstractRSocket() {
            override fun requestResponse(payload: Payload): Mono<Payload> {
                return Mono.just(payload)
                    .map { deserialize<Message>(it.dataUtf8) }
                    .map { message ->
                        Tuples.of(
                            message.clientID,
                            KeyManager.server.decryptAES(message.encryptedData.toBytes()),
                            isValid(message)
                        )
                    }
                    .map { (clientID, plaintextBlob, isValid) ->
                        Tuples.of(
                            clientID,
                            plaintextBlob.toString(Charset.forName("UTF-8")),
                            isValid
                        )
                    }
                    .map { (clientID, blob, isValid) ->
                        Tuples.of(
                            clientID,
                            deserialize<RequestDataBlob>(blob),
                            isValid
                        )
                    }
                    .map { tuple -> tuple.t2.isVerified = tuple.t3;tuple }
                    .map { (clientID, blob) -> Tuples.of(clientID, getResponseHandler(blob).invoke(blob)) }
                    .map { itt -> networkLogger.info("Sending back:${itt.t2.dataUtf8}");itt }
                    .encryptBackToPerson()

            }

            override fun requestStream(payload: Payload): Flux<Payload> {
                return Mono.just(payload)
                    .map { pay: Payload -> deserialize<Message>(pay.dataUtf8) }
                    .map { message: Message ->
                        Tuples.of(
                            message.clientID,
                            KeyManager.server.decryptAES(message.encryptedData.toBytes()),
                            isValid(message)
                        )
                    }
                    .map { (clientID, plaintextBlob, isValid) ->
                        Tuples.of(
                            clientID,
                            plaintextBlob.toString(Charset.forName("UTF-8")),
                            isValid
                        )
                    }
                    .map { networkLogger.info("stream    :" + it.t2);it }
                    .map { (clientID, blob, isValid) ->
                        Tuples.of(
                            clientID,
                            deserialize<RequestDataBlob>(blob),
                            isValid
                        )
                    }
                    .map { it.t2.isVerified = it.t3;Tuples.of(it.t1, it.t2) }
                    .flatMapMany { (clientID, blob) ->
                        getStreamHandler(blob).invoke(blob).map { Tuples.of(clientID, it) }
                    }
                    .map { itt -> itt }
                    .encryptBackToPerson()
            }
        })
    }
}


