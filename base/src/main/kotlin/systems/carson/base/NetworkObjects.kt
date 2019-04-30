package systems.carson.base

import io.rsocket.Payload
import io.rsocket.util.DefaultPayload
import org.apache.commons.codec.digest.DigestUtils
import java.nio.charset.Charset
import java.security.PublicKey
import java.util.*


const val PORT = 48626

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


enum class RequestDataBlobType {
    NORMAL,
    SIGN_UP_DATA,
    ENCRYPTED_DATA,
    BLOCK_DATA,
    STRING_DATA,
    INT_DATA,
    TRANSACTION,
    DATA_SUBMIT
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

    private fun toSingableString(): String {
        return clientID + data.toString()
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

enum class ActionType { SIGN_UP, TRANSACTION, DATA }


class Message(
    val encryptedData: EncryptedString,
    val clientID: String,
    val signature: String//base64 of Signature
)
