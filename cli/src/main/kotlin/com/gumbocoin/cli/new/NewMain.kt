package com.gumbocoin.cli.new

import com.gumbocoin.cli.*
import com.gumbocoin.cli.new.dsl.mainConsole
import com.xenomachina.argparser.ArgParser
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.client.TcpClientTransport
import reactor.core.publisher.Flux
import systems.carson.base.*
import java.time.Duration
import java.util.*


fun main(args :Array<String>) {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN")

    val socket = GSocket()
    val context = Context.create(socket,args)
    socket.setContext(context)
    context.initServerKey()

    Flux.interval(Duration.ofMinutes(1))
        .flatMap { socket.requestResponse(RequestDataBlob(Request.Response.PING, "default")) }
        .subscribe()
    mainConsole.run(context)
}

class GSocket{
    private lateinit var context :Context
    fun setContext(context: Context){ this.context = context }
    private val socket :RSocket = RSocketFactory.connect()
        .transport(TcpClientTransport.create("localhost", PORT))
        .start()
        .block()!!

    fun requestResponse(blob :RequestDataBlob, keys :Person) = socket.requestResponse(blob,keys)
    fun requestResponse(blob :RequestDataBlob) = requestResponse(blob, if(context.isLoggedIn) context.credentials.keys else Person.default )
    fun requestStream(blob :RequestDataBlob, keys :Person) = socket.requestStream(blob,keys)
    fun requestStream(blob :RequestDataBlob) = requestStream(blob, if(context.isLoggedIn) context.credentials.keys else Person.default )

}

class ConsoleAction(
    val name :String,
    val aliases :List<String>,
    val desc :String,
    val runner :Runner)



class Context private constructor(var socket: GSocket,
                                  var scan: Scanner,
                                  private var credentialsNullable :Credentials? = null,
                                  var threadedMiner :ThreadedMiner? = null,
                                  val arguments :PassedArguments){
    val isLoggedIn
        get() = credentialsNullable != null
    val credentials
        get() = credentialsNullable!!

    fun setDaCredentials(credentials: Credentials){
        credentialsNullable = credentials
    }

    //make sure there's only ever one instant - maybe later, we might need more?
    // we should stay away from global states, right
    companion object{
        private var created = false
        fun create(socket: GSocket, args :Array<String>):Context{
            if(created)
                error("Can not create more then one instance of Context")
            created = true
            return Context(
                socket = socket,
                scan = Scanner(System.`in`),
                credentialsNullable = null,
                arguments = ArgParser(args).parseInto(::PassedArguments))
        }
    }

    operator fun component1() = socket
    operator fun component2() = scan
    operator fun component3() = isLoggedIn
    operator fun component4() = credentials
}

interface Runner{ fun run(context: Context) }

object TODORunner :Runner{
    override fun run(context: Context) {
        TODO("not implemented")
    }
}


data class Credentials(
    val clientID: String,
    val keys: Person)

class InteractiveConsole(private val actions :List<ConsoleAction>,
                         private val prompt :String){

    fun run(context :Context){
        while(true) {
            print("$prompt ")
            val input = context.scan.nextLine().trim()
            if(input == "help"){
                val pad = (actions.map { it.name.length }.max() ?: 0) + 2
                actions
                    .map { it.name.padEnd(pad,' ') + "|  " + it.desc }
                    .forEach { println(it) }
                continue
            }else if(input == "exit"){
                return
            }
            val action = actions.firstOrNull { it.name == input || it.aliases.contains(input) }
            if (action == null) {
                println("Can't find action \"$input\"")
                continue
            }
            action.runner.run(context)
        }
    }
}