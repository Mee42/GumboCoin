package systems.carson

import org.apache.commons.codec.digest.DigestUtils
import sun.misc.BASE64Encoder
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

open class Person(val name :String) {

    protected open val keyPair = buildKeyPair(DigestUtils.sha1(name)!!.contentHashCode().toLong())

    val publicKey : PublicKey
        get() = keyPair.public
    private val privateKey : PrivateKey
        get() = keyPair.private ?: error("Current Person implementation doesn't support functions that use the private key")

    fun sign(data :ByteArray): Signature {
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

        private fun buildKeyPair(seed :Long): KeyPair {
            val random = SecureRandom.getInstance("SHA1PRNG")
            random.setSeed(seed)
            val keySize = 2048
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(keySize,random)
            return keyPairGenerator.genKeyPair()
        }

    }

    override fun toString(): String {
        return "Person(name=$name, finger=${fingerprint().substring(0,10)})"
    }

    fun fingerprint():String = DigestUtils.sha1Hex(publicKey.encoded)

    /** This needs to be below 245 bytes */
    fun encrypt(data :ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    /** This needs to be below 245 bytes */
    fun decrypt(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(encrypted)
    }


    /** This will encrypt over 245 bytes */
    fun encryptAES(data :ByteArray, publicKey : PublicKey): EncryptedData {
        val iv = ByteArray(16) { -1 }
        SecureRandom.getInstanceStrong().nextBytes(iv)

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128)
        val secretKey = keyGen.generateKey()


        val ivParameterSpec = IvParameterSpec(iv)
        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

        val final = aesCipher.doFinal(data)
        return EncryptedData(iv, encrypt(secretKey.encoded,publicKey), final)
        //need to return encrypted secret key and the encrypted message
    }

    fun decryptAES(data : EncryptedData) :ByteArray{
        val iv = data.iv
        val ivParameterSpec = IvParameterSpec(iv)

        val decryptedSecretKey = decrypt(data.encryptedSecretKey)

        val secretKey = SecretKeySpec(decryptedSecretKey, 0, decryptedSecretKey.size, "AES")


        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return aesCipher.doFinal(data.encrypedData)
    }
}