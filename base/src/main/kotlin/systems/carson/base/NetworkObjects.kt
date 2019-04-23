package systems.carson.base

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.sun.jmx.remote.internal.ClientListenerInfo
import io.rsocket.Payload
import io.rsocket.util.DefaultPayload
import org.apache.commons.codec.binary.Base64
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*


const val PORT = 48626

val gson :Gson = GsonBuilder()
    .registerTypeAdapter(Action::class.java,object :JsonDeserializer<Action> {
        override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): Action? {
            val obj = json.asJsonObject

            return when(ActionType.valueOf((obj.get("type").asString))) {
                ActionType.SIGN_UP -> {
                    SignUpAction(
                        clientID = obj.getAsJsonPrimitive("clientID").asString,
                        publicKey = obj.getAsJsonPrimitive("publicKey").asString
                    )
                }
            }
        }
    })
    .registerTypeAdapter(RequestDataBlob::class.java,object :JsonDeserializer<RequestDataBlob> {

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): RequestDataBlob? {
            if(json == null)
                return null
            val obj = json.asJsonObject

            val clientID :String = obj.getAsJsonPrimitive("clientID").asString
            val intent :String = obj.getAsJsonPrimitive("intent").asString

            return when(RequestDataBlobType.valueOf(obj.getAsJsonPrimitive("type").asString)){
                RequestDataBlobType.SIGN_UP_DATA -> SignUpDataBlob(
                    clientID = clientID,
                    signUpAction = context.deserialize(obj.getAsJsonObject("signUpAction"), SignUpAction::class.java)
                )
                RequestDataBlobType.ENCRYPTED_DATA -> EncryptedDataBlob(
                    clientID = clientID,
                    data = context.deserialize(obj.getAsJsonObject("data"),EncryptedString::class.java),
                    intent = intent
                )
                RequestDataBlobType.BLOCK_DATA -> BlockDataBlob(
                    clientID = clientID,
                    block = context.deserialize(obj.getAsJsonObject("block"),Block::class.java),
                    intent = intent
                )
                RequestDataBlobType.STRING_DATA -> StringDataBlob(
                    clientID = clientID,
                    value = obj.getAsJsonPrimitive("value").asString,
                    intent = intent
                )
                RequestDataBlobType.INT_DATA -> IntDataBlob(
                    clientID = clientID,
                    value = obj.getAsJsonPrimitive("value").asInt,
                    intent = intent
                )
                RequestDataBlobType.NORMAL -> RequestDataBlob(
                    clientID = clientID,
                    intent = intent)
            }
        }
    })

    .create()


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
        return gson.toJson(this)
    }
    fun toPayload():Payload{
        return DefaultPayload.create(send())
    }
    companion object
}

inline fun <reified T :Sendable> Sendable.Companion.fromJson(str :String):T = gson.fromJson(str,T::class.java)

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
