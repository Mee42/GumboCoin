package com.gumbocoin.cli

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import systems.carson.base.*
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import java.util.ArrayList
import java.util.Collections.synchronizedList


val logger = GLogger.logger()

val network = GLogger.logger("Network")


val me = Person.generateNew()
val clientID = UUID.randomUUID().toString().split("-")[0]

class MutableBlock(
    var author: String,
    var actions: List<Action>,
    var timestamp: Long,
    var nonce: Long,
    var difficulty: Long,
    var lasthash: String,
    var signature: String
) {
    fun toBlock(): Block {
        return Block(
            author = author,
            actions = actions,
            timestamp = timestamp,
            nonce = nonce,
            difficulty = difficulty,
            lasthash = lasthash,
            signature = signature
        )
    }

    constructor(b: Block) : this(
        author = b.author,
        actions = b.actions,
        timestamp = b.timestamp,
        nonce = b.nonce,
        difficulty = b.difficulty,
        lasthash = b.lasthash,
        signature = b.signature
    )

    fun hash() = this.toBlock().hash()
}

inline fun <reified T> Mono<String>.mapFromJson(): Mono<T> = map { it.trimAESPadding() }
    .map { deserialize<T>(it) }

fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN")
    logger.printInfo()
    network.printWarnings()


    val socket = RSocketFactory.connect()
        .transport(TcpClientTransport.create("localhost", PORT))
        .start()
        .block()!!

    (0..2).forEach {
        time("Ping-$it") { socket.requestResponse(RequestDataBlob(Request.Response.PING, clientID)).block() }
    }

    socket.requestResponse(SignUpDataBlob(clientID, me.publicKeyBase64()))
        .mapFromJson<Status>()
        .printStatus("Registered successfully")
        .block()

    val threadedMiner = ThreadedMiner(socket,GLogger.logger("Miner"))

    socket.requestStream(RequestDataBlob(Request.Stream.BLOCKCHAIN_UPDATES, clientID))
        .map { Sendable.fromJson<ActionUpdate>(it) }
        .map {
            println("Updating: ${serialize(it.actions)}")
            threadedMiner.update(
                Update(
                    Block(
                        author = clientID,
                        actions = it.actions,
                        timestamp = System.currentTimeMillis(),
                        nonce = Random().nextLong(),
                        difficulty = it.difficulty,
                        lasthash = it.lasthash,
                        signature = ""
                    )
                )
            )
        }.subscribe()

    threadedMiner.start()

    threadedMiner
        .toFlux()
        .take(10)
        .index { index, tuple2 -> Tuples.of(index,tuple2.t1,tuple2.t2) }
        .map { println("Block!-${it.t1}") }
        .blockLast()

    threadedMiner.stop()

    socket.requestResponse(RequestDataBlob(
        clientID = clientID,
        intent = Request.Response.BLOCKCHAIN))
        .map { Sendable.fromJson<Blockchain>(it) }
        .map { println("blockchain:${prettyPrint(it)}") }
        .block()

    socket.requestResponse(StringDataBlob(clientID,clientID,Request.Response.MONEY.intent))
        .map { Sendable.fromJson<SendableInt>(it) }
        .map { println("Money:${it.value}") }
        .block()

    socket.requestResponse(TransactionDataBlob(
        clientID,
        TransactionAction.sign(
            clientID = clientID,
            recipientID = "server",
            amount = 5,
            person = me)
    ))
        .map { Sendable.fromJson<Status>(it) }
        .printStatus("Transaction successfull")
        .block()

    threadedMiner.start()

    threadedMiner
        .toFlux()
        .take(1)
        .index { index, tuple2 -> Tuples.of(index,tuple2.t1,tuple2.t2) }
        .map { println("Block!-${it.t1}") }
        .blockLast()

    threadedMiner.stop()

    socket.requestResponse(StringDataBlob(clientID,clientID,Request.Response.MONEY.intent))
        .map { Sendable.fromJson<SendableInt>(it) }
        .map { println("Money:${it.value}") }
        .block()

    threadedMiner.stop()



    threadedMiner.exit()
}

class Update(val newBlock: Block)
class ThreadedMiner(private val socket: RSocket,private val loggger : GLogger = GLogger.logger(), sleep: Long = 100) {


    private var running = false
    private val update = synchronizedList(mutableListOf<Update>())
    private var exit = false
    private var sleepLength = sleep

    fun start() {
        running = true
    }


    fun stop(){
        running = false
    }

    fun exit() {
        running = false; exit = true
    }

    fun update(u: Update) = update.add(u)

    private val hot = DirectProcessor.create<Tuple2<Block, Status>>()

    fun toFlux(): Flux<Tuple2<Block, Status>> {
        return hot.toFlux()
    }

    init {
        thread(start = true) {
            var block: MutableBlock? = null
            root@ while (true) {
                if (exit) {
//                    println("exiting")
                    break@root
                }
                if (!running) {
//                    println("not running :(")
                    Thread.sleep(sleepLength)
                    continue@root
                }
                if (update.isNotEmpty()) {
                    println("Updating block")
                    block = MutableBlock(update.removeAt(0).newBlock)
                }
                val bblock = if(block == null) { logger.debug("Block is null");continue@root }else{ block }
                val hash = bblock.hash()
                if (hash.isValid()) {
//                    println("Found valid hash: $hash")
                    bblock.signature =
                        me.sign(bblock.toBlock().excludeSignature().toByteArray(Charset.forName("UTF-8"))).toBase64()
                    val realBlock = bblock.toBlock()

                    socket.requestResponse(
                        BlockDataBlob(
                            block = realBlock,
                            clientID = clientID,
                            intent = Request.Response.BLOCK.intent
                        )
                    )
                        .map { Sendable.fromJson<Status>(it) }
                        .printStatus("Block accepted successfully!",loggger,debugSuccess = true)
                        .map { hot.onNext(Tuples.of(realBlock, it)) }
                        .block()
//                    println("BLock accepted block done")
                } else {
                    bblock.nonce++
//                    println("Invalid hash:$hash")
                }
            }
        }
    }


}

