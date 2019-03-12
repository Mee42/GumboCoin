package systems.carson.client

import com.google.gson.reflect.TypeToken
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Control
import javafx.scene.layout.*
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.fxmisc.easybind.EasyBind
import reactor.core.publisher.Mono
import systems.carson.shared.*
import tornadofx.*


private val socket: RSocket = RSocketFactory.connect()
    .transport(TcpClientTransport.create("localhost", 7000))
    .start()
    .block()!!

fun main(args: Array<String>) {
    //start transfers and such
    socket.requestStream(DataBlob(RequestString.GENERIC_STREAM.string).payload())
        .map { it.dataUtf8 }
        .map { gson.fromJson(it,GenericStreamBlob::class.java) }
        .map { println(it) }
        .blockLast()

    launch<ClientVisual>(args)
}

lateinit var inst :ClientView

class ClientView : View() {

    class User(val id :SimpleStringProperty,val value :SimpleIntegerProperty)

    private val users = FXCollections.observableArrayList<User>()

    val specs: List<Pair<String, SimpleStringProperty>> = listOf(
        Pair("ID",SimpleStringProperty("null")),
        Pair("Coins",SimpleStringProperty("null")),
        Pair("Blocks",SimpleStringProperty("null")),
        Pair("Transactions",SimpleStringProperty("null")))

    override val root : Parent


    fun updateSpecs(){
        specs[0].second.set(Credentials.username.get())
    }


    init {
//        val h = 750.0
//        val w = 1500.0

        root = hbox {
            vbox {
                alignment = Pos.TOP_CENTER
//                spacer {
//                    spacing = 0.1
//                    prefHeight = 0.1
//                }
                spacing = 10.0

                button("Users") {
                    fitToParentWidth()
                    action {
                        socket.requestResponse(DataBlob(RequestString.USERS.string).payload())
                            .map { it.dataUtf8 }
                            .map { gson.fromJson<Map<String,Int>>(it, object:TypeToken<HashMap<String,Int>>() {}.type) }
                            .map { it.toList() }
                            .map {
                                it.map { w ->
                                    User(
                                        SimpleStringProperty(w.first),
                                        SimpleIntegerProperty(w.second)
                                    )
                                }
                            }
                            .map { Platform.runLater { users.clear(); users.addAll(it) } }
                            .subscribe()
                    }
                }
                button("Sign in") {
                    fitToParentWidth()
                    action {
                        if (Credentials.authorized.value) {
                            Credentials.username.set(null)
                            Credentials.password.set(null)
                            Credentials.authorized.set(false)
                        } else {
                            openInternalWindow<SignIn>()
                        }
                    }
                    this.textProperty()
                        .bind(EasyBind.map(Credentials.authorized) { w: Boolean -> if (w) "Sign out" else "Sign in" })
                }
                vbox {
                    visibleProperty().bind(Credentials.authorized)
//                    spacer {
//                        prefHeight = 210.0
//                    }
                    spacing = 10.0
                    prefWidth = 100.0
                    button("pay") {
                        fitToParentWidth()
                    }
                    vbox {
                        alignment = Pos.TOP_CENTER
                        background = Background(BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY))
                        label("Stats"){
                            style = "-fx-font-weight: bold;-fx-font-size:20;"
                        }

                        specs.forEach {
                            val title = it.first
                            val value = it.second
                            this+= label(title){
                                style = "-fx-font-weight: bold;-fx-font-size:15;"
                            }
                            this+= label(value){
                                this.textProperty().bind(value)
                                style = "-fx-font-size:14;"
                            }
                        }
                    }
                }
                button("Sign up") {
                    this.isVisible = true
                    fitToParentWidth()
                    this.textProperty().bind(Credentials.authorized.map { w: Boolean -> if (w) "Help" else "Sign up" })
                    action {
                        if (Credentials.authorized.value) {
                            //if help menu
                            openInternalWindow<HelpMenu>()
                        } else {
                            println("sign up")
                        }
                    }
                }
            }
            tableview(users) {
                border = Border.EMPTY
                column("ID", User::id)
                column("Value", User::value)
                placeholder = label("No data")
                isFocusTraversable = false
            }

        }
        updateSpecs()
        inst = this
    }

}

private fun <T,E> ObservableValue<T>.map(function: (T) -> E): ObservableValue<E> {
    return EasyBind.map(this) {w -> function(w) }
}


object Credentials{
    var username :SimpleStringProperty = SimpleStringProperty(null)
    var password :SimpleStringProperty = SimpleStringProperty(null)
    var authorized :SimpleBooleanProperty = SimpleBooleanProperty(false)

    init {
        username.set("u")
        password.set("p")
        authorized.set(true)
    }

    fun isAuthorized() : Mono<Boolean> {
        if(Credentials.password.isNull.get())return Mono.just(false)
        if(Credentials.username.isNull.get())return Mono.just(false)

        if(Credentials.password.get().contains(":")){
            return Mono.just(false)
        }
        return socket.requestResponse(DataBlob(RequestString.AUTH.string, data = Credentials.username.get() + ":" + Credentials.password.get()).payload())
            .map { it.dataUtf8 }
            .map { it == "true" }
            .map { safe { authorized.set(it) };it }
    }
}

fun safe(r :() -> Unit){
    Platform.runLater { r() }
}

class HelpMenu :Fragment(){
    override val root: Parent = vbox {
        label("Help Menu"){
            alignment = Pos.TOP_CENTER
            font = Font.font("monospace",20.0)
        }
        text("" +
                "*put help stuff here*")
    }
}

class SignIn :Fragment() {

    val username = textfield {}
    val password = passwordfield {}
    val error = label {
        addClass(ClientStyles.error)
        isVisible = false
    }



    override val root: Parent = vbox {
        spacing = 10.0
        hbox {
            label("Username:")
            this+=username
        }
        hbox {
            label("Password:")
            this+=password
        }
        hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 10.0
            button("Sign in") {
                prefWidth = 75.0
                action {
                    Credentials.username.set(username.text)
                    Credentials.password.set(password.text)
                    //try to authorize
                    Credentials.isAuthorized().subscribe {bool :Boolean ->
                        if (bool)
                            Platform.runLater { inst.updateSpecs();close() }
                        else
                            Platform.runLater {
                                error.text = "Error logging in"
                                error.isVisible = true
                            }
                    }
                }
            }
            this+=error
        }
    }
}


class ClientVisual : App(ClientView::class, ClientStyles::class)

class ClientStyles : Stylesheet() {
    companion object {
        val pink1 by cssclass()
        val pink2 by cssclass()
        val error by cssclass()
    }
    init {
        label {
            fontWeight = FontWeight.NORMAL
            font = Font.font("monospace")
            backgroundColor += c("#ffffff")
        }

        listView {
            font = Font.font("monospace")
            fontSize = 20.px
        }

        button {
            backgroundColor += c("#aaaaaa")
            minHeight = 10.px
            minWidth = 10.px
        }

        error {
            backgroundColor += c("#fc9999")
        }
        pink1 {
            backgroundColor += c("#fa00ff")
        }
        pink2 {
            backgroundColor += c("#af07b2")
        }

    }
}