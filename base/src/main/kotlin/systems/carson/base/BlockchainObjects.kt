package systems.carson.base

import java.lang.IllegalStateException

class Blockchain(val blocks: List<Block>) : Sendable {

    val users: List<User>
        get() = blocks
//            .flatMap { it.actions }//TODO this alwaus fucks up on reqorks
//            .filter { it.type == ActionType.SIGN_UP }
//            .map { Sendable.deserialize<SignUpData>(it.data) }
            .map { TODO("Seed NetworkObjects ln24") }
//            .map { User(it.clientID,Person.deserialize(it.publicKey)) }
}

class User(
    val id: String,
    val person: Person
)

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

    val hash :String
        get() {
            if(hashLazy == null){
                hashLazy = lazyOf(hash())
            }
            return hashLazy!!.value
        }

    @Transient
    private var hashLazy :Lazy<String>? = lazy { hash() }

}

open class Action(val type: ActionType) : Sendable{
    override fun toString(): String { return this.send() }
    companion object
}


class SignUpAction(val clientID :String,val publicKey :String) : Action(ActionType.SIGN_UP)



//class SignUpData(val clientID :String,val publicKey: String):Sendable

enum class ActionType { SIGN_UP }
