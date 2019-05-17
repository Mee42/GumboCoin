package com.gumbocoin.base

import com.google.gson.*
import java.lang.reflect.Type


actual fun serialize(obj: Any): String {
    return GsonHolder.def.toJson(obj)
}

actual fun prettyPrint(obj: Any): String {
    return GsonHolder.prettySerializingGson.toJson(obj)
}
actual inline fun <reified T> rawDeserialize(json :String):T{
    return GsonHolder.def.fromJson(json,T::class.java)
}

actual inline fun <reified T :Sendable> deserialize(json: String): T {
    return GsonHolder.deserializingGson.fromJson(json, T::class.java)
}


private inline fun <reified T> JsonObject.obj(name :String, context :JsonDeserializationContext) :T =
    context.deserialize(getAsJsonObject(name),T::class.java)

fun JsonObject.str(s: String): String = getAsJsonPrimitive(s).asString
fun JsonObject.int(s: String): Int = getAsJsonPrimitive(s).asInt


object GsonHolder {
    val def: Gson by lazy { defProducer.invoke().create() }
    val prettySerializingGson: Gson by lazy { defProducer.invoke().setPrettyPrinting().create() }

    val deserializingGson: Gson by lazy { deserializingGsonProducer.invoke().create() }


    private val defProducer: () -> GsonBuilder = { GsonBuilder() }
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
    }

}
