import com.gumbocoin.base.hexToByteArray
import com.gumbocoin.base.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class HelperTests {
    @Test
    fun `hexDecoding`(){
        val list = listOf(0x123,0x00,0x215,0x010).map { it.toByte() }
        val bytes = ByteArray(list.size) { index -> list[index] }
        val hash = bytes.toHexString()
        val plain = hash.hexToByteArray()
        assertEquals(plain,bytes)
    }
}