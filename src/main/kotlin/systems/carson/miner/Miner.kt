package systems.carson.miner

import io.rsocket.Payload
import systems.carson.server.payloadOf
import systems.carson.shared.*
import java.util.*
import kotlin.concurrent.thread

object Miner{
    private var mined = 0
    private var failed = 0
    private var mine = false
    fun startMining() { mine = true }
    fun stopMining() { mine = false }

    var update = false
    fun update() { update = true }

    init {
        thread(start = true) {
            var block =
                blockFrom(lastBlock, transactions)

            while (true) {
                if (update) {
                    update = false
                    block = blockFrom(
                        lastBlock,
                        transactions
                    )
                }
                if (mine) {
                    if (block.hash().isValidHash()) {
                        //submit the block for checking, stop till we get a response
                        stopMining()
                        s.socket.requestResponse(
                            DataBlob(
                                intent = RequestString.BLOCK.string,
                                data = gson.toJson(block)
                            ).payload()
                        )
                            .map { it.dataUtf8 }
                            .map {
                                if (Const.matches(
                                        it,
                                        Const.BLOCK_ADD_FAILED
                                    )
                                )
                                    Optional.of(
                                        it.replaceFirst(
                                            Const.BLOCK_ADD_FAILED.string,
                                            ""
                                        )
                                    )
                                else
                                    Optional.empty()
                            }
                            .map { it.map { w -> w.trim() } }
                            .subscribe {
                                it.ifPresent { w ->
                                    System.err.println("Block rejected: $w")
                                    failed++
                                }
                                it.ifNotPresent {
                                    println("Minned block ${block.hash()} ($hashes,$hashesPerSecond)")
                                    mined++
                                }
                                startMining()
                            }

                    } else {
                        for (i in 0..100) {
                            if (block.hash().isValidHash()) break
                            block.nonce++
                            hashes++
                        }//run 100 before checking whether or not we need to update
                        //makes it faster? not sure
                    }
                } else {
                    Thread.sleep(100)
                }
            }
        }
    }
}


var hashes = 0L
var startTime = System.currentTimeMillis()

val hashesPerSecond :Long
    get() = hashes / ((System.currentTimeMillis() - startTime)/1000)


val random = Random()
fun blockFrom(last :Block, transactions: Transactions):Block{
    return Block(
        transactions = transactions,
        nonce = random.nextLong(),
        timestamp = System.currentTimeMillis(),
        lastHash = last.hash())
}
