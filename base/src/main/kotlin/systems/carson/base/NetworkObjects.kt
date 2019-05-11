package systems.carson.base

import io.rsocket.Payload
import io.rsocket.util.DefaultPayload
import org.apache.commons.codec.digest.DigestUtils
import java.nio.charset.Charset
import java.security.PublicKey
import java.util.*
import kotlin.reflect.KClass


val PORT = mapOf(
    Release.MASTER to 48625,
    Release.BETA to 48626,
    Release.DEV to 48627)

open class RequestDataBlob(
    val intent: String,
    val clientID: String,
    val type: RequestDataBlobType = RequestDataBlobType.NORMAL,
    @Transient var isVerified: Boolean = false
) : Sendable {
    constructor(
        intent: Request.Response,
        clientID: String,
        type: RequestDataBlobType = RequestDataBlobType.NORMAL
    ) : this(intent.intent, clientID, type)

    constructor(
        intent: Request.Stream,
        clientID: String,
        type: RequestDataBlobType = RequestDataBlobType.NORMAL
    ) : this(intent.intent, clientID, type)
}


class Status(
    val failed: Boolean = false,
    val errorMessage: String = "",
    val extraData: String = ""
) : Sendable

class ActionUpdate(
    val actions: List<Action>,
    val lasthash: String,
    val difficulty: Long
) : Sendable

interface Sendable {
    fun send(): String {
        return serialize(this)
    }

    fun toPayload(): Payload {
        return DefaultPayload.create(send())
    }

    companion object
}


enum class RequestDataBlobType(val clazz :Class<out RequestDataBlob>) {
    NORMAL(RequestDataBlob::class),
    SIGN_UP_DATA(SignUpDataBlob::class),
    ENCRYPTED_DATA(EncryptedDataBlob::class),
    BLOCK_DATA(BlockDataBlob::class),
    STRING_DATA(StringDataBlob::class),
    INT_DATA(IntDataBlob::class),
    TRANSACTION(TransactionDataBlob::class),
    DATA_SUBMIT(DataSubmissionDataBlob::class),
    VERIFY_DATA(VerifyActionBlob::class),
    SUBMIT_KEY_FILE(SubmitKeyFileDataBlob::class);

    constructor(clazzz: KClass<out RequestDataBlob>):this(clazzz.java)
}

fun String.trimAESPadding(): String {
    var i = this.lastIndex
    while (i - 1 < length && this[i - 1] == (0).toChar())
        i--
    return this.substring(0, i)
}

inline fun <reified T : Sendable> Sendable.Companion.fromJson(str: String): T = deserialize(str.trimAESPadding())

class SendableBoolean(val value: Boolean) : Sendable
class SendableString(val value: String) : Sendable
class SendableInt(val value: Int) : Sendable


class SignUpDataBlob(clientID: String, val signUpAction: SignUpAction) : RequestDataBlob(
    intent = Request.Response.SIGN_UP.intent,
    clientID = clientID,
    type = RequestDataBlobType.SIGN_UP_DATA
) {
    constructor(clientID: String, publicKey: String) : this(clientID, SignUpAction(clientID, publicKey))
}

//class StringDataBlob(clientID :String, val value :String, request :Request.Response) :RequestDataBlob(request,clientID = clientID)
class EncryptedDataBlob(clientID: String, val data: EncryptedString, intent: String) :
    RequestDataBlob(intent, clientID, RequestDataBlobType.ENCRYPTED_DATA)

class BlockDataBlob(clientID: String, val block: Block, intent: String) :
    RequestDataBlob(intent, clientID, RequestDataBlobType.BLOCK_DATA)

class StringDataBlob(clientID: String, val value: String, intent: String) :
    RequestDataBlob(intent, clientID, RequestDataBlobType.STRING_DATA)

class SubmitKeyFileDataBlob(
    clientID: String,
    val key :String,
    val password :String,
    intent :String
) :RequestDataBlob(intent,clientID,RequestDataBlobType.SUBMIT_KEY_FILE)

class IntDataBlob(clientID: String, val value: Int, intent: String) :
    RequestDataBlob(intent, clientID, RequestDataBlobType.INT_DATA)

class TransactionDataBlob(clientID: String, val transactionAction: TransactionAction) :
    RequestDataBlob(Request.Response.TRANSACTION, clientID, RequestDataBlobType.TRANSACTION)

class DataSubmissionDataBlob(clientID: String, val action: DataAction) :
    RequestDataBlob(Request.Response.DATA, clientID, RequestDataBlobType.DATA_SUBMIT)

open class Action(val type: ActionType) {
    companion object
}

data class SignUpAction(val clientID: String, val publicKey: String) : Action(ActionType.SIGN_UP)
data class TransactionAction(val clientID: String, val recipientID: String, val amount: Int, val signature: String) :
    Action(ActionType.TRANSACTION) {
    fun isSignatureValid(publicKey: PublicKey): Boolean {
        return Person.verify(
            publicKey,
            Signature.fromBase64(signature),
            (clientID + recipientID + amount).toByteArray(Charset.forName("UTF-8"))
        )
    }

    companion object {
        fun sign(clientID: String, recipientID: String, amount: Int, person: Person): TransactionAction {
            return TransactionAction(
                clientID = clientID,
                recipientID = recipientID,
                amount = amount,
                signature = person.sign((clientID + recipientID + amount).toByteArray(Charset.forName("UTF-8"))).toBase64()
            )
        }
    }
}

data class VerifyAction(val clientID :String,
                        val dataID :String,
                        val signature :String) :Action(ActionType.VERIFY)

class VerifyActionBlob(clientID :String, val action :VerifyAction) :RequestDataBlob(
    Request.Response.VERIFY.intent,
    clientID,
    type = RequestDataBlobType.VERIFY_DATA
)


data class DataPair(
    val key: String,
    val value: String,
    val uniqueID: String = DigestUtils.sha256Hex(UUID.randomUUID().toString()).substring(0, 10)
)

data class DataAction(
    val clientID: String,
    val data: DataPair,
    val signature: String
) : Action(ActionType.DATA) {

    fun toSingableString(): String {
        return clientID + serialize(data)
    }

    fun isSignatureValid(publicKey: PublicKey): Boolean {
        return Person.verify(
            publicKey,
            Signature.fromBase64(signature),
            this.toSingableString().toByteArray(Charset.forName("UTF-8"))
        )
    }
    companion object {
        fun sign(clientID: String, data: DataPair, person: Person): DataAction {
            return DataAction(
                clientID = clientID,
                data = data,
                signature = person.sign(
                    DataAction(
                        clientID = clientID,
                        data = data,
                        signature = ""
                    ).toSingableString().toByteArray(Charset.forName("UTF-8"))
                ).toBase64()
            )
        }
    }
}

enum class ActionType(val clazz :Class<out Action>) {
    SIGN_UP(SignUpAction::class),
    TRANSACTION(TransactionAction::class),
    DATA(DataAction::class),
    VERIFY(VerifyAction::class);
    constructor(clazzz : KClass<out Action>):this(clazzz.java)
}


class Message(
    val encryptedData: EncryptedString,
    val clientID: String,
    val signature: String//base64 of Signature
)
