package com.gumbocoin.base

actual fun serialize(obj :Any):String{
    return JSON.stringify(obj)
}
actual fun prettyPrint(obj :Any):String{
    return JSON.stringify(obj,null,2)
}

infix fun String.str(dyn :dynamic) = dyn[this].unsafeCast<String>()
infix fun String.int(dyn :dynamic) = dyn[this].unsafeCast<Int>()





actual inline fun <reified T> rawDeserialize(json :String):T {
    return JSON.parse(json)
}
actual inline fun <reified T :Sendable> deserialize(json :String):T{
    val data = JSON.parse<T>(json)
    return when(data) {
        is Action -> {
            val dyn = JSON.parse<dynamic>(json)
            //need to parse better
            when(data.type){
                ActionType.SIGN_UP -> SignUpAction(
                    clientID = "clientID" str dyn,
                    publicKey = "publicKey" str dyn
                ) as T
                ActionType.TRANSACTION -> TransactionAction(
                    clientID = "clientID" str dyn,
                    recipientID = "recipientID" str dyn,
                    amount = "amount" int dyn,
                    signature = "signature" str dyn
                ) as T
                ActionType.DATA -> DataAction(
                    clientID = "clientID" str dyn,
                    data = dyn.data.unsafeCast<DataPair>(),
                    signature = "signature" str dyn
                ) as T
                ActionType.VERIFY -> VerifyAction(
                    clientID = "clientID" str dyn,
                    dataID = "dataID" str dyn,
                    signature = "signature" str dyn
                )as T
            }
        }
        else -> error("Can't deserialize ${T::class}")
    }
}

//https://github.com/google/closure-library/blob/8598d87242af59aac233270742c8984e2b2bdbe0/closure/goog/crypt/crypt.js#L117-L143
actual fun String.utfToByteArray():ByteArray{
    val func = js("function(str) {\n" +
            "  // TODO(user): Use native implementations if/when available\n" +
            "  var out = [], p = 0;\n" +
            "  for (var i = 0; i < str.length; i++) {\n" +
            "    var c = str.charCodeAt(i);\n" +
            "    if (c < 128) {\n" +
            "      out[p++] = c;\n" +
            "    } else if (c < 2048) {\n" +
            "      out[p++] = (c >> 6) | 192;\n" +
            "      out[p++] = (c & 63) | 128;\n" +
            "    } else if (\n" +
            "        ((c & 0xFC00) == 0xD800) && (i + 1) < str.length &&\n" +
            "        ((str.charCodeAt(i + 1) & 0xFC00) == 0xDC00)) {\n" +
            "      // Surrogate Pair\n" +
            "      c = 0x10000 + ((c & 0x03FF) << 10) + (str.charCodeAt(++i) & 0x03FF);\n" +
            "      out[p++] = (c >> 18) | 240;\n" +
            "      out[p++] = ((c >> 12) & 63) | 128;\n" +
            "      out[p++] = ((c >> 6) & 63) | 128;\n" +
            "      out[p++] = (c & 63) | 128;\n" +
            "    } else {\n" +
            "      out[p++] = (c >> 12) | 224;\n" +
            "      out[p++] = ((c >> 6) & 63) | 128;\n" +
            "      out[p++] = (c & 63) | 128;\n" +
            "    }\n" +
            "  }\n" +
            "  return out;\n" +
            "};")
    return func(this) as ByteArray
}
actual fun ByteArray.utfToString():String{
    val func = js("" +
            "function(bytes) {\n" +
            "  // TODO(user): Use native implementations if/when available\n" +
            "  var out = [], pos = 0, c = 0;\n" +
            "  while (pos < bytes.length) {\n" +
            "    var c1 = bytes[pos++];\n" +
            "    if (c1 < 128) {\n" +
            "      out[c++] = String.fromCharCode(c1);\n" +
            "    } else if (c1 > 191 && c1 < 224) {\n" +
            "      var c2 = bytes[pos++];\n" +
            "      out[c++] = String.fromCharCode((c1 & 31) << 6 | c2 & 63);\n" +
            "    } else if (c1 > 239 && c1 < 365) {\n" +
            "      // Surrogate Pair\n" +
            "      var c2 = bytes[pos++];\n" +
            "      var c3 = bytes[pos++];\n" +
            "      var c4 = bytes[pos++];\n" +
            "      var u = ((c1 & 7) << 18 | (c2 & 63) << 12 | (c3 & 63) << 6 | c4 & 63) -\n" +
            "          0x10000;\n" +
            "      out[c++] = String.fromCharCode(0xD800 + (u >> 10));\n" +
            "      out[c++] = String.fromCharCode(0xDC00 + (u & 1023));\n" +
            "    } else {\n" +
            "      var c2 = bytes[pos++];\n" +
            "      var c3 = bytes[pos++];\n" +
            "      out[c++] =\n" +
            "          String.fromCharCode((c1 & 15) << 12 | (c2 & 63) << 6 | c3 & 63);\n" +
            "    }\n" +
            "  }\n" +
            "  return out.join('');\n" +
            "};")
    return func(this) as String
}



actual fun sha256(bytes: ByteArray):ByteArray{
    val hashBuffer = js("crypto.subtle.digest('SHA-256', msgBuffer)")
    return hashBuffer as ByteArray
}
