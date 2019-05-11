package com.gumbocoin.cli

import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import systems.carson.base.*
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread


class Update(val newBlock: Block)


class ThreadedMiner(context : Context, sleep: Long = 100) {


    private var running = false
    val isRunning: Boolean
        get() = running
    private val update = Collections.synchronizedList(mutableListOf<Update>())
    private var exit = false
    private var sleepLength = sleep
    var statuses = mutableListOf<Status>()
    val mined: Int
        get() = statuses.count { !it.failed }
    val failed: Int
        get() = statuses.count { it.failed }

    fun start() {
        running = true
    }


    fun stop() {
        running = false
    }

    fun exit() {
        running = false; exit = true
    }

    fun update(u: Update) = update.add(u)

    private val hot =
        DirectProcessor.create<Tuple2<Block, Status>>()

    fun toFlux(): Flux<Tuple2<Block, Status>> {
        return hot.toFlux()
    }

    init {
        context.socket.requestStream(RequestDataBlob(Request.Stream.BLOCKCHAIN_UPDATES, context.credentials.clientID), context.credentials.keys)
            .map { Sendable.fromJson<ActionUpdate>(it) }
            .map {
                update(
                    Update(
                        Block(
                            author = context.credentials.clientID,
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
//                    println("Updating block")
                    block = MutableBlock(update.removeAt(0).newBlock)
                }

                val bblock = if (block == null) {
//                    println("Block is null")
                    Thread.sleep(sleepLength)
                    continue@root
                } else {
//                    println("Block is not null")
                    block
                }

                val hash = bblock.hash()
                if (hash.isValid()) {
//                    println("Found valid hash: $hash")
                    bblock.signature =
                        context.credentials.keys.sign(
                            bblock.toBlock().excludeSignature().toByteArray(
                                Charset.forName(
                                    "UTF-8"
                                )
                            )
                        ).toBase64()
                    val realBlock = bblock.toBlock()
                    val result = context.socket.requestResponse(
                        BlockDataBlob(
                            block = realBlock,
                            clientID = context.credentials.clientID,
                            intent = Request.Response.BLOCK.intent
                        ))
                        .map { Sendable.fromJson<Status>(it) }
//                        .printStatus(
//                            "Block accepted successfully!   hash:${bblock.hash()}",
//                            loggger,
//                            debugSuccess = true
//                        )
                        .map { hot.onNext(Tuples.of(realBlock, it));it }
                        .block() ?: Status(failed = true, errorMessage = "Server did not return anything")
                    statuses.add(result)
//                    println("BLock accepted block done")
                } else {
                    bblock.nonce++
//                    println("Invalid hash:$hash : ${bblock.hash()} : ${bblock.toBlock().hash()} : ${bblock.toBlock()}")
                }
            }
        }
    }


}