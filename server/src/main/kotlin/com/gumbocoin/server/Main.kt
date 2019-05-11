package com.gumbocoin.server


import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.TcpServerTransport
import reactor.core.publisher.DirectProcessor
import systems.carson.base.*
import java.io.PrintWriter
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import kotlin.random.Random

val targetTimeBetweenBlocks: Duration = Duration.ofMinutes(1)
const val defaultDifficulty = 5L
const val blocksToTake = 5


val blockchain: Blockchain
    get() = BlockchainManager.blockchain

val diff: Long
    get() {
        if(inputArguments.devFlags.contains(DevFlags.LOW_DIFF)){
            return defaultDifficulty
        }
        if (blockchain.blocks.size < blocksToTake){//start out like this and get a feel for the power
            return defaultDifficulty
        }
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


lateinit var inputArguments :InputArguments

fun main(args :Array<String>) {



    val data = Random.Default.nextBytes(16 * 16)


    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    println("gen IV")

    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(128)
    val secretKey = keyGen.generateKey()
    println("gen secret key")

    val ivParameterSpec = IvParameterSpec(iv)
    val aesCipher = Cipher.getInstance(Person.AES_CIPHER)
    aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
    println("init aes")

    val final = aesCipher.doFinal(data)
    println("did final")
    val encrypted =  EncryptedBytes(iv, Person.encryptRSA(secretKey.encoded, Person.default), final)

    println("Encrypted")
    val plain = Person.default.decryptAES(encrypted)
    println("Decrypted")
    if(!data.contentEquals(plain)){
        error("fuck")
    }


    System.exit(99)

    try {
        inputArguments = ArgParser(args).parseInto(::InputArguments)
    }catch(e :ShowHelpException){
        val writer =  PrintWriter(System.err)
        e.printUserMessage(
            writer = writer ,
            programName = "server",
            columns = 1000
        )
        writer.flush()
        System.out.flush()
        System.exit(0)
    }
        println("STARTING SERVER: MODE: ${inputArguments.release}")

    val outputLogger = OutputGLogger()
    outputLogger.setLevel(GLevel.DEBUG)

    val fileLogger = FileGLogger()
    fileLogger.setLevel(GLevel.DEBUG)
    GManager.addLoggerImpl(fileLogger)
     //parse the arguments

    GManager.addLoggerImpl(outputLogger)


    val closable = RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create(PORT.getValue(inputArguments.release)))
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



    logger.info("Success!")


    logger.log(GLevel.IMPORTANT, "Server started. Mode: ${inputArguments.release}")

    (closable.block() ?: error("CloseableChannel did not complete with a value"))
        .onClose()
        .block()

    logger.info("Killing https")
    https.kill()
    logger.info("Done")
}