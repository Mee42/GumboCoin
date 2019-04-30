package com.gumbocoin.server


import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.core.publisher.DirectProcessor
import systems.carson.base.*
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant

val targetBlockTime: Duration = Duration.ofMinutes(1)
val blockInTheLast : Duration = Duration.ofMinutes(2)
const val defaultDifficulty = 4L


var blockchain: Blockchain =
    Blockchain(
        listOf(
            block(
                KeyManager.server.sign(block(Signature.VOID.toBase64()).hash.toByteArray(Charset.forName("UTF-8"))).toBase64()
            )
        )
    )

val diff :Long
    get(){
        if(blockchain.blocks.size < 5)
            return defaultDifficulty

//        val lastFiveBlocks = blockchain.blocks.subList(blockchain.blocks.size - 5,blockchain.blocks.size)


        val timedBlocks = blockchain.blocks.filter { Duration.between(Instant.ofEpochMilli(it.timestamp), Instant.now()).abs().toMillis() < blockInTheLast.toMillis() }

        if(timedBlocks.size <= 1){
            //if there are no blocks, either no one is mining or no one has mined anything in the last hour
            // just don't change diff. It's not worth the time
            return blockchain.blocks.last().difficulty//don't change it
        }

        //  diffsOverTime = sum { it -> it.diff }
        //  blocksOverTime = count()
        //  wantedBlocksOverTime = constant

        //  diffsOverTime      wantedDiffsOverTime
        // ---------------  = ---------------------
        //  blocksOverTime     wantedBlocksOverTime

        // wantedDiffsOverTime = (diffsOverTime * wantedBlocksOverTime) / blocksOverTime
        // diff = wantedDiffsOverTime / wantedBlocksOverTime

        // diff = ((diffsOverTime * wantedBlocks ) / blocks) / wantedBlocksOverTime


        // diffsOverTime = 52
        // blocksOverTime = 13
        // wantedBlocksOverTime = 8
        // diff = diffsOverTime / wantedBlocksOverTime
        val time =  Duration.between(Instant.ofEpochMilli(timedBlocks.first().timestamp),
            Instant.ofEpochMilli(timedBlocks.last().timestamp)).abs()

        println("Time: ${time.toMillis()}")
        println("Target time block time: ${targetBlockTime.toMillis()}")
        println("blockInTheLast: ${blockInTheLast.toMillis()}")
        if(time.toMillis() < targetBlockTime.toMillis()){
            println("Not enough time has passed")
            return defaultDifficulty
        }
        val wantedBlocks = time.toMillis() / targetBlockTime.toMillis()
        println("Wanted blocks:$wantedBlocks")
        val diffsOverTime = timedBlocks.fold(0L) { a,b -> a + b.difficulty }
        println("Diffs over time: $diffsOverTime")
        val blocksOverTime = timedBlocks.size
        println("BLocks over time: $blocksOverTime")
        println("Wanted blocks over time:$wantedBlocks")

        val newDiff = diffsOverTime / wantedBlocks
        println("newDiff: $newDiff")
        return if(newDiff > 3)
            newDiff
        else {
            System.out.println("newDiff is <= 3")
            4L
        }
    }


val logger = GLogger.logger()

fun block(sig: String): Block {
    return Block(
        author = "server",
        actions = listOf(),
        timestamp = System.currentTimeMillis(),
        nonce = 0,
        difficulty = defaultDifficulty,
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
    println("STARTING SERVER: MODE: ${ReleaseManager.release}")

    val outputLogger = OutputGLogger()
    outputLogger.setLevel(GLevel.DEBUG)

    GManager.addLoggerImpl(outputLogger)
    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("0.0.0.0", PORT))
        .start()!!

    logger.info("Network initialized")
    logger.info("blockchain:" + serialize(blockchain))

    val https = startHttps()

    logger.info("Https setup")

    logger.info("Starting discord connection")

    DiscordManager.client.login().subscribe()

    val discordLogger = DiscordLogger()
    discordLogger.setLevel(GLevel.WARNING)
    GManager.addLoggerImpl(discordLogger)

    logger.log(GLevel.IMPORTANT,"Server started. Mode: ${ReleaseManager.release}")

    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()

    logger.info("Killing https")
    https.kill()
    logger.info("Done")
}