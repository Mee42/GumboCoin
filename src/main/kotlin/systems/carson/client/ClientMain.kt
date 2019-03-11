package systems.carson.client

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import systems.carson.shared.*
import java.util.*

fun main() {
    startClient()
}

fun startClient(){
    Producer.id = "client"

    val socket: RSocket = RSocketFactory.connect()
        .transport(TcpClientTransport.create("localhost", 7000))
        .start()
        .block()!!

    val scan = Scanner(System.`in`)
    while(true){
        print('>')
        val input = scan.nextLine()
        when(input){
            "users" -> {
                socket.requestResponse(DataBlob(RequestString.USERS.string).payload())
                    .map { it.dataUtf8 }
                    .map { println(it) }
                    .block()
            }
            "blockchain" -> {
                socket.requestResponse(DataBlob(RequestString.BLOCKCHAIN_PRETTY.string).payload())
                    .map { it.dataUtf8 }
                    .map { println(it) }
                    .block()
            }
        }
    }



}