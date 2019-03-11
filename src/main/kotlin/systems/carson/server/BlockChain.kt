package systems.carson.server

import systems.carson.shared.*
import java.util.*

class BlockChain{
    public val transactions :List<Transaction>
        get() = blocks.flatMap { it.transactions.list }

    private val blocks = mutableListOf(
        Block(
            Transactions(emptyList()),
            -1,
            System.currentTimeMillis(),
            "starter"
        )
    )

    /**returns an error message if failed, null if success */
    fun addBlock(b : Block): Optional<String> {
        synchronized(lock) {
            blocks.add(b)
            val s = checkBlockchain()
            s.ifPresent{ blocks.removeAt(blocks.size - 1) }
            return s
        }
    }

    private fun checkBlockchain(): Optional<String> {
        for(i in 1 until blocks.size){
            val block = blocks[i]
            val prev = blocks[i - 1]
            if(!block.hash().isValidHash()) return Optional.of("Block $i is invalid")
            if(block.lastHash != prev.hash()) return Optional.of("Block $i has the wrong lastHash")
        }
        return Optional.empty()
    }

    fun last() : Block {
        return blocks.last()
    }

    fun users():Map<String,Int>{
        val map = mutableMapOf<String,Int>()
        transactions.forEach {
            map[it.from] = (map[it.from]?: 0) - it.amount
            map[it.to] = (map[it.to]?: 0) + it.amount
        }
        return map
    }


    override fun toString(): String {
        return gson.toJson(this)
    }
}