package systems.carson

import org.apache.commons.codec.digest.DigestUtils
import java.security.*
import javax.crypto.Cipher
import sun.misc.BASE64Encoder
import java.nio.charset.Charset
import kotlin.random.Random
import javax.crypto.spec.IvParameterSpec
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec


//const val debug = true

fun alice():Person{
    val alice = Person("Alice")
    return PublicPerson(alice.name,alice.publicKey)
}

fun main() {
    val bob = Person("Bob")
    val alice = alice()


    val data = Random(100).nextBytes(ByteArray(10000) { -1 })
    val encrypted = bob.encryptAES(data)


}


private fun ByteArray.hash() = DigestUtils.sha1Hex(this.contentToString())

private fun String.toBytesUTF8():ByteArray = toByteArray(Charset.forName("UTF-8"))

class Signature(val byteArray: ByteArray)

class PublicPerson(name :String,private val publicKeyIn :PublicKey) :Person(name){
    override val keyPair: KeyPair
        get() = KeyPair(publicKeyIn,null)
}

open class Person(val name :String) {

    protected open val keyPair = buildKeyPair(DigestUtils.sha1(name)!!.contentHashCode().toLong())

    val publicKey :PublicKey
        get() = keyPair.public
    private val privateKey :PrivateKey
        get() = keyPair.private ?: error("Current Person implementation doesn't support functions that use the private key")

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

    fun publicKeyString() :String = BASE64Encoder().encode(publicKey.encoded).replace("\n","")

    override fun toString(): String {
        return "Person(name=$name, finger=${fingerprint().substring(0,10)})"
    }

    fun fingerprint():String = DigestUtils.sha1Hex(publicKey.encoded)

    /** This needs to be below 245 bytes */
    fun encrypt(data :ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decrypt(privateKey: PrivateKey, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        return cipher.doFinal(encrypted)
    }

    fun decrypt(encrypted: ByteArray):ByteArray = decrypt(privateKey,encrypted)

    /** This will encrypt over 245 bytes */
    fun encryptAES(data :ByteArray, publicKey :PublicKey): EncryptedData {
        val iv = ByteArray(16) { -1 }
        SecureRandom.getInstanceStrong().nextBytes(iv)

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        val secretKey = keyGen.generateKey()


        val ivParameterSpec = IvParameterSpec(iv)
        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

        val final = aesCipher.doFinal(data)
        return EncryptedData(iv,encrypt(secretKey.encoded),final)
        //need to return encrypted secret key and the encrypted message
    }

    fun decryptAES(data :EncryptedData,privateKey :PrivateKey) :ByteArray{
        val iv = data.iv
        val ivParameterSpec = IvParameterSpec(iv)

        val decryptedSecretKey = decrypt(privateKey,data.encryptedSecretKey)

        val secretKey = SecretKeySpec(decryptedSecretKey,0, decryptedSecretKey.size,"AES")


        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return aesCipher.doFinal(data.encrypedData)
    }
}

class EncryptedData(val iv :ByteArray, val encryptedSecretKey :ByteArray,val encrypedData :ByteArray)


private fun buildKeyPair(seed :Long): KeyPair {
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(seed)
    val keySize = 2048
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(keySize,random)
    return keyPairGenerator.genKeyPair()
}

