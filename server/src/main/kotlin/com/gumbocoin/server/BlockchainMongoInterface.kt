package com.gumbocoin.server

import systems.carson.base.Block
import systems.carson.base.Blockchain
import systems.carson.base.Signature
import java.nio.charset.Charset
import com.gumbocoin.server.BlockchainMongoInterface.BlockchainSource.*
import com.mongodb.reactivestreams.client.MongoCollection
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono

private const val USE_DATABASE_FLAG = "USE_DATABASE"

object BlockchainMongoInterface {

    private enum class BlockchainSource{
        DATABASE,
        GENERATE;
        companion object{
            val default = GENERATE
        }
    }

    private val flag  by lazy {
        val flag = System.getenv(USE_DATABASE_FLAG).toUpperCase()
        if(flag.isBlank())
            return@lazy BlockchainSource.default
        return@lazy BlockchainSource.valueOf(flag)
    }

    private var blockchain :Blockchain = generateCorrectBlockchain()

    private fun generateCorrectBlockchain():Blockchain{
        return when(flag){
            DATABASE -> generateFromDatabase()
            GENERATE -> generateNewBlockchain()
        }
    }

    private fun generateFromDatabase():Blockchain{
        val blockchainSource = Mongo.blockchain
        //TODO store blockchains as BSON object, but include their size
        //    then pull the biggest blockchain
        //    no deletions will ever need to be done
    }

    private fun generateNewBlockchain(): Blockchain {
        return Blockchain(
            listOf(
                block(
                    KeyManager.server.sign(block(Signature.VOID.toBase64()).hash.toByteArray(Charset.forName("UTF-8"))).toBase64()
                )
            )
        )
    }
}


private fun block(sig: String): Block {
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
