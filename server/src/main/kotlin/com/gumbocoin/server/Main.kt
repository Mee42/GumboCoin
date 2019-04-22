package com.gumbocoin.server


import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import mu.KotlinLogging
import reactor.core.publisher.DirectProcessor
import systems.carson.base.*
import java.nio.charset.Charset

const val diff = 2L

private val logger by lazy { KotlinLogging.logger {} }

var blockchain :Blockchain =
    Blockchain(listOf(block(
        KeyManager.server.sign(block(Signature.VOID.toBase64()).hash.toByteArray(Charset.forName("UTF-8"))).toBase64()
    )))


private fun block(sig :String):Block{
    return Block(
        author = "server",
        actions = listOf(SignUpAction(clientID = "miner",publicKey = KeyManager.miner.publicKeyBase64())),
        timestamp = System.currentTimeMillis(),
        nonce = 0,
        difficulty = diff,
        lasthash = "null",
        signature = sig)
}

val dataCache :MutableList<Action> = mutableListOf()

fun addToDataCache(action :Action){
    logger.info("Logging action: ${serialize(action)}")
    dataCache.add(action)
    sendUpdates()
}
fun clearDataCache(){
    dataCache.clear()
    sendUpdates()
}
fun sendUpdates(){
    updateSource
        .onNext(ActionUpdate(
            actions = dataCache,
            difficulty = diff,
            lasthash = blockchain.blocks.last().hash
        ))
}

val updateSource: DirectProcessor<ActionUpdate> = DirectProcessor.create<ActionUpdate>()


fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("0.0.0.0", PORT))
        .start()!!

    logger.info("Network initialized")
    logger.info("blockchain:" + serialize(blockchain))

    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()
}