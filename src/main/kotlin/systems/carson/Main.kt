package systems.carson

class Main{
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val mode = if(args.isEmpty())error("need argument")else args[0]
            when(mode){
                "client" -> systems.carson.client.mainf()
                "server" -> systems.carson.server.main()
                "miner" -> systems.carson.miner.main()
                else -> error("unknown mode $mode")
            }
        }
    }
}