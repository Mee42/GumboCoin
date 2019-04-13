import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document

external fun sleep(ms :Long)

external val client :RSocketClient

class RSocketClient{

}

@Suppress("UNCHECKED_CAST")
fun <T : Element> el(id :String):T{
    return document.getElementById(id) as T
}

fun main() {
    var i = 0
    el<HTMLButtonElement>("button_1").onclick = {
        el<HTMLDivElement>("text_1").textContent = "" + i++
        println("Hello")
    }
}