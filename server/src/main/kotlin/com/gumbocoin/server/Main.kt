package com.gumbocoin.server


import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.core.publisher.DirectProcessor
import systems.carson.base.*
import java.nio.charset.Charset
import kotlin.concurrent.thread

const val diff = 4L

var blockchain: Blockchain =
    Blockchain(
        listOf(
            block(
                KeyManager.server.sign(block(Signature.VOID.toBase64()).hash.toByteArray(Charset.forName("UTF-8"))).toBase64()
            )
        )
    )

val logger = GLogger.logger()

fun block(sig: String): Block {
    return Block(
        author = "server",
        actions = listOf(),
        timestamp = System.currentTimeMillis(),
        nonce = 0,
        difficulty = diff,
        lasthash = "null",
        signature = sig
    )
}

val dataCache: MutableList<Action> = mutableListOf()

fun addToDataCache(action: Action) {
    logger.info("Logging action: ${serialize(action)}")
    dataCache.add(action)
    sendUpdates()
}

fun clearDataCache() {
    dataCache.removeAll { true }

    sendUpdates()
}

fun sendUpdates() {

    logger.info("Sending update. lastHash: ${blockchain.blocks.last().hash}")
    updateSource
        .onNext(
            ActionUpdate(
                actions = dataCache,
                difficulty = diff,
                lasthash = blockchain.blocks.last().hash
            )
        )

}

val updateSource: DirectProcessor<ActionUpdate> = DirectProcessor.create<ActionUpdate>()


fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN")

    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("0.0.0.0", PORT))
        .start()!!

    logger.info("Network initialized")
    logger.info("blockchain:" + serialize(blockchain))

    val https = startHttps()

    logger.info("Https setup")

    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()

    logger.info("Killing https")
    https.kill()
    logger.info("Done")
}