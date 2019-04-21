package com.gumbocoin.cli

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import mu.KLogger
import mu.KotlinLogging
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.base.*
import java.nio.charset.Charset
import java.util.*


val logger = KotlinLogging.logger {  }
val response = KotlinLogging.logger("Response")


val me = Person.generateNew()
val clientID = UUID.randomUUID().toString().split("-")[0]

fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO")

    val socket = time("RSocket created") {
        RSocketFactory.connect()
            .transport(TcpClientTransport.create("localhost", PORT))
            .start()
            .block()!!
    }
    time("Ping test 1"){
        socket.requestResponse(Request.Response.PING,NoData)
            .stringValues()
            .println()
            .block()
    }

    println()

    fun hello(i :Int) = "Hello, World: $i"
    fun me(i :Int) = "cli:\"${hello(i)}\""
    time("All AES tests") {
        for (i in 0 until 10) {
            time("AES test #$i") {
                val data = hello(i).toByteArray(Charset.forName("UTF-8"))
                socket.requestResponse(Request.Response.DECRYPT, Person.encryptAES(data, server).toStrings())
                    .stringValues()
                    .map { it.trimAESPadding() }
//                    .println()
                    .block()
                    .let {
                        if(it != me(i))
                            error("\n\"$it\"\n\'${me(i)}\"\n\n${it?.toByteArray()?.contentToString()}\n${me(i).toByteArray().contentToString()}")
                    }
            }
        }
    }

    println()


    time("Ping test 1"){
        socket.requestResponse(Request.Response.PING,NoData)
            .stringValues()
            .println()
            .block()
    }

    time("Stream test 0"){
        socket.requestStream(Request.Stream.NUMBERS,SendableInteger(25))
            .stringValues()
            .println()
            .blockLast()
    }

    println()

    time("check verification") {
        socket.requestResponse(Request.Response.VERIFIED,NoData)
            .stringValues()
            .map { Sendable.deserialize<SendableBoolean>(it) }
            .block()
            ?.let {
                if(it.value){
                    logger.info("Successfully verified")
                }else{
                    logger.warn("Unsuccessfully verified")
                }
            }
    }

    time("Submit public key"){
        socket.requestResponse(Request.Response.PUBLIC_KEY,SendableString(me.justPublic().serialize()))
            .stringValues()
            .map { Sendable.deserialize<Status>(it) }
            .printStatus("Attempted to submit public key")
            .block()
    }

    time("check verification") {
        socket.requestResponse(Request.Response.VERIFIED,NoData)
            .stringValues()
            .map { Sendable.deserialize<SendableBoolean>(it) }
            .block()
            ?.let {
                if(it.value){
                    logger.info("Successfully verified")
                }else{
                    logger.warn("Unsuccessfully verified")
                }
            }
    }

}