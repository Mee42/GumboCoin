package systems.carson.client

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import systems.carson.shared.*
import java.util.*


private val socket: RSocket = RSocketFactory.connect()
    .transport(TcpClientTransport.create("localhost", 7000))
    .start()
    .block()!!

fun main() {

    mainf()
//        launch<ClientVisual>(args)
}
fun mainf() {
    val scan = Scanner(System.`in`)
    var userId :String? = null
    while(true){
        try {
            print(">")
            val input = scan.nextLine()
            when (input) {
                "user" -> {
                    print("user id:")
                    userId = scan.nextLine()
                }
                "money" -> {
                    socket.requestResponse(
                        DataBlob(
                            intent = RequestString.USER_AMOUNT.string,
                            id = userId ?: throw IllegalStateException("user id not set")
                        ).payload()
                    )
                        .map { it.dataUtf8 }
                        .map { println(it) }
                        .block()
                }
                "pay" -> {
                    var money: Int? = null
                    socket.requestResponse(
                        DataBlob(
                            intent = RequestString.USER_AMOUNT.string,
                            id = userId ?: throw IllegalStateException("user id not set")
                        ).payload()
                    )
                        .map { it.dataUtf8 }
                        .map { Integer.parseInt(it) }
                        .subscribe { money = it }

                    print("user (to pay)id:")
                    val u = scan.nextLine()
                    while (money == null);
                    print("You have $money. Amount to pay:")
                    val f = scan.nextLine().toIntOrNull() ?: throw IllegalStateException("illegal integer")
                    if (f > money!!)
                        error("You don't have enough money :eyes:")
                    socket.requestResponse(DataBlob(
                        intent = RequestString.HACKED_TRANSACTION.string,
                        id = userId,
                        data = gson.toJson(Transaction(to = u,from = userId,amount = f))
                    ).payload())
                        .map { println(it.dataUtf8) }
                        .block()

                }
            }
        }catch(e :java.lang.IllegalStateException){
            System.out.flush()
            System.err.println(e.message)
            System.err.flush()
        }
    }

}
//
//lateinit var inst :ClientView
//
//class ClientView : View() {
//
//    private val textf = SimpleStringProperty("""
//    """.trimIndent())
//
//    override val root : Parent
//
//
//
//    init {
////        val h = 750.0
////        val w = 1500.0
//
//        root = hbox {
//
////            this.maxHeight = 500.0
////            this.minHeight = 500.0
////            this.prefHeight = 500.0
//
////            this.maxWidth = 500.0
////            this.minWidth = 500.0
//            this.prefWidth = 1000.0
//
////            alignment = Pos.TOP_CENTER
////            spacing = 10.0
////            transforms.setAll(Scale(this.width/this.prefWidth,this.height/this.prefHeight,0.0,0.0))
//            vbox {
////                alignment = Pos.TOP_CENTER
////                spacing = 10.0
//
//                button("Blockchain") {
////                    fitToParentWidth()
//                    action {
//                        socket.requestResponse(DataBlob(RequestString.BLOCKCHAIN_PRETTY.string).payload())
//                            .map { it.dataUtf8 }
//                            .map { w -> Platform.runLater { textf.value = w } }
//                            .subscribe()
//                    }
//                }
//                button("Users") {
////                    fitToParentWidth()
//                    action {
//                        socket.requestResponse(DataBlob(RequestString.USERS.string).payload())
//                            .map { it.dataUtf8 }
//                            .map { w -> Platform.runLater { textf.value = w } }
//                            .subscribe()
//                    }
//                }
//            }
////            vbox {
//                textarea {
////                    fitToParentHeight()
//                    textProperty().bind(this@ClientView.textf)
//                }
////            }
//
//        }
//
//        inst = this
//    }
//
//}
//
//
//
//class ClientVisual : App(ClientView::class, ClientStyles::class)
//
//class ClientStyles : Stylesheet() {
//    companion object {
//        val pink1 by cssclass()
//        val pink2 by cssclass()
//        val error by cssclass()
//    }
//    init {
//        label {
//            fontWeight = FontWeight.NORMAL
//            font = Font.font("monospace")
//            backgroundColor += c("#ffffff")
//        }
//
//        listView {
//            font = Font.font("monospace")
//            fontSize = 20.px
//        }
//
//        button {
//            backgroundColor += c("#aaaaaa")
//            minHeight = 10.px
//            minWidth = 10.px
//        }
//
//        error {
//            backgroundColor += c("#fc9999")
//        }
//        pink1 {
//            backgroundColor += c("#fa00ff")
//        }
//        pink2 {
//            backgroundColor += c("#af07b2")
//        }
//
//    }
//}