package com.gumbocoin.base

const val START_PUBLIC = "--- BEGIN PUBLIC KEY ---"
const val END_PUBLIC = "--- END PUBLIC KEY ---"
const val START_PRIVATE = "--- BEGIN PRIVATE KEY ---"
const val END_PRIVATE = "--- END PRIVATE KEY ---"


expect class KeyPair{
    fun serializeToKeyfile():String
    fun hasPrivateKey():Boolean
    fun hasPublicKey():Boolean
    companion object{
        fun deserializeFromKeyFile(string :String):KeyPair
    }
}

//expect object RSAEncryption
//
//expect object AESEncryption
//
//expect object HybridEncryption