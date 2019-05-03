package com.gumbocoin.server

import com.gumbocoin.server.BlockchainManager.BlockchainSource.*
import com.mongodb.client.model.Filters
import org.bson.Document
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import systems.carson.base.*
import java.nio.charset.Charset
import kotlin.concurrent.thread
import org.bson.json.JsonWriterSettings




object BlockchainManager {
    private const val USE_DATABASE_FLAG = "USE_DATABASE"

    private enum class BlockchainSource{
        DATABASE,
        MEMORY,
        RESET_AND_CREATE;//like DATABASE but it wipes the database on startup
        companion object{
            val default = MEMORY
        }
    }

    private val flag  by lazy {
        val flag = System.getenv(USE_DATABASE_FLAG).toUpperCase()
        if(flag.isBlank())
            return@lazy BlockchainSource.default
        return@lazy valueOf(flag)
    }

    var blockchain :Blockchain = generateCorrectBlockchain()
        set(newBlockchain){
            if(!newBlockchain.isValidBoolean())
                error("Can't set to invalid blockchain: ${newBlockchain.isValid().orElseGet { "" } } ")
            field = newBlockchain
            thread(start = true) {
                when (flag) {
                    DATABASE,RESET_AND_CREATE -> {//write it to the database.
                        println("Inserting blockchain into database")
                        Mongo.blockchain.insertOne(blockchainToBson(newBlockchain))
                            .toMono()
                            .subscribe()
                    }
                    MEMORY -> { /* do nothing */ }
                }
            }
        }

    private fun generateCorrectBlockchain():Blockchain{
        return when(flag){
            DATABASE -> generateFromDatabase()
            MEMORY -> generateNewBlockchain()
            RESET_AND_CREATE -> resetAndCreate()
        }
    }

    private fun resetAndCreate():Blockchain {
        if(ReleaseManager.release == Release.MASTER){
            error("YOU IDIOT!")
        }
        Mongo.blockchain.deleteMany(Filters.exists("_id"))
            .toMono()
            .subscribe()
        return generateNewBlockchain()
    }


    private fun generateFromDatabase():Blockchain{
        val blockchainSource = Mongo.blockchain
        val blockchain = blockchainSource
            .find()
            .toFlux()
            .collectList()
            .block()!!
            .map { bsonToBlockchain(it) }
            .removeNull("Error when finding blockchains")
            .maxBy { it.blocks.size }  //take the one with the most blocks
            ?: error("Couldn't find the largest blockchain")
        if(!blockchain.isValidBoolean())
            error("blockchain loaded from database is invalid")
        return blockchain
    }

    private fun <T> List<T>?.removeNull(errorProducer :String):List<T>{
        if(this == null)
            error(errorProducer)
        return this
    }

    private fun bsonToBlockchain(doc : Document):Blockchain{
        val settings = JsonWriterSettings.builder()
            .int64Converter { value, writer -> writer.writeNumber(value!!.toString()) }
            .build()
        return deserialize(doc.toJson(settings))
    }
    private fun blockchainToBson(blockchain :Blockchain):Document{

        return Document.parse(serialize(blockchain))
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

}

