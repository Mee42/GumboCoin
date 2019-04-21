package com.gumbocoin.server

import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import systems.carson.base.PORT
import systems.carson.base.ServerKey

private const val password = "6Lu+ji1ljuucZBctWAeJX3ld0kceqCSai6PlBwWxz"
val server = ServerKey.person(password)

fun main() {
    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("0.0.0.0", PORT))
        .start()!!
    println("Network initialized")
    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()
}