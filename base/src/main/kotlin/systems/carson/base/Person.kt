package systems.carson.base

import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.security.*
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val START_PUBLIC = "--- BEGIN PUBLIC KEY ---"
const val END_PUBLIC = "--- END PUBLIC KEY ---"
const val START_PRIVATE = "--- BEGIN PRIVATE KEY ---"
const val END_PRIVATE = "--- END PRIVATE KEY ---"


class Person private constructor(
                  private val keyPair: KeyPair) {

    val logger = KotlinLogging.logger {  }

    val publicKey : PublicKey
        get() = keyPair.public ?: error("Current Person implementation doesn't support functions that use the public key")
    private val privateKey : PrivateKey
        get() = keyPair.private ?: error("Current Person implementation doesn't support functions that use the private key")


    fun getPrivateKeyy():PrivateKey{
        return privateKey
    }

    fun isValid() = fromPrivateKey(privateKey).publicKey == publicKey

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

        /** This will encryptRSA over 245 bytes */
        fun encryptAES(data :ByteArray,person :Person):EncryptedBytes = encryptAES(data,person.publicKey)

        /** This will encryptRSA over 245 bytes */
        fun encryptAES(data :ByteArray, publicKey : PublicKey): EncryptedBytes {
            val iv = ByteArray(16) { -1 }
            SecureRandom.getInstanceStrong().nextBytes(iv)

            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(128)
            val secretKey = keyGen.generateKey()


            val ivParameterSpec = IvParameterSpec(iv)
            val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

            val final = aesCipher.doFinal(data)
            return EncryptedBytes(iv, encryptRSA(secretKey.encoded,publicKey), final)
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
        fun encryptRSA(data :ByteArray, publicKey: PublicKey): ByteArray {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(data)
        }

        /** This needs to be below 245 bytes */
        fun encryptRSA(data :ByteArray, person :Person):ByteArray = encryptRSA(data,person.publicKey)


        fun deserialize(string :String):Person{
            var private :PrivateKey? = null
            var public :PublicKey? = null
            if(string.contains(START_PRIVATE)){
//                println("finding private")
                var str = string
                    .substring(string.indexOf(START_PRIVATE))
                    .replaceFirst(START_PRIVATE,"")
                str = str.substring(0,str.indexOf(END_PRIVATE))
                    .replace("\n","")
                    .trim()
                val encoded= Base64.decodeBase64(str)
                val spec = PKCS8EncodedKeySpec(encoded)
                private = KeyFactory.getInstance("RSA").generatePrivate(spec)
            }
            if(string.contains(START_PUBLIC)){
//                println("finding public")
                var str = string
                    .substring(string.indexOf(START_PUBLIC))
                    .replaceFirst(START_PUBLIC,"")
                str = str.substring(0,str.indexOf(END_PUBLIC))
                    .replace("\n","")
                    .trim()
                val encoded = Base64.decodeBase64(str)
                val spec = X509EncodedKeySpec(encoded)
                public = KeyFactory.getInstance("RSA").generatePublic(spec)
            }
            if(public == null && private != null)
                return fromPrivateKey(private)
            return fromKeyPair(KeyPair(public,private))
        }
        val default by lazy { deterministicFromString("asdfklujgahsdk tfygakuyfg taysjmd cgbrtjfaeuwyrctg ayesfgvr jayewuv rcfjuaywekv ftbruckasefvg uj ebrstyjuwesbct vfrhyasdgvf anhysertcfgyasdjya") }


        //generators
        fun fromKeyPair(keyPair: KeyPair):Person = Person(keyPair)
        fun fromPublicKey(publicKey: PublicKey):Person = Person(KeyPair(publicKey,null))
        fun fromPrivateKey(privateKey: PrivateKey):Person {
            //attempt to find the correct public key
            if(privateKey !is RSAPrivateCrtKey)
                error("Private key is not a RSAPrivateCrtKey and does not contain enough data to compute the public key")
            val spec = RSAPublicKeySpec(privateKey.modulus,privateKey.publicExponent)
            val factory = KeyFactory.getInstance("RSA")
            val publicKey = factory.generatePublic(spec)
            return Person(KeyPair(publicKey,privateKey))
        }
        fun deterministicFromString(string :String) :Person = Person(buildKeyPair(DigestUtils.sha1(string)!!.contentHashCode().toLong()))
        fun generateNew() :Person = Person(buildKeyPair())
    }

    override fun toString(): String {
        return "Person(finger=${fingerprint().substring(0,10)})"
    }


    fun sign(data :ByteArray): Signature {
        sig.initSign(privateKey)
        sig.update(data)
        return Signature(sig.sign())
    }

    fun fingerprint():String = DigestUtils.sha1Hex(publicKey.encoded)

    /** This needs to be below 245 bytes */
    fun decryptRSA(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        try {
            return cipher.doFinal(encrypted)
        }catch(e : BadPaddingException){
//            if(e.message == "Decryption error"){
                val new = IllegalAccessException("Unable to decryptRSA data with given key")
                new.addSuppressed(e)
                throw new
//            }
//            throw e
        }
    }

    fun decryptAESAndTestDefaultKey(data :EncryptedBytes):ByteArray =
        try{
            decryptAES(data)
        }catch(e :IllegalAccessException){
            try {
                Person.default.decryptAES(data)
            }catch(e :IllegalAccessException){
                throw IllegalStateException("Both keys failed to decrypt",e)
            }
        }


    fun decryptAES(data : EncryptedBytes) :ByteArray{
        val iv = data.iv
        val ivParameterSpec = IvParameterSpec(iv)

        val decryptedSecretKey = decryptRSA(data.encryptedSecretKey)

        val secretKey = SecretKeySpec(decryptedSecretKey, 0, decryptedSecretKey.size, "AES")


        val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return aesCipher.doFinal(data.encryptedData)
    }

    fun serialize():String{
        fun addNewLines(s :String):String{
            val new = StringBuilder()
            var i = 0
            for(char in s){
                new.append(char)
                if(++i % 80 == 0)
                    new.append('\n')
            }
            return new.toString()
        }
        var s = ""
        if(keyPair.private != null){
            s += "$START_PRIVATE\n"
            s += addNewLines(Base64.encodeBase64String(privateKey.encoded)) + "\n"
            s += "$END_PRIVATE\n"
        }
        if(keyPair.public != null){
            s += "$START_PUBLIC\n"
            s += addNewLines(Base64.encodeBase64String(publicKey.encoded)) + "\n"
            s += "$END_PUBLIC\n"
        }
        return s
    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person

        if(keyPair.private != other.keyPair.private)return false
        if(keyPair.public != other.keyPair.public)return false


        return true
    }

    override fun hashCode(): Int {
        return keyPair.hashCode()
    }

    fun justPublic() = fromPublicKey(publicKey)


}
