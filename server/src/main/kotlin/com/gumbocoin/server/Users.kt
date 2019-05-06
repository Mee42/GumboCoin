package com.gumbocoin.server

import org.apache.commons.codec.binary.Hex
import org.bson.Document
import org.bson.json.JsonWriterSettings
import systems.carson.base.deserialize
import systems.carson.base.serialize
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class ServerUser(
    val clientID :String,
    val keyfile :String,
    val salt :String,
    val hash :String)

private fun ByteArray.toStringHexValue() = Hex.encodeHexString(this)
private fun String.toByteArrayHexValue() = Hex.decodeHex(this)

inline fun <reified T> deserialize(doc : Document): T {
    val settings = JsonWriterSettings.builder()
        .int64Converter { value, writer -> writer.writeNumber(value!!.toString()) }
        .build()
    return deserialize(doc.toJson(settings))
}
fun <T :Any> serializeToDocument(item :T):Document{
    return Document.parse(serialize(item))
}
fun passwordHash(password :String):Pair<String,String>{
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return Pair(passwordHash(password,salt),salt.toStringHexValue())
}

fun passwordHash(password: String, salt: ByteArray):String{
    val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 128)
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val hash = f.generateSecret(spec).encoded
    return hash.toStringHexValue()
}
fun passwordHash(password: String, salt: String):String{
    return passwordHash(password,salt.toByteArrayHexValue())
}
