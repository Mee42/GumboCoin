package systems.carson

import java.util.*

fun main() {
    val bob = Person("Bob")
    val alice = Person("Alice")
    val text = "Hello, World!"
    val encrypted = bob.encryptAES(text.toByteArray(),alice.publicKey)
    val plaintext = String(alice.decryptAES(encrypted))
    println(plaintext)
}
private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
