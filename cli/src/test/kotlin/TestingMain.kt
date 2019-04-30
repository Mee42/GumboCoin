import com.gumbocoin.cli.*
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import systems.carson.base.*
import java.util.*


val logger = GLogger.logger()

val network = GLogger.logger("Network")


val me = Person.generateNew()
val clientID = UUID.randomUUID().toString().split("-")[0]


fun main() {
    println("STARTING CLI: MODE: ${ReleaseManager.release}")

    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN")

    val outputManager = OutputGLogger()
    outputManager.setLevel(GLevel.INFO)
    GManager.addLoggerImpl(outputManager)

    val socket = RSocketFactory.connect()
        .transport(TcpClientTransport.create("localhost", PORT))
        .start()
        .block()!!

    (0..2).forEach {
        time("Ping-$it") {
            socket.requestResponse(RequestDataBlob(Request.Response.PING, clientID), me).block()
        }
    }

    socket.requestResponse(SignUpDataBlob(clientID, me.publicKeyBase64()), me)
        .mapFromJson<Status>()
        .printStatus("Registered successfully")
        .block()

    val threadedMiner = ThreadedMiner(socket, GLogger.logger("Miner"), me, clientID)

    socket.requestStream(RequestDataBlob(Request.Stream.BLOCKCHAIN_UPDATES, clientID), me)
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

    threadedMiner.mineAbout(3)

    socket.requestResponse(StringDataBlob(clientID, clientID, Request.Response.MONEY.intent), me)
        .map { Sendable.fromJson<SendableInt>(it) }
        .map { println("Money:${it.value}") }
        .block()

    socket.requestResponse(
        TransactionDataBlob(
            clientID,
            TransactionAction.sign(
                clientID = clientID,
                recipientID = "server",
                amount = 2,
                person = me
            )
        ), me
    )
        .map { Sendable.fromJson<Status>(it) }
        .printStatus("Transaction successful")
        .block()

    threadedMiner.mineAbout(3)

    socket.requestResponse(
        DataSubmissionDataBlob(
            clientID = clientID,
            action = DataAction.sign(
                clientID = clientID,
                data = DataPair("name", "Carson Graham"),
                person = me
            )
        ), me
    )
        .map { Sendable.fromJson<Status>(it) }
        .printStatus("Data submission successful")
        .block()

    threadedMiner.mineAbout(3)

    socket.requestResponse(StringDataBlob(clientID, clientID, Request.Response.MONEY.intent), me)
        .map { Sendable.fromJson<SendableInt>(it) }
        .map { println("Money:${it.value}") }
        .block()

    threadedMiner.stop()



    threadedMiner.exit()
}

fun ThreadedMiner.mineAbout(i: Int) {
    start()
    toFlux()
        .take(i.toLong())
        .blockLast()
    stop()
}
