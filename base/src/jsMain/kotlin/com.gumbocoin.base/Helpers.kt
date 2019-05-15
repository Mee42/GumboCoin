package com.gumbocoin.base

actual fun serialize(obj :Any):String{
    return JSON.stringify(obj)
}
actual fun prettyPrint(obj :Any):String{
    return JSON.stringify(obj,null,2)
}

infix fun String.str(dyn :dynamic) = dyn[this].unsafeCast<String>()
infix fun String.int(dyn :dynamic) = dyn[this].unsafeCast<Int>()

actual inline fun <reified T> deserialize(json :String):T{
    val data = JSON.parse<T>(json)
    if(data is Action){
        val dyn = JSON.parse<dynamic>(json)
        //need to parse better
        when(data.type){
            ActionType.SIGN_UP -> SignUpAction(
                clientID = "clientID" str dyn,
                publicKey = "publicKey" str dyn
            )
            ActionType.TRANSACTION -> TransactionAction(
                clientID = "clientID" str dyn,
                recipientID = "recipientID" str dyn,
                amount = "amount" int dyn,
                signature = "signature" str dyn
            )
            ActionType.DATA -> DataAction(
                clientID = "clientID" str dyn,
                data = dyn.data.unsafeCast<DataPair>(),
                signature = "signature" str dyn
            )
            ActionType.VERIFY -> VerifyAction(
                clientID = "clientID" str dyn,
                dataID = "dataID" str dyn,
                signature = "signature" str dyn
            )
        }
    }
    return data
}


actual fun String.utfToByteArray():ByteArray{
    val str = this
    val utf8 = js("unescape(encodeURIComponent(str))")
    val arr = mutableListOf<Byte>()
    for (i in 0 until utf8.length.unsafeCast<Int>()) {
        arr.add(utf8.charCodeAt(i).unsafeCast<Byte>())
    }
    return arr.toByteArray()
}
actual fun ByteArray.utfToString():String{

}



actual fun sha256(bytes: ByteArray):ByteArray{
    TODO()
}
