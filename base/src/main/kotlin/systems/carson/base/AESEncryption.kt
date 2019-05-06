package systems.carson.base

import org.apache.commons.codec.binary.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedAESBytes(
    val bytes :ByteArray,
    val iv :ByteArray
){
    fun toStrings():EncryptedAESStrings{
        return EncryptedAESStrings(
            Base64.encodeBase64String(bytes),
            Base64.encodeBase64String(iv))
    }
}
data class EncryptedAESStrings(
    val bytes :String,
    val iv :String
){
    fun toBytes():EncryptedAESBytes{
        return EncryptedAESBytes(
            Base64.decodeBase64(bytes),
            Base64.decodeBase64(iv))
    }
}


object AESEncryption{

    const val keySize = 16
//    const val AES_CIPHER = "AES/CTR/PKCS5Padding"


    fun decryptAES(data: EncryptedAESBytes, secretKey :ByteArray): ByteArray {
        val paddedSecretKey = secretKey + ByteArray(keySize - secretKey.size) { 0x0 }
        val iv = data.iv
        val ivParameterSpec = IvParameterSpec(iv)

        val keySpec = SecretKeySpec(paddedSecretKey, 0, keySize, "AES")

        val aesCipher = Cipher.getInstance(Person.AES_CIPHER)
        aesCipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)

        return aesCipher.doFinal(data.bytes)
    }
    fun encryptAES(data :ByteArray, secretKey: ByteArray):EncryptedAESBytes {
        val paddedSecretKey = secretKey + ByteArray(keySize - secretKey.size) { 0x0 }
        val iv = ByteArray(16)
        SecureRandom.getInstanceStrong().nextBytes(iv)
        val keySpec = SecretKeySpec(paddedSecretKey, 0, keySize, "AES")



        val ivParameterSpec = IvParameterSpec(iv)
        val aesCipher = Cipher.getInstance(Person.AES_CIPHER)
        aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)

        val final = aesCipher.doFinal(data)
        return EncryptedAESBytes(
            bytes = final,
            iv = iv)
    }
}
