package systems.carson.server

import io.rsocket.*
import io.rsocket.transport.netty.server.TcpServerTransport
import org.apache.commons.codec.digest.DigestUtils
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import systems.carson.shared.*
import java.io.File
import java.time.Duration
import java.io.FileWriter
import java.io.BufferedWriter
import java.math.BigInteger
import java.util.*


//fun main() {
//    startServer()
//}

val blockchain = BlockChain()

val lastHashFlux: DirectProcessor<Block> = DirectProcessor.create<Block>()

class Lock
val lock = Lock()//lock for the blockchain

var transactionCache = mutableListOf<Transaction>()
val transactionFlux: DirectProcessor<Transactions> = DirectProcessor.create<Transactions>()

val random = Random()

fun startServer() {



    RequestResponse[RequestString.PING] = {
        println("got a ping from ${it.data.id}");payloadOf("pong")
    }

    RequestResponse[RequestString.BLOCK] = r@{
        val block = gson.fromJson(it.data.data, Block::class.java)
        //check transactions

        if(block.transactions.list.size != transactionCache.size + 1)
            return@r payloadOf( "${Const.BLOCK_ADD_FAILED.string} Transaction size different then expected")

        for(t in block.transactions.list){
            if(!(transactionCache.contains(t) || t == Transaction(it.data.id,"server",1))){
                return@r payloadOf("${Const.BLOCK_ADD_FAILED.string} Transaction unrecognized $t")
            }
        }
        for(t in transactionCache){
            if(!(block.transactions.list.contains(t))){
                return@r payloadOf("${Const.BLOCK_ADD_FAILED} Transaction not found on block $t")
            }
        }

        val result = blockchain.addBlock(block)
        if(!result.isPresent){
            lastHashFlux.onNext(block)
            transactionCache = mutableListOf()
            transactionFlux.onNext(Transactions(transactionCache))
        }
        payloadOf(
            if(result.isPresent)
                "${Const.BLOCK_ADD_FAILED.string} ${result.get()}"
            else
                Const.BLOCK_ADDED_SUCCESS.string)
    }

    RequestStream[RequestString.LATEST_BLOCK] = {lastHashFlux.toFlux().map { gson.toJson(it) }.map { payloadOf(it) } }
    RequestResponse[RequestString.LATEST_BLOCK] = { payloadOf(gson.toJson(blockchain.last())) }

    RequestResponse[RequestString.LATEST_TRANSACTIONS] = { payloadOf(gson.toJson(Transactions(transactionCache))) }
    RequestStream[RequestString.LATEST_TRANSACTIONS] = { transactionFlux.toFlux().map { gson.toJson(it) }.map { payloadOf(it) } }



    /** get requests */
    RequestResponse[RequestString.USER_AMOUNT] = { payloadOf("" + (blockchain.users()[it.data.id] ?: 0)) }
    RequestResponse[RequestString.BLOCKCHAIN_PRETTY] = { payloadOf(pretty.toJson(blockchain)) }
    RequestResponse[RequestString.USERS] = { payloadOf(pretty.toJson(blockchain.users())) }

    RSocketFactory.receive()
        .acceptor(MasterHandler())
        .transport(TcpServerTransport.create("localhost",7000))
        .start()
        .log()
        .block()!!
        .onClose()
        .block()

}
fun hash(s :String):String = DigestUtils.sha256Hex(s)

fun main() {
    val maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength("AES")
    println("Max Key Size for AES : $maxKeySize")
}