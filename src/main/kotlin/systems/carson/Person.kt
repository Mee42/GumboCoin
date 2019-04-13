package systems.carson

import org.apache.commons.codec.digest.DigestUtils
import java.security.*
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Person private constructor(val name :String,
                  private val keyPair: KeyPair) {

    val publicKey : PublicKey
        get() = keyPair.public ?: error("Current Person implementation doesn't support functions that use the public key")
    private val privateKey : PrivateKey
        get() = keyPair.private ?: error("Current Person implementation doesn't support functions that use the private key")


    fun getPrivateKeyy():PrivateKey{
        return privateKey
    }

    fun isValid() = fromPrivateKey(name,privateKey).publicKey == publicKey

    companion object {
        const val MAX_RSA_BYTES = 245
        private val sig = java.security.Signature.getInstance("SHA1WithRSA")

        fun verify(publicKey: PublicKey, signature: Signature, data: ByteArray): Boolean {
            sig.initVerify(publicKey)
            sig.update(data)
            return sig.verify(signature.byteArray)
        }
        fun verify(person :Person, signature: Signature, data :ByteArray):Boolean {
            return verify(person.publicKey,signature,data)
        }

        /** This will encrypt over 245 bytes */
        fun encryptAES(data :ByteArray,person :Person):EncryptedData = encryptAES(data,person.publicKey)

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
        //buildKeyPair(DigestUtils.sha1(name)!!.contentHashCode().toLong())
        private fun buildKeyPair(seed :Long): KeyPair {
            val random = SecureRandom.getInstance("SHA1PRNG")
            random.setSeed(seed)
            val keySize = 2048
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(keySize,random)
            return keyPairGenerator.genKeyPair()
        }

        private fun buildKeyPair():KeyPair {
            val keySize = 2048
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(keySize)
            return keyPairGenerator.genKeyPair()
        }

        /** This needs to be below 245 bytes */
        fun encrypt(data :ByteArray, publicKey: PublicKey): ByteArray {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(data)
        }

        /** This needs to be below 245 bytes */
        fun encrypt(data :ByteArray, person :Person):ByteArray = encrypt(data,person.publicKey)



        //generators
        fun fromKeyPair(name :String, keyPair: KeyPair):Person = Person(name, keyPair)
        fun fromPublicKey(name :String, publicKey: PublicKey):Person = Person(name,KeyPair(publicKey,null))
        fun fromPrivateKey(name :String, privateKey: PrivateKey):Person {
            //attempt to find the correct public key
            if(privateKey !is RSAPrivateCrtKey)
                error("Private key is not a RSAPrivateCrtKey and does not contain enough data to compute the public key")
            val spec = RSAPublicKeySpec(privateKey.modulus,privateKey.publicExponent)
            val factory = KeyFactory.getInstance("RSA")
            val publicKey = factory.generatePublic(spec)
            return Person(name, KeyPair(publicKey,privateKey))
        }
        fun deterministicFromName(name :String) :Person = Person(name,buildKeyPair(DigestUtils.sha1(name)!!.contentHashCode().toLong()))
        fun generateNew(name :String) :Person = Person(name, buildKeyPair())
    }

    override fun toString(): String {
        return "Person(name=$name, finger=${fingerprint().substring(0,10)})"
    }


    fun sign(data :ByteArray): Signature {
        sig.initSign(privateKey)
        sig.update(data)
        return Signature(sig.sign())
    }

    fun fingerprint():String = DigestUtils.sha1Hex(publicKey.encoded)

    /** This needs to be below 245 bytes */
    fun decrypt(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        try {
            return cipher.doFinal(encrypted)
        }catch(e : BadPaddingException){
//            if(e.message == "Decryption error"){
                val new = IllegalAccessException("Unable to decrypt data with given key")
                new.addSuppressed(e)
                throw new
//            }
//            throw e
        }
    }



    fun decryptAES(data : EncryptedData) :ByteArray{
        val iv = data.iv
        val ivParameterSpec = IvParameterSpec(iv)

        val decryptedSecretKey = decrypt(data.encryptedSecretKey)

        val secretKey = SecretKeySpec(decryptedSecretKey, 0, decryptedSecretKey.size, "AES")


        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return aesCipher.doFinal(data.encryptedData)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person

        if (name != other.name) return false
        if(keyPair.private != other.keyPair.private)return false
        if(keyPair.public != other.keyPair.public)return false


        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }

}