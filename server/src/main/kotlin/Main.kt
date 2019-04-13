import systems.carson.Person

fun main() {
    val bob = Person.generateNew("Bob")
    val data = "Hello, my name is Bob".toByteArray()
    val encryptedData = Person.encrypt(data,bob)
    val plaintext = bob.decrypt(encryptedData)
    println(String(plaintext))
}