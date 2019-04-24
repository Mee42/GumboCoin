package systems.carson.base

class Blockchain(val blocks: List<Block>)  {

    val users: List<User>
        get() = blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.SIGN_UP }
            .map { it as SignUpAction }
            .map { User(it.clientID,Person.fromPublicKey(it.publicKey)) }
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
