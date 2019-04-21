package com.gumbocoin.server


import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import mu.KotlinLogging
import systems.carson.base.PORT


private val logger by lazy { KotlinLogging.logger {} }


fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("0.0.0.0", PORT))
        .start()!!
    logger.info("Network initialized")
    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()
}