package systems.carson

import java.security.KeyPair
import java.security.PublicKey

class Signature(val byteArray: ByteArray)


class PublicPerson(name :String,private val publicKeyIn : PublicKey) :Person(name){
    override val keyPair: KeyPair
        get() = KeyPair(publicKeyIn,null)
}

class EncryptedData(val iv :ByteArray, val encryptedSecretKey :ByteArray,val encrypedData :ByteArray)


