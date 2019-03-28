package systems.carson.server

import io.rsocket.*
import io.rsocket.transport.netty.server.TcpServerTransport
import org.apache.commons.codec.digest.DigestUtils
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.toFlux
import systems.carson.shared.*
import java.util.*


fun main() {
    startServer()
}

val blockchain = BlockChain()

val lastHashFlux: DirectProcessor<Block> = DirectProcessor.create<Block>()

class Lock
val lock = Lock()//lock for the blockchain

var transactionCache = mutableListOf<Transaction>()
val transactionFlux: DirectProcessor<Transactions> = DirectProcessor.create<Transactions>()

val random = Random()


val genericStream = DirectProcessor.create<GenericStreamBlob>()



fun startServer() {
    genericStream.doOnNext { println(it) }.subscribe()

    /** actual processing */
    RequestResponse[RequestString.PING] = { println("got a ping from ${it.data.id}");payloadOf("pong") }

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
        result.ifNotPresent {
            lastHashFlux.onNext(block)
            transactionCache = mutableListOf()
            transactionFlux.onNext(Transactions(transactionCache))
        }
        result.ifNotPresent {
            val users = blockchain.users()
            val changedUsers = users
                .filter { name -> block.transactions.list.any { w -> w.from == name.key || w.to == name.key } }
                .map { name: Map.Entry<String, Int> -> UserBlob(
                    id = name.key,
                    blocksOwned = blockchain.transactions.filter { w -> w.from == "server" && w.to == name.key }.count(),
                    coins = users.getValue(name.key),
                    minerOnline = false) }
            genericStream.onNext(GenericStreamBlob(
                usersUpdated = changedUsers,
                transactionsPassed = block.transactions.list,
                totalCoins = blockchain.totalCoins))
        }
        payloadOf(
            if(result.isPresent)
                "${Const.BLOCK_ADD_FAILED.string} ${result.get()}"
            else
                Const.BLOCK_ADDED_SUCCESS.string)
    }

    RequestResponse[RequestString.AUTH] = {
        payloadOf("true")
    }

    /** get requests */
    RequestResponse[RequestString.USER_AMOUNT] = { payloadOf("" + (blockchain.users()[it.data.id] ?: 0)) }
    RequestResponse[RequestString.BLOCKCHAIN_PRETTY] = { payloadOf(pretty.toJson(blockchain)) }
    RequestResponse[RequestString.USERS] = { payloadOf(pretty.toJson(blockchain.users())) }
    RequestResponse[RequestString.LATEST_BLOCK] = { payloadOf(gson.toJson(blockchain.last())) }
    RequestResponse[RequestString.LATEST_TRANSACTIONS] = { payloadOf(gson.toJson(Transactions(transactionCache))) }

    /** update streams */
    RequestStream[RequestString.LATEST_BLOCK] = { lastHashFlux.toFlux().map { gson.toJson(it) }.map { payloadOf(it) } }
    RequestStream[RequestString.GENERIC_STREAM] = { genericStream.toFlux().map { gson.toJson(it) }.map { payloadOf(it) }}
    RequestStream[RequestString.LATEST_TRANSACTIONS] = { transactionFlux.toFlux().map { gson.toJson(it) }.map { payloadOf(it) } }


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
