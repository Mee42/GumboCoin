package com.gumbocoin.cli

import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import org.apache.commons.codec.digest.DigestUtils
import reactor.core.publisher.Flux
import systems.carson.base.*
import java.io.File
import java.nio.charset.Charset
import java.time.Duration
import java.util.*

val scan = Scanner(System.`in`)

val socket = RSocketFactory.connect()
    .transport(TcpClientTransport.create("localhost", PORT))
    .start()
    .block()!!

fun main() {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN")

    Flux.interval(Duration.ofMinutes(1))
        .flatMap { socket.requestResponse(RequestDataBlob(Request.Response.PING, "defaultID"), Person.default) }
        .subscribe()

    while (true) {
        print("G \$ ")
        val input = scan.nextLine()
        if (input == "exit") {
            miner?.exit()
            return
        }
        runStep(input)
    }
}

class ErrorStatus private constructor(val failed: Boolean, val message: String? = null) {
    companion object {
        fun success(): ErrorStatus = ErrorStatus(false, null)
        fun error(message: String): ErrorStatus = ErrorStatus(true, message)
        fun error(): ErrorStatus = ErrorStatus(true, null)
    }
}

fun Boolean.toError(message: String) = if (this) ErrorStatus.success() else ErrorStatus.error(message)

fun prompt(message: String, isValid: (String) -> ErrorStatus = { ErrorStatus.success() }): String {
    print("$message:")
    val input = scan.nextLine()
    val status = isValid(input)
    if (!status.failed)
        return input
    if (status.message != null)
        System.err.println(status.message)
    else
        println("Invalid input")
    return prompt(message, isValid)
}

var clientIDNullable: String? = null
val clientID: String
    get() {
        if (loggedIn)
            return clientIDNullable!!
        error("Can't access clientID if not logged in")
    }
var meNullable: Person? = null
val me: Person
    get() {
        if (loggedIn)
            return meNullable!!
        error("Can't access keys if not logged in")
    }

var loggedIn = false


var miner: ThreadedMiner? = null


fun runStep(input: String) {
    when (input) {
        "" -> {
        }
        "login" -> {
            //attempt to test the clientID
            val keyFile = File(prompt("Keyfile") { it.isNotBlank().toError("Input can not be blank") })

            val str = keyFile.name
            var maybeID = str.replaceFirst(".gc.key", "")
            if (!str.contains(".gc.key")) {
                maybeID = ""
            }
            maybeID = if (maybeID.isNotBlank()) "($maybeID)" else ""

            var clientID = prompt("Client ID $maybeID") {
                if (maybeID.isNotBlank())
                    ErrorStatus.success()
                else
                    it.isNotBlank().toError("Input can not be blank")
            }
            if (clientID.isBlank())
                clientID = maybeID.substring(1, maybeID.length - 1)//I hate this code
            clientID = clientID.trim()

            val person = Person.fromKeyFile(keyFile.readText(Charset.forName("UTF-8")))

            val result = socket.requestResponse(RequestDataBlob(Request.Response.VERIFIED, clientID), person)
                .map { Sendable.fromJson<SendableBoolean>(it) }
                .map { it.value }
                .block()
            if (result == true) {
                println("Logged in successfully")
                clientIDNullable = clientID
                meNullable = person
                loggedIn = true
            } else {
                println("Couldn't verify account. Login unsuccessful")
            }
            //TODO test
        }
        "signup" -> {
            val clientID = DigestUtils.sha1Hex(UUID.randomUUID().toString()).substring(0, 10)
            val keys = Person.generateNew()
            println("Registering with the server... ClientID: $clientID")
            val status = socket.requestResponse(
                SignUpDataBlob(
                    clientID = clientID,
                    signUpAction = SignUpAction(
                        clientID = clientID,
                        publicKey = keys.publicKeyBase64()
                    )
                ), keys
            )
                .mapFromJson<Status>()
                .block()
            if (status == null) {
                println("Didn't get a response from the server")
            } else {
                if (status.failed) {
                    println("Error:" + status.errorMessage)
                    status.extraData.let { if (it.isBlank()) null else it }
                        ?.let { println("Extra Data:$it") }
                } else {
                    println("Success")
                    clientIDNullable = clientID
                    meNullable = keys
                    loggedIn = true
                    var keyFile = prompt("Directory to store keys in (.)")
                    if (keyFile.isBlank())
                        keyFile = "./"
                    if (!keyFile.endsWith("/"))
                        keyFile = "$keyFile/"

                    val file = File("$keyFile$clientID.gc.key")
                    file.writeText(me.serialize(), Charset.forName("UTF-8"))
                    println("Created file $keyFile$clientID.gs.key")
                }
            }

        }
        "money" -> {
            if (!loggedIn) {
                println("You need to be logged to get your balance")
                return
            }
            val money = socket.requestResponse(
                StringDataBlob(
                    clientID = clientID,
                    intent = Request.Response.MONEY.intent,
                    value = clientID
                ), me
            )
                .mapFromJson<SendableInt>()
                .block()?.value ?: -1
            println("$clientID has $money Gumbocoin" + if (money == 1) "" else "s")

        }
        "miner" -> {
            if (!loggedIn) {
                println("Can't startup the miner without logging in")
                return
            }
            minerMenu()
        }
        "help" -> {
            """
            login   |  log in
            signup  |  make a new Gumbocoin account
            money   |  find out how many Gumbocoins you have
            miner   |  control the miner
            help    |  print this menu
            exit    |  exit
        """.trimIndent()
        }
        else -> {
            println("Unknown command \"$input\"")
        }
    }
}

fun minerMenu() {

    miner@ while (true) {

        print("Miner: $ ")
        when (val inn = scan.nextLine()) {
            "start" -> {
                if (miner == null) {
                    miner = ThreadedMiner(
                        socket = socket,
                        person = me,
                        clientID = clientID
                    )
                }
                println("Starting miner")
                miner?.start() ?: println("Error starting miner")

            }
            "stop" -> {
                if (miner == null || !miner!!.isRunning) {
                    println("Miner is not running, can't stop it")
                } else {
                    println("Stopping miner")
                    miner?.stop() ?: println("Error stopping miner...")
                }
            }
            "help" -> {
                """
                        You are in the mining menu!
                        There are only a few simple comamnds:
                        start  |   starts the miner
                        stop   |   stops the miner
                        back   |   exit the miner menu and return to the main prompt
                        status |   get the status of the miner

                    """.trimIndent().let { println(it) }
            }
            "status" -> {
                val m = miner
                if (m == null) {
                    println("You haven't started the miner yet")
                    continue@miner
                }
                println("Blocks mined:${m.mined}")
                println("Rejected blocks:${m.failed}")
                if (m.failed != 0) {
                    println("Last 5 errors:")
                    m.statuses.filter { it.failed }
                        .map { it.errorMessage + if (it.extraData.isNotBlank()) " - " + it.extraData else "" }
                        .forEach { println(it) }
                }
                println(
                    "Percent accepted:${m.mined.toDouble().div(m.failed.toDouble().plus(m.mined.toDouble())).times(
                        100
                    )}%"
                )
            }
            "back", "exit" -> {
                return
            }
            else -> {
                println("Unknown miner command \"$inn\"")
            }
        }
    }
}