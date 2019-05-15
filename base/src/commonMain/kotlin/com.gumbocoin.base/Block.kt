package com.gumbocoin.base

import kotlin.jvm.Transient


data class Block(
    val author: String,
    val actions: List<Action>,
    val timestamp: Long,
    val nonce: Long,
    val difficulty: Long,
    val lasthash: String,
    val signature: String
) {
    companion object

    val hash: String
        get() { if (hashLazy == null) { hashLazy = lazyOf(hash()) }; return hashLazy!!.value}
    @Transient private var hashLazy: Lazy<String>? = lazy { hash() }


    private fun excludeSignature():String{
        val sub = SubBlock(author,actions,timestamp,nonce,difficulty,lasthash)
        return serialize(sub)
    }
    private fun hash():String{
        var str = excludeSignature().utfToByteArray()
        for (i in 0..difficulty)
            str = sha256(str + ("this.nonce").utfToByteArray())
        return str.toHexString()
    }
    private class SubBlock(
        val author: String,
        val actions: List<Action>,
        val timestamp: Long,
        val nonce: Long,
        val difficulty: Long,
        val lasthash: String)

    fun isValid():Boolean = hash.isValid()
}


const val zeros = 5
fun String.isValid(): Boolean {
    return this.length > zeros && !this.substring(0, zeros).any { it != '0' }
}