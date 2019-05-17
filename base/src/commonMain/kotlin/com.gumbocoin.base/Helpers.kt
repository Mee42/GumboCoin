package com.gumbocoin.base

expect fun serialize(obj :Any):String
expect fun prettyPrint(obj :Any):String
expect inline fun <reified T :Sendable> deserialize(json :String):T
expect inline fun <reified T> rawDeserialize(json :String):T


expect fun sha256(bytes: ByteArray):ByteArray

expect fun String.utfToByteArray():ByteArray
expect fun ByteArray.utfToString():String


//expect fun bytesToHex(byteArray: ByteArray):String
//expect fun hexToBytes(hex :String):String

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }
fun String.hexToByteArray() = this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()