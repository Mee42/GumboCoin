package systems.carson.base

import org.apache.commons.codec.binary.Base64

class Signature(val byteArray: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }
    fun toBase64() :String = Base64.encodeBase64String(byteArray)
    companion object{
        fun fromBase64(string :String):Signature = Signature(Base64.decodeBase64(string))
    }
}

class EncryptedBytes(val iv :ByteArray, val encryptedSecretKey :ByteArray,val encryptedData :ByteArray){
    fun toStrings():EncryptedString{
        return EncryptedString(
            iv = Base64.encodeBase64String(iv),
            encryptedSecretKey = Base64.encodeBase64String(encryptedSecretKey),
            encryptedData = Base64.encodeBase64String(encryptedData)
        )
    }
    fun concat():ByteArray{
        return iv + encryptedSecretKey + encryptedData
    }
}
class EncryptedString(val iv :String, val encryptedSecretKey :String, val encryptedData :String) :Sendable{
    fun toBytes():EncryptedBytes{
        return EncryptedBytes(
            iv = Base64.decodeBase64(iv),
            encryptedSecretKey = Base64.decodeBase64(encryptedSecretKey),
            encryptedData = Base64.decodeBase64(encryptedData)
        )
    }
}
