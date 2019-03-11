package systems.carson.server

import io.rsocket.*
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import systems.carson.shared.DataBlob
import systems.carson.shared.RequestString
import systems.carson.shared.gson


class MasterHandler : SocketAcceptor {
    override fun accept(setup: ConnectionSetupPayload?, sendingSocket: RSocket?): Mono<RSocket> {
        return Mono.just(object : AbstractRSocket(){
            override fun requestResponse(payload: Payload?): Mono<Payload> {
                return if(payload != null)
                    Mono.fromCallable { this@MasterHandler.requestResponse(payload) }
                else
                    Mono.empty()
            }

            override fun fireAndForget(payload: Payload?): Mono<Void> {
                return if(payload != null)
                    Mono.fromRunnable { this@MasterHandler.fireAndForget(payload) }
                else
                    Mono.empty()
            }

            override fun requestStream(payload: Payload?): Flux<Payload> {
                return if(payload != null)
                    this@MasterHandler.requestStream(payload)
                else
                    Flux.empty()
            }
        })
    }

    fun requestResponse(payload : Payload) : Payload {
        log("RR: " +payload.dataUtf8)
        val data = gson.fromJson(payload.dataUtf8, DataBlob::class.java)
        return RequestResponse
            .handlers
            .filter { it.key == data.intent }
            .toList()
            .firstOrNull()
            ?.second?.invoke(DataPackage(data,payload)) ?: {
            System.err.println("No RR handler for intent ${data.intent}")
            payloadOf()
        }()
    }

    fun requestStream(payload : Payload) : Flux<Payload> {
        log("RS: " + payload.dataUtf8)

        val data = gson.fromJson(payload.dataUtf8, DataBlob::class.java)
        return RequestStream.handlers
            .filter {it.key == data.intent}
            .toList()
            .firstOrNull()
            ?.second?.invoke(DataPackage(data,payload)) ?: {
            System.err.println("No RS handler for intent ${data.intent}")
            Flux.empty<Payload>()
        }()
    }
    fun fireAndForget(payload : Payload) {
        log("FAF:" + payload.dataUtf8)
        val data = gson.fromJson(payload.dataUtf8, DataBlob::class.java)
        FireAndForget
            .handlers
            .filter { it.key == data.intent }
            .toList()
            .firstOrNull()
            ?.second?.invoke(DataPackage(data,payload)) ?: {
            System.err.println("No FAF handler for intent ${data.intent}")
            payloadOf()
        }()
    }
}

fun log(s :String){
    println(s)
}



fun payloadOf(s :String = ""): Payload = DefaultPayload.create(s)

class DataPackage(val data : DataBlob, val payload : Payload)

object RequestResponse{
    val handlers = mutableMapOf<String,(DataPackage) -> Payload>()

    operator fun set(s : RequestString, r:(DataPackage) -> Payload) = set(s.string,r)

    operator fun set(s :String,r: (DataPackage) -> Payload){
        RequestResponse.handlers[s] = r
    }
}

object RequestStream{
    val handlers = mutableMapOf<String,(DataPackage) -> Flux<Payload>>()
    operator fun set(s : RequestString, r:(DataPackage) -> Flux<Payload>) = set(s.string, r)

    operator fun set(s :String,r: (DataPackage) -> Flux<Payload>){
        RequestStream.handlers[s] = r
    }
}

object FireAndForget{
    val handlers = mutableMapOf<String,(DataPackage) -> Unit>()

    operator fun set(s : RequestString, r:(DataPackage) -> Unit) = set(s.string,r)

    operator fun set(s :String,r: (DataPackage) -> Unit){
        FireAndForget.handlers[s] = r
    }
}