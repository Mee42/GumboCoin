package com.gumbocoin.server

import io.rsocket.Payload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import systems.carson.base.*
import java.nio.charset.Charset

private fun Flux<String>.toPayloads(): Flux<Payload> = this.map { it.toPayload() }


val streamsThatDontNeedVerification =
    listOf(
        Request.Stream.NUMBERS,
        Request.Stream.BLOCKCHAIN_UPDATES
    )
//TODO implement this in Network.kt
val requestsThatDontNeedVerification =
        listOf(
            Request.Response.PING,
            Request.Response.DECRYPT,
            Request.Response.VERIFIED,
            Request.Response.SIGN_UP
            )

fun getStreamHandler(payloadInitial: StreamDataBlob): (StreamDataBlob) -> Flux<Payload> =
    when (payloadInitial.intent) {
        Request.Stream.NUMBERS -> { pay ->
            Mono.fromCallable { pay.data.fromJson<SendableInteger>().value }
                .filter { it != null }
                .map { it!! }
                .flatMapMany { count -> Flux.range(0, count) }
                .map { it.toString() }
                .toPayloads()
        }
        Request.Stream.BLOCKCHAIN_UPDATES -> { _ ->
            val value = ActionUpdate(dataCache, blockchain.blocks.last().hash(),diff)
            updateSource.toFlux().startWith(value).map(ActionUpdate::toPayload)
        }

    }

fun getResponseHandler(payloadInitial: RequestDataBlob): (RequestDataBlob) -> Payload =
    when (payloadInitial.intent) {

        Request.Response.PING -> { _ -> "pong".toPayload() }
        Request.Response.DECRYPT -> { pay ->
            val plain = KeyManager.server.decryptAES(pay.data.fromJson<EncryptedString>().toBytes())
            "${pay.clientID}:\"${plain.toString(Charset.forName("UTF-8"))}\"".toPayload()
        }
        Request.Response.VERIFIED -> { pay -> SendableBoolean(pay.isVerified).send().toPayload() }
        Request.Response.SIGN_UP -> req@{ pay ->
            val user = Sendable.deserialize<SignUpData>(pay.data.fromJson<SignUpAction>().data)
            if (blockchain.users.any { it.id == user.clientID })
                return@req Status(failed = true, errorMessage = "User already exists").toPayload()
            addToDataCache(user)
            return@req Status().toPayload()
        }
        Request.Response.BLOCK -> req@ { pay ->
            if(pay.isVerified)
                return@req Status(failed = true, errorMessage = "Not verified").toPayload()//TODO move into Network.kt
            val block = pay.data.fromJson<Block>()
            println("got block:$block")
            if(!block.hash().isValid())
                return@req Status(failed = true, errorMessage = "Block hash is wrong",extraData = "Hash: ${block.hash()}").toPayload()
            val newBlockchain = blockchain.newBlock(block)
            val valid = newBlockchain.isValid()
            if(valid.isPresent) {
                return@req Status(
                    failed = true,
                    errorMessage = "Block is invalid when added to the blockchain",
                    extraData = valid.get()
                ).toPayload()
            }
            blockchain = newBlockchain
            clearDataCache()
            Status().toPayload()
        }

    }
