package systems.carson.base

import org.apache.commons.codec.digest.DigestUtils
import java.nio.charset.Charset
import java.util.*


fun Blockchain.newBlock(block: Block): Blockchain {
    return Blockchain(blocks + block)
}


/** Returns empty if it's valid, otherwise the reason that it's invalid */
fun Blockchain.isValid(): Optional<String> {
    for (i in 1 until blocks.size) {
        val block = blocks[i]
        val hash = block.hash
        if (!hash.isValid())
            return Optional.of("Block hash is not correct")
        val old = blocks[i - 1]
        if (block.lasthash != old.hash)
            return Optional.of("Block lasthash is incorrect\nNeeded ${old.hash}, got ${block.lasthash}")
        val sig = Signature.fromBase64(block.signature)
        val user = this.subBlockchain(i + 1).users.firstOrNull { it.id == block.author }
            ?: return Optional.of("Can't find user on the blockchain")
        val valid = Person.verify(user.person, sig, block.excludeSignature().toByteArray(Charset.forName("UTF-8")))
        // make sure it's signed properly.
        // The user needs to be on the blockchain because we need both the public key and the signature to verify it
        if (!valid)
            return Optional.of("User signature is invalid")
    }
    return Optional.empty()
}


fun Blockchain.subBlockchain(endIndex: Int): Blockchain {
    return Blockchain(this.blocks.subList(0, endIndex))
}


const val zeros = 5
fun String.isValid(): Boolean {
    return this.length > zeros && !this.substring(0, zeros).any { it != '0' }
}

fun Block.excludeSignature(): String {
    data class BlockWithNoSignature(
        val author: String,
        val actions: List<Action>,
        val timestamp: Long,
        val nonce: Long,
        val difficulty: Long,
        val lasthash: String
    )

    val b = BlockWithNoSignature(
        author,
        actions,
        timestamp,
        nonce,
        difficulty,
        lasthash
    )
    return serialize(b)
}

fun Block.hash(): String {
    var str = excludeSignature()
    for (i in 0..difficulty)
        str = DigestUtils.sha256Hex(str + this.nonce)
    return str
}