package com.gumbocoin.cli

import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.base.*
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread


val logger = KotlinLogging.logger { }
val response = KotlinLogging.logger("Response")

val me = Person.generateNew()
val clientID = UUID.randomUUID().toString().split("-")[0]

fun log(s :Status, success :String){
    if(s.failed){
        s.errorMessage.takeIf { it.isNotBlank() }
            ?.let { logger.warn(it) }
        s.extraData.takeIf { it.isNotBlank() }
            ?.let { logger.warn(it) }
    }else{
        logger.info(success)
    }
}
fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

    val socket = time("RSocket created") {
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", PORT))
            .start()
            .block()!!
    }

    time("Ping") { socket.requestResponse(Request.Response.PING, NoData).block() }


    time("To register") {
        socket.requestResponse(Request.Response.SIGN_UP, SignUpAction(clientID,me.publicKeyBase64()))
            .mapFromSendable<Status>()
            .map { log(it,"Registered successfully") }
            .block()
    }

    time("To mine a block") {
        var updates = Optional.empty<ActionUpdate>()
        thread(start = true) {
            Thread.sleep(100)
            socket.requestStream(Request.Stream.BLOCKCHAIN_UPDATES, NoData)
                .stringValues()
                .println()
                .map { Sendable.deserialize<ActionUpdate>(it) }
                .subscribe {
                    updates = Optional.of(it)
//            println(it.lasthash)
                }
        }

        var block: Block? = null

        w@ while (true) {
            if (updates.isPresent) {
                logger.info { "Updating block" }
                val u = updates.get()
                block = Block(
                    author = clientID,
                    actions = u.actions,//TODO plus transaction to myself
                    timestamp = System.currentTimeMillis(),
                    nonce = Random().nextLong(),
                    difficulty = u.difficulty,
                    lasthash = u.lasthash,
                    signature = ""
                )
                updates = Optional.empty()
            }
            if (block != null) {
                println("trying ${block.hash}")
                if (block.hash.isValid()) {
                    //reconstruct block with signature
                    val new = Block(
                        block.author,
                        block.actions,
                        block.timestamp,
                        block.nonce,
                        block.difficulty,
                        block.lasthash,
                        me.sign(block.hash.toByteArray(Charset.forName("UTF-8"))).toBase64()
                    )
                    logger.info("Sending: $new")
                    val status = socket.requestResponse(Request.Response.BLOCK, new)
                        .mapFromSendable<Status>()
                        .block()!!

                    log(status, "Block accepted successfully")
                    break@w
                } else {
                    block = Block(
                        block.author,//TODO better mutable block interface
                        block.actions,
                        block.timestamp,
                        block.nonce + 1,
                        block.difficulty,
                        block.lasthash,
                        block.signature
                    )
                }
            } else {
                println("block is null...")
            }
        }
    }
}

