package systems.carson.base

import com.google.gson.*
import java.lang.reflect.Type


fun serialize(obj: Any): String {
    return GsonHolder.serializingGson.toJson(obj)
}

fun prettyPrint(obj: Any): String {
    return GsonHolder.prettySerializingGson.toJson(obj)
}

inline fun <reified T> deserialize(string: String): T {
    return GsonHolder.deserializingGson.fromJson(string, T::class.java)
}


private inline fun <reified T> JsonObject.obj(name :String, context :JsonDeserializationContext) :T =
    context.deserialize(getAsJsonObject(name),T::class.java)

fun JsonObject.str(s: String): String = getAsJsonPrimitive(s).asString
fun JsonObject.int(s: String): Int = getAsJsonPrimitive(s).asInt

object GsonHolder {
    val serializingGson: Gson by lazy { serializingGsonProducer.invoke().create() }
    val prettySerializingGson: Gson by lazy { serializingGsonProducer.invoke().setPrettyPrinting().create() }

    val deserializingGson: Gson by lazy { deserializingGsonProducer.invoke().create() }


    private val serializingGsonProducer: () -> GsonBuilder = { GsonBuilder() }
    private val deserializingGsonProducer: () -> GsonBuilder = {
        GsonBuilder()
            .registerTypeAdapter(Action::class.java, object : JsonDeserializer<Action> {
                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type?,
                    context: JsonDeserializationContext
                ): Action? {
                    val obj = json.asJsonObject



                    val clientID: String = obj.str("clientID")

                    return when (ActionType.valueOf((obj.get("type").asString))) {
                        ActionType.SIGN_UP -> {
                            SignUpAction(
                                clientID = clientID,
                                publicKey = obj.str("publicKey")
                            )
                        }
                        ActionType.TRANSACTION -> TransactionAction(
                            clientID = clientID,
                            recipientID = obj.str("recipientID"),
                            amount = obj.int("amount"),
                            signature = obj.str("signature")
                        )
                        ActionType.DATA -> DataAction(
                            clientID = clientID,
                            data = obj.obj("data",context),
                            signature = obj.str("signature")
                        )
                        ActionType.VERIFY -> VerifyAction(
                            clientID = clientID,
                            dataID = obj.str("dataID"),
                            signature = obj.str("signature")
                        )
                    }
                }
            })
            .registerTypeAdapter(RequestDataBlob::class.java, object : JsonDeserializer<RequestDataBlob> {

                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext
                ): RequestDataBlob? {
                    if (json == null)
                        return null
                    val obj = json.asJsonObject

                    fun str(s: String): String = obj.getAsJsonPrimitive(s).asString
                    fun int(s: String): Int = obj.getAsJsonPrimitive(s).asInt

                    val clientID: String = obj.getAsJsonPrimitive("clientID").asString
                    val intent: String = obj.getAsJsonPrimitive("intent").asString

                    return when (RequestDataBlobType.valueOf(obj.getAsJsonPrimitive("type").asString)) {
                        RequestDataBlobType.SIGN_UP_DATA -> SignUpDataBlob(
                            clientID = clientID,
                            signUpAction = context.deserialize(
                                obj.getAsJsonObject("signUpAction"),
                                SignUpAction::class.java
                            )
                        )
                        RequestDataBlobType.ENCRYPTED_DATA -> EncryptedDataBlob(
                            clientID = clientID,
                            data = obj.obj("data",context),
                            intent = intent
                        )
                        RequestDataBlobType.BLOCK_DATA -> BlockDataBlob(
                            clientID = clientID,
                            block = obj.obj("block",context),
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
                            transactionAction = obj.obj("transactionAction",context)
                        )
                        RequestDataBlobType.DATA_SUBMIT -> DataSubmissionDataBlob(
                            clientID = clientID,
                            action = obj.obj("action",context)
                        )
                        RequestDataBlobType.VERIFY_DATA -> VerifyActionBlob(
                            clientID = clientID,
                            action = obj.obj("action",context)
                        )
                    }
                }
            })
    }

}
