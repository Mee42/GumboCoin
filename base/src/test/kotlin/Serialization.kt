import org.junit.jupiter.api.Test

import EncryptionTests.Companion.privateBob
import EncryptionTests.Companion.publicBob
import EncryptionTests.Companion.privateBobRandom
import EncryptionTests.Companion.publicBobRandom
import EncryptionTests.Companion.privateEve
import EncryptionTests.Companion.publicEve
import EncryptionTests.Companion.privateEveRandom
import EncryptionTests.Companion.publicEveRandom
import EncryptionTests.Companion.privateAlice
import EncryptionTests.Companion.publicAlice
import EncryptionTests.Companion.privateAliceRandom
import EncryptionTests.Companion.publicAliceRandom
import org.junit.jupiter.api.Assertions.assertEquals
import systems.carson.base.Person


internal class Serialization {
    @Test
    fun `Serialize both public and private`(){
        val person = privateBob()
        val serialized = person.serialize()
        //println(serialized)
        val new = Person.deserialize(serialized)
        assertEquals(person,new)

    }
    @Test
    fun `Serialize just the public key`(){
        val person = publicBob()
        val serialized = person.serialize()
        println(serialized)
        val new = Person.deserialize(serialized)
        assertEquals(person,new)
    }
    //TODO FIGURE OUT HOW TO GET PUBLIC KEY
}
