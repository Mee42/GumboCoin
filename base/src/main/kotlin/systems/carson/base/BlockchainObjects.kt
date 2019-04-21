package systems.carson.base

class Blockchain(val blocks: List<Block>) : Sendable {

    val users: List<User>
        get() = blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.SIGN_UP }
            .map { Sendable.deserialize<SignUpData>(it.data) }
            .map { User(it.clientID, Person.fromPublicKey(it.publicKey)) }
}

class User(
    val id: String,
    val person: Person
):Sendable//TODO FIXME RIGHTNOW use this to replace SignUpData

data class Block(
    val author: String,
    val actions: List<Action>,
    val timestamp: Long,
    val nonce: Long,
    val difficulty: Long,
    val lasthash: String,
    val signature: String
) : Sendable{
    companion object

    @delegate:Transient
    val hash by lazy { this.hash() }
}

open class Action(val type: ActionType, val data :String) : Sendable


class SignUpAction(clientID :String, publicKey: String) : Action(ActionType.SIGN_UP, SignUpData(clientID,publicKey).send())

class SignUpData(val clientID :String, val publicKey :String) :Sendable

enum class ActionType { SIGN_UP }
