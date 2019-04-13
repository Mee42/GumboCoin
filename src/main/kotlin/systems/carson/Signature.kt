package systems.carson

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
}

class EncryptedData(val iv :ByteArray, val encryptedSecretKey :ByteArray,val encryptedData :ByteArray)