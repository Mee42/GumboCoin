package systems.carson.base

import com.google.gson.*
import java.lang.reflect.Type



fun serialize(obj :Any) :String{
    return GsonHolder.serializingGson.toJson(obj)
}

fun prettyPrint(obj :Any) :String {
    return GsonHolder.prettySerializingGson.toJson(obj)
}

inline fun <reified T> deserialize(string :String) :T{
    return GsonHolder.deserializingGson.fromJson(string,T::class.java)
}


object GsonHolder{
    val serializingGson :Gson by lazy { serializingGsonProducer.invoke().create() }
    val prettySerializingGson :Gson by lazy { serializingGsonProducer.invoke().setPrettyPrinting().create() }

    val deserializingGson:Gson by lazy { deserializingGsonProducer.invoke().create() }



    private val serializingGsonProducer :() -> GsonBuilder = { GsonBuilder() }
    private val deserializingGsonProducer :() -> GsonBuilder = { GsonBuilder()
        .registerTypeAdapter(Action::class.java,object : JsonDeserializer<Action> {
            override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): Action? {
                val obj = json.asJsonObject

                fun str(s :String):String = obj.getAsJsonPrimitive(s).asString
                fun int(s :String):Int = obj.getAsJsonPrimitive(s).asInt

                val clientID :String = obj.getAsJsonPrimitive("clientID").asString

                return when(ActionType.valueOf((obj.get("type").asString))) {
                    ActionType.SIGN_UP -> {
                        SignUpAction(
                            clientID = clientID,
                            publicKey = str("publicKey")
                        )
                    }
                    ActionType.TRANSACTION -> TransactionAction(
                        clientID = clientID,
                        recipientID = str("recipientID"),
                        amount = int("amount"),
                        signature = str("signature"))
                    ActionType.DATA -> DataAction(
                        clientID = clientID,
                        data = context.deserialize(obj.getAsJsonObject("data"),DataPair::class.java),
                        signature = str("signature")
                    )
                }
            }
        })
        .registerTypeAdapter(RequestDataBlob::class.java,object : JsonDeserializer<RequestDataBlob> {

            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): RequestDataBlob? {
                if(json == null)
                    return null
                val obj = json.asJsonObject

                fun str(s :String):String = obj.getAsJsonPrimitive(s).asString
                fun int(s :String):Int = obj.getAsJsonPrimitive(s).asInt

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
                        value = str("value"),
                        intent = intent
                    )
                    RequestDataBlobType.INT_DATA -> IntDataBlob(
                        clientID = clientID,
                        value = int("value"),
                        intent = intent
                    )
                    RequestDataBlobType.NORMAL -> RequestDataBlob(
                        clientID = clientID,
                        intent = intent
                    )
                    RequestDataBlobType.TRANSACTION -> TransactionDataBlob(
                        clientID = clientID,
                        transactionAction = context.deserialize(obj.getAsJsonObject("transactionAction"),TransactionAction::class.java)
                    )
                    RequestDataBlobType.DATA_SUBMIT -> DataSubmissionDataBlob(
                        clientID = clientID,
                        action = context.deserialize(obj.getAsJsonObject("action"),DataAction::class.java)
                    )

                }
            }
        })
    }

}
