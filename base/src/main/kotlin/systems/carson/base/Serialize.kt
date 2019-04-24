package systems.carson.base

import com.google.gson.*
import java.lang.reflect.Type



fun serialize(obj :Any) :String{
    return GsonHolder.serializingGson.toJson(obj)
}

inline fun <reified T> deserialize(string :String) :T{
    return GsonHolder.deserializingGson.fromJson(string,T::class.java)
}

object GsonHolder{
    val serializingGson :Gson = Gson()
    val deserializingGson :Gson = GsonBuilder()
        .registerTypeAdapter(Action::class.java,object : JsonDeserializer<Action> {
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
        .registerTypeAdapter(RequestDataBlob::class.java,object : JsonDeserializer<RequestDataBlob> {

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

}
