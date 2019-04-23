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

class MutableBlock(
    var author: String,
    var actions: List<Action>,
    var timestamp: Long,
    var nonce: Long,
    var difficulty: Long,
    var lasthash: String,
    var signature: String
){
    fun toBlock():Block{
        return Block(
            author = author,
            actions = actions,
            timestamp = timestamp,
            nonce = nonce,
            difficulty = difficulty,
            lasthash = lasthash,
            signature = signature)
    }
    constructor(b :Block): this(
        author = b.author,
        actions = b.actions,
        timestamp = b.timestamp,
        nonce = b.nonce,
        difficulty = b.difficulty,
        lasthash = b.lasthash,
        signature = b.signature)
    fun hash() = this.toBlock().hash()
}

inline fun <reified T> Mono<String>.mapFromJson():Mono<T> = map { it.trimAESPadding() }
        .map {
//    println("value: $it")
    gson.fromJson(it,T::class.java)
}

fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

    val socket = time("RSocket created") {
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", PORT))
            .start()
            .block()!!
    }

    time("Ping") { socket.requestResponse(RequestDataBlob(Request.Response.PING, clientID)).block() }


    time("To register") {
        socket.requestResponse(SignUpDataBlob(clientID,me.publicKeyBase64()))
            .mapFromJson<Status>()
            .printStatus("Registered successfully")
            .block()
    }
    var updates = Optional.empty<ActionUpdate>()
    thread(start = true) {
        Thread.sleep(100)
        socket.requestStream(RequestDataBlob(Request.Stream.BLOCKCHAIN_UPDATES,clientID))
            .println()
            .map { Sendable.fromJson<ActionUpdate>(it) }
            .subscribe {
                updates = Optional.of(it)
            println(it.lasthash)
            }
    }

    var block :MutableBlock? = null

    while(!updates.isPresent);

    while(true){
        if(updates.isPresent){
            val u = updates.get()
            block = MutableBlock(Block(
                author = clientID,
                actions = u.actions,
                timestamp = System.currentTimeMillis(),
                nonce = Random().nextLong(),
                difficulty = u.difficulty,
                lasthash = u.lasthash,
                signature = Signature.VOID.toBase64()
            ))
        }
        if(block == null){
            println("block in null...")
            continue
        }
        val hash = block.hash()
        if(hash.isValid()){
            block.signature = me.sign(block.toBlock().excludeSignature().toByteArray(Charset.forName("UTF-8"))).toBase64()
            socket.requestResponse(BlockDataBlob(clientID,block.toBlock(),Request.Response.BLOCK.intent))
                .mapFromJson<Status>()
                .printStatus("Block accepted successfully!")
                .block()
        }else{
            block.nonce++
        }
    }
}

