package systems.carson.shared

import com.google.gson.GsonBuilder
import io.rsocket.Payload
import org.apache.commons.codec.digest.DigestUtils
import systems.carson.server.payloadOf
import java.util.*


val gson = GsonBuilder().create()!!
val pretty = GsonBuilder()
    .setPrettyPrinting()
    .setLenient()
    .create()!!


const val iterations = 1

object Producer{
    var id = "default"
}

class DataBlob(val intent :String, val data :String = "", val id :String = Producer.id )

fun DataBlob.payload(): Payload {
    return payloadOf(gson.toJson(this))
}


data class Transactions(val list :List<Transaction>)

data class Transaction(val to :String, val from :String, val amount :Int)

class Block(val transactions :Transactions, var nonce :Long, val timestamp :Long, val lastHash :String){
    fun hash() :String{
        return recursiveHash(gson.toJson(this),iterations)
    }
    private fun recursiveHash(s :String, iterationsLeft :Int) :String{
        if(iterationsLeft < 0)return s
        return recursiveHash(DigestUtils.sha256Hex(s),iterationsLeft-1)
    }

    override fun toString(): String {
        return gson.toJson(this)
    }
}

fun String.isValidHash() :Boolean{
    return this.startsWith("00000")
}


fun <T> Optional<T>.ifNotPresent(var1: () -> Unit) {
    if (!this.isPresent) {
        var1.invoke()
    }

}


data class GenericStreamBlob(
    val usersUpdated :List<UserBlob> = emptyList(),
    val transactionsPassed :List<Transaction> = emptyList(),
    val totalCoins :Int)

data class UserBlob(
    val id :String,
    val blocksOwned :Int,
    val coins :Int,
    val minerOnline :Boolean)



enum class Const(val string :String, val equal :Boolean = true/*Opposite of equal is startsWith*/){
    BLOCK_ADDED_SUCCESS("Block has been added successfully"),
    BLOCK_ADD_FAILED("Error adding block", equal = false), ;

    companion object {
        fun matches(s: String, c :Const): Boolean {
            return if(c.equal)
                s == c.string
            else
                s.startsWith(c.string)
        }
    }
}



enum class RequestString(val string :String){
    LATEST_BLOCK("latest-block"),
    PING("ping"),
    BLOCK("block"),
    GENERIC_STREAM("generic-stream"),
    LATEST_TRANSACTIONS("transactions"),
    USER_AMOUNT("user-amount"),
    BLOCKCHAIN_PRETTY("blockchain-pretty"),
    USERS("users"),
    AUTH("auth"),
    HACKED_TRANSACTION("frik")
}

/*

QSfQFO9PE-
LbBawnjLc-
wXDgjwXuF-
RnLDFFs1S-
b9vN9o8


 */