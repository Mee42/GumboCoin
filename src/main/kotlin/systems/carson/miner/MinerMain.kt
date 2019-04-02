package systems.carson.miner

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import systems.carson.shared.*
import java.util.*


fun main() {
    startMiner()
}

lateinit var lastBlock :Block
var transactions = Transactions(emptyList())

class SocketHolder(val socket :RSocket)
lateinit var s :SocketHolder



fun startMiner() {
    Producer.id = UUID.randomUUID().toString().split("-")[0]

    val socket: RSocket = RSocketFactory.connect()
        .transport(TcpClientTransport.create("72.66.54.109", 7000))
        .start()
        .block()!!

    s = SocketHolder(socket)

    val last = socket.requestResponse(DataBlob(RequestString.LATEST_BLOCK.string).payload())
        .map { it.dataUtf8 }
        .map { gson.fromJson(it,Block::class.java) }
        .block()
    if(last != null)
        lastBlock = last
    else
        System.err.println("Last block gotten on startup is null")


    socket.requestStream(DataBlob(RequestString.LATEST_BLOCK.string).payload())
        .map { it.dataUtf8 }
        .map { gson.fromJson(it,Block::class.java) }
        .subscribe {
            lastBlock = it
            Miner.update()
        }

    transactions = socket.requestResponse(DataBlob(RequestString.LATEST_TRANSACTIONS.string).payload())
        .map { it.dataUtf8 }
        .map { gson.fromJson(it, Transactions::class.java) }
        .map { Arrays.asList(*it.list.toTypedArray(), Transaction(Producer.id,"server",1)) }
        .map {Transactions(it) }
        .block()!!

    println("transactions set to $transactions")
    socket.requestStream(DataBlob(RequestString.LATEST_TRANSACTIONS.string).payload())
        .map { it.dataUtf8 }
        .map { gson.fromJson(it, Transactions::class.java) }
        .map { Arrays.asList(*it.list.toTypedArray(), Transaction(Producer.id,"server",1)) }
        .subscribe {
            transactions = Transactions(it)
            Miner.update()
        }


    Miner.startMining()
    println("miner ---- ${Producer.id}")
}
