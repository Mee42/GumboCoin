import systems.carson.base.Blockchain
import systems.carson.base.deserialize
import java.io.File
import java.time.Duration
import java.time.Instant

fun main() {

    val now = Instant.now()

    val blockchain = deserialize<Blockchain>(File("test.blockchain").readText())
    val list = blockchain
        .blocks
        .map { it.timestamp }
        .map { Duration.between(Instant.ofEpochMilli(it),now) }
        .map { it.seconds }
    val v = mutableListOf<Long>()
    for(i in 1 until list.size){
        val a = list[i-1]
        val b = list[i]
        v+=Math.abs(a - b)
    }
    println(v.average())

}