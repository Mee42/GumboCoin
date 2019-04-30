import EncryptionTests.Companion.privateBob
import EncryptionTests.Companion.publicBob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import systems.carson.base.Person


internal class Serialization {
    @Test
    fun `Serialize both public and private`() {
        val person = privateBob()
        val serialized = person.serialize()
        //println(serialized)
        val new = Person.fromKeyFile(serialized)
        assertEquals(person, new)

    }

    @Test
    fun `Serialize just the public key`() {
        val person = publicBob()
        val serialized = person.serialize()
//        println(serialized)
        val new = Person.fromKeyFile(serialized)
        assertEquals(person, new)
    }
    //TODO FIGURE OUT HOW TO GET PUBLIC KEY
}
