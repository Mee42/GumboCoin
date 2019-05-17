package com.gumbocoin.base

import org.apache.commons.codec.digest.DigestUtils
import java.nio.charset.Charset


actual fun sha256(bytes :ByteArray): ByteArray = DigestUtils.sha256(bytes)


actual fun String.utfToByteArray():ByteArray{
    return this.toByteArray(Charset.forName("UTF-8"))
}
actual fun ByteArray.utfToString():String{
    return this.toString(Charset.forName("UTF-8"))
}