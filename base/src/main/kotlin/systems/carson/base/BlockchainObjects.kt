package systems.carson.base

class Blockchain(val blocks: List<Block>)  {

    val users: List<User>
        get() = emptyList()//TODO even more todo :(

//            .flatMap { it.actions }//TODO this alwaus fucks up on reqorks
//            .filter { it.type == ActionType.SIGN_UP }
//            .map { Sendable.deserialize<SignUpData>(it.data) }
//            .map { TODO("Seed NetworkObjects ln24") }
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
) {
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
