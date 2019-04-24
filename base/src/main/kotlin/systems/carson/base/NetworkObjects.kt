package systems.carson.base

import io.rsocket.Payload
import io.rsocket.util.DefaultPayload


const val PORT = 48626

class Request{
    enum class Response(val intent :String){
        PING("ping"),
        DECRYPT("decrypt"),
        VERIFIED("verified"),
        SIGN_UP("sign_up"),
        BLOCK("block")
    }
    enum class Stream(val intent :String){
        NUMBERS("numbers"),
        BLOCKCHAIN_UPDATES("blockchain_updates")
    }
}

open class RequestDataBlob(
    val intent :String,
    val clientID :String,
    val type :RequestDataBlobType = RequestDataBlobType.NORMAL,
    @Transient var isVerified :Boolean = false) :Sendable{
    constructor(intent :Request.Response, clientID :String, type :RequestDataBlobType = RequestDataBlobType.NORMAL):this(intent.intent,clientID,type)
    constructor(intent :Request.Stream, clientID :String, type :RequestDataBlobType = RequestDataBlobType.NORMAL):this(intent.intent,clientID,type)
}


enum class RequestDataBlobType{
    NORMAL,
    SIGN_UP_DATA,
    ENCRYPTED_DATA,
    BLOCK_DATA,
    STRING_DATA,
    INT_DATA
}

class Status(
    val failed :Boolean = false,
    val errorMessage :String = "",
    val extraData :String = "") :Sendable

class ActionUpdate(val actions :List<Action>,
                   val lasthash :String,
                   val difficulty :Long) :Sendable

interface Sendable{
    fun send():String{
        return serialize(this)
    }
    fun toPayload():Payload{
        return DefaultPayload.create(send())
    }
    companion object
}


fun String.trimAESPadding():String{
    var i = this.lastIndex
    while(i - 1 < length && this[i - 1] == (0).toChar())
        i--
    return this.substring(0,i)
}

inline fun <reified T :Sendable> Sendable.Companion.fromJson(str :String):T = deserialize(str.trimAESPadding())

class SendableBoolean(val value :Boolean) :Sendable
class SendableString(val value :String) :Sendable
class SendableInt(val value :Int) :Sendable



class SignUpDataBlob(clientID :String,val signUpAction: SignUpAction) :RequestDataBlob(intent = Request.Response.SIGN_UP.intent,clientID = clientID, type = RequestDataBlobType.SIGN_UP_DATA){
    constructor(clientID: String, publicKey :String):this(clientID, SignUpAction(clientID,publicKey))
}
//class StringDataBlob(clientID :String, val value :String, request :Request.Response) :RequestDataBlob(request,clientID = clientID)
class EncryptedDataBlob(clientID :String, val data :EncryptedString, intent :String) :RequestDataBlob(intent,clientID,RequestDataBlobType.ENCRYPTED_DATA)
class BlockDataBlob(clientID: String, val block :Block, intent :String) :RequestDataBlob(intent, clientID,RequestDataBlobType.BLOCK_DATA)
class StringDataBlob(clientID :String, val value :String, intent :String) :RequestDataBlob(intent,clientID,RequestDataBlobType.STRING_DATA)
class IntDataBlob(clientID :String, val value :Int, intent :String) :RequestDataBlob(intent,clientID,RequestDataBlobType.INT_DATA)


open class Action(val type: ActionType){
    companion object
}

class SignUpAction(val clientID :String,val publicKey :String) : Action(ActionType.SIGN_UP)

enum class ActionType { SIGN_UP }


class Message(
    val encryptedData: EncryptedString,
    val clientID: String,
    val signature :String//base64 of Signature
)
