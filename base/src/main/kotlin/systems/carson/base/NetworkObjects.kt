package systems.carson.base

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.codec.binary.Base64
import java.util.*


const val PORT = 48626

val gson : Gson = GsonBuilder().create()


class Request{

    enum class Response(val intent :String){
        PING("ping"),
        DECRYPT("decrypt"),
        VERIFIED("verified")//TODO("SHould return like a boolean or soemthing if the sig is present and works")
    }
    enum class Stream(val intent :String){
        NUMBERS("numbers")
    }
    enum class Fire(val intent :String){

    }
}

class DataBlob(
    val intent :String,
    val data :String)

class RequestDataBlob(
    val intent : Request.Response,
    val clientID :String,
    val data :ReceivedData,
    val isVerified :Boolean)

class StreamDataBlob(
    val intent :Request.Stream,
    val clientID :String,
    val data :ReceivedData,
    val isVerified :Boolean)

class FireDataBlob(
    val intent :Request.Fire,
    val clientID :String,
    val data :ReceivedData,
    val isVerified :Boolean)

class ReceivedData(val data :String){
    fun base64() :ByteArray{
        return Base64.decodeBase64(data)
    }
    inline fun <reified T :Sendable> fromJson() :T{
        return gson.fromJson(data,T::class.java)
    }
}

class Message(
    val encryptedData: EncryptedString,
    val clientID: String,
    val signature :String//base64 of Signature
)



interface Sendable{
    fun send() :String{
        return gson.toJson(this)
    }
}

class SendableInteger(val value: Int):Sendable
class SendableString(val value :String):Sendable
object NoData :Sendable{
    override fun send() = ""
}

//class SendString(val value :String) :Sendable{
//    override fun send(): String {
//        return value
//    }
//}

