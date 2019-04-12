package systems.carson

import java.security.*
import javax.crypto.Cipher
import sun.misc.BASE64Encoder
import java.nio.charset.Charset


fun main() {
    val bob = Person("bob")
    val alice = Person("alice")

    val data = "This is some data!".toBytes()

    val signature = bob.sign(data)

    val verified = Person.verify(alice,signature,data)
    println(verified)
    println(bob)


}

private infix fun <A,B,C> Pair<A,B>.and(value :C):Triple<A,B,C> = Triple(first,second,value)

private fun String.toBytes():ByteArray = toByteArray(Charset.forName("UTF-8"))

class Signature(val byteArray: ByteArray)

class Person(val name :String){
//    private val sig: java.security.Signature = java.security.Signature.getInstance("SHA1WithRSA")


    private val keyPair = buildKeyPair()

    val publicKey :PublicKey
        get() = keyPair.public
    private val privateKey :PrivateKey
        get() = keyPair.private

    fun sign(data :ByteArray):Signature{
        sig.initSign(privateKey)
        sig.update(data)
        return Signature(sig.sign())
    }
    companion object {
        private val sig = java.security.Signature.getInstance("SHA1WithRSA")

        fun verify(publicKey: PublicKey, signature: Signature, data: ByteArray): Boolean {
            sig.initVerify(publicKey)
            sig.update(data)
            return sig.verify(signature.byteArray)
        }
        fun verify(person :Person, signature: Signature,data: ByteArray):Boolean =
                verify(person.publicKey, signature, data)
    }

    override fun toString(): String {
        return "Person(name=$name, pubKey=${BASE64Encoder().encode(publicKey.encoded).substring(0,10)})"
    }
}

fun buildKeyPair(): KeyPair {
    val keySize = 2048
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(keySize)
    return keyPairGenerator.genKeyPair()
}

fun encrypt(privateKey: PrivateKey, message: String): ByteArray {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, privateKey)

    return cipher.doFinal(message.toByteArray())
}

fun decrypt(publicKey: PublicKey, encrypted: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.DECRYPT_MODE, publicKey)

    return cipher.doFinal(encrypted)
}