package systems.carson.base

import com.google.gson.Gson

//fun main() {
//    println(Person.generateNew().serialize())
//}


/*
open class Action(val type: ActionType)
class SignUpAction(val clientID :String,val publicKey :String) : Action(ActionType.SIGN_UP)
 */
class A(val list :List<Action>)

fun main() {

    val a = A(listOf(SignUpAction("client-id","public-key")))
    val str :String = gson.toJson(a)

    println(str)
}

class StringContainer(val str :String)
class B(val list :List<StringContainer>)

fun mainf() {

    val cont = StringContainer("Stringy string")
    val b = B(listOf(cont))


    val str :String = gson.toJson(cont)

    println(str)

    val bc = gson.fromJson(str,StringContainer::class.java)


    val str2 :String = gson.toJson(bc)

    println(str2)


}