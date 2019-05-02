package com.gumbocoin.cli

import systems.carson.base.Action
import systems.carson.base.Block
import systems.carson.base.hash

class MutableBlock(
    var author: String,
    var actions: List<Action>,
    var timestamp: Long,
    var nonce: Long,
    var difficulty: Long,
    var lasthash: String,
    var signature: String
) {
    fun toBlock(): Block {
        return Block(
            author = author,
            actions = actions,
            timestamp = timestamp,
            nonce = nonce,
            difficulty = difficulty,
            lasthash = lasthash,
            signature = signature
        )
    }

    constructor(b: Block) : this(
        author = b.author,
        actions = b.actions,
        timestamp = b.timestamp,
        nonce = b.nonce,
        difficulty = b.difficulty,
        lasthash = b.lasthash,
        signature = b.signature
    )

    fun hash() = this.toBlock().hash()
}