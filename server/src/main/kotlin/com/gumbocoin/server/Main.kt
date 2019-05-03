package com.gumbocoin.server


import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.core.publisher.DirectProcessor
import systems.carson.base.*
import java.time.Duration
import java.time.Instant

val targetTimeBetweenBlocks: Duration = Duration.ofMinutes(1)
const val defaultDifficulty = 25L
const val blocksToTake = 5


val blockchain: Blockchain
    get() = BlockchainManager.blockchain

val diff: Long
    get() {

        if (blockchain.blocks.size < blocksToTake)//start out like this and get a feel for the power
            return defaultDifficulty
        val lastFiveBLocks = blockchain.blocks.subList(blockchain.blocks.size - blocksToTake,blockchain.blocks.size)
        val time:Duration = Duration
            .between(Instant.ofEpochMilli(lastFiveBLocks.first().timestamp),
                Instant.ofEpochMilli(lastFiveBLocks.last().timestamp))
            .abs()//make sure it's positive
        val averageDiffs = lastFiveBLocks.map { it.difficulty }.average().toLong()
        val averageTimeBetweenBlocks:Duration = time.dividedBy(blocksToTake.toLong() - 1)
        val averageTimePerDiff:Duration = averageTimeBetweenBlocks.dividedBy(averageDiffs)
        return targetTimeBetweenBlocks.toMillis() / averageTimePerDiff.toMillis()
    }

val logger = GLogger.logger()

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

    logger.log(GLevel.IMPORTANT, "Server started. Mode: ${ReleaseManager.release}")

    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()

    logger.info("Killing https")
    https.kill()
    logger.info("Done")
}