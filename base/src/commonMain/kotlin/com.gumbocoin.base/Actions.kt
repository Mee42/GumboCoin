package com.gumbocoin.base


enum class ActionType {
    SIGN_UP,
    TRANSACTION,
    DATA,
    VERIFY
}


open class Action(val type: ActionType) {
    companion object
}


data class SignUpAction(val clientID: String, val publicKey: String) : Action(ActionType.SIGN_UP)

data class TransactionAction(val clientID: String, val recipientID: String, val amount: Int, val signature: String) :
    Action(ActionType.TRANSACTION) {
//    fun isSignatureValid(publicKey: PublicKey): Boolean {
//        return Person.verify(
//            publicKey,
//            Signature.fromBase64(signature),
//            (clientID + recipientID + amount).toByteArray(Charset.forName("UTF-8"))
//        )
//    }
//
//    companion object {
//        fun sign(clientID: String, recipientID: String, amount: Int, person: Person): TransactionAction {
//            return TransactionAction(
//                clientID = clientID,
//                recipientID = recipientID,
//                amount = amount,
//                signature = person.sign((clientID + recipientID + amount).toByteArray(Charset.forName("UTF-8"))).toBase64()
//            )
//        }
//    }
}

data class VerifyAction(val clientID :String,
                        val dataID :String,
                        val signature :String) :Action(ActionType.VERIFY)

data class DataAction(
    val clientID: String,
    val data: DataPair,
    val signature: String
) : Action(ActionType.DATA) {
//
//    fun toSingableString(): String {
//        return clientID + serialize(data)
//    }
//
//    fun isSignatureValid(publicKey: PublicKey): Boolean {
//        return Person.verify(
//            publicKey,
//            Signature.fromBase64(signature),
//            this.toSingableString().toByteArray(Charset.forName("UTF-8"))
//        )
//    }
//    companion object {
//        fun sign(clientID: String, data: DataPair, person: Person): DataAction {
//            return DataAction(
//                clientID = clientID,
//                data = data,
//                signature = person.sign(
//                    DataAction(
//                        clientID = clientID,
//                        data = data,
//                        signature = ""
//                    ).toSingableString().toByteArray(Charset.forName("UTF-8"))
//                ).toBase64()
//            )
//        }
//    }
}
