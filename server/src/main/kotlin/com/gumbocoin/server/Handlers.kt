package com.gumbocoin.server

import io.rsocket.Payload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import systems.carson.base.*
import java.nio.charset.Charset


val handlerLogger = GLogger.logger("Handler")
enum class StreamHandler(
    val request :Request.Stream,
    val handler :(RequestDataBlob) -> Flux<Payload>){
    NUMBERS(Request.Stream.NUMBERS,req@{pay ->
        pay as IntDataBlob
        return@req Flux.range(0,pay.value).map { "" + it }.map { it.toPayload() }
    }),
    BLOCKCHAIN_UPDATES(Request.Stream.BLOCKCHAIN_UPDATES, req@ {
//        println("dataCache: ${serialize(dataCache)}")
        val value = ActionUpdate(dataCache, blockchain.blocks.last().hash,diff)
//        println("Value: ${serialize(value)}")
        updateSource.toFlux().startWith(value)
            .map { println("Sending out ${serialize(it)}");it }
            .map { it.toPayload() }
//        Flux.just(value)
//            .map { println("Sending out ${serialize(it)}");it }
//            .map { it.toPayload() }
    })
}

enum class ResponseHandler(
    val request :Request.Response,
    val handler :(RequestDataBlob) -> Payload){
    PING(Request.Response.PING,req@ { "pong".toPayload() }),
    DECRYPT(Request.Response.DECRYPT,req@ {pay ->
        pay as EncryptedDataBlob
        val plain = KeyManager.server.decryptAES(pay.data.toBytes())
        "${pay.clientID}:\"${plain.toString(Charset.forName("UTF-8"))}\"".toPayload()
    }),
    SIGN_UP(Request.Response.SIGN_UP,req@ {pay ->
        pay as SignUpDataBlob

        if (blockchain.users.any { it.id == pay.clientID })
            return@req Status(failed = true, errorMessage = "User already exists").toPayload()
        addToDataCache(pay.signUpAction)
        Status().toPayload()
    }),
    VERIFIED(Request.Response.VERIFIED,req@ {pay ->
        return@req SendableBoolean(pay.isVerified).toPayload()
    }),
    BLOCK(Request.Response.BLOCK, req@ { pay ->
        pay as BlockDataBlob
        val block = pay.block
        handlerLogger.debug("got block:$block")
        if(!block.hash.isValid())
            return@req Status(failed = true, errorMessage = "Block hash is wrong",extraData = "Hash: ${block.hash}").toPayload()
        val newBlockchain = blockchain.newBlock(block)
        val valid = newBlockchain.isValid()
        if(valid.isPresent) {
            return@req Status(
                failed = true,
                errorMessage = "Block is invalid when added to the blockchain",
                extraData = valid.get()
            ).toPayload()
        }
        TODO("check data cache and make sure it matches")

        blockchain = newBlockchain
        clearDataCache()
        Status().toPayload()
    }),
    BLOCKCHAIN(Request.Response.BLOCKCHAIN, { blockchain.toPayload() }),
    TRANSACTION(Request.Response.TRANSACTION, req@ {pay ->
        if(!pay.isVerified)
            return@req Status(failed = true,errorMessage =  "Unverified RequestDataBlob").toPayload()

        pay as TransactionDataBlob

        if (!blockchain.users.any { it.id == pay.clientID })
            return@req Status(failed = true, errorMessage = "User does not exists").toPayload()
        val user = blockchain.users.first { it.id == pay.clientID }
        if(!pay.transactionAction.isSignatureValid(user.person.publicKey))
            return@req Status(failed = true, errorMessage = "Signature on transactionAction is not valid").toPayload()
        addToDataCache(pay.transactionAction)
        Status().toPayload()
    }),
    MONEY(Request.Response.MONEY, { pay ->
        pay as StringDataBlob
        SendableInt(blockchain.amounts[pay.value] ?: 0).toPayload()
    })
}