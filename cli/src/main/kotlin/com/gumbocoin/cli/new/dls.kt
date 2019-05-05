package com.gumbocoin.cli.new

class ConsoleActionBuilder{
    var name :String = ""
    var desc :String = ""
    var aliases :List<String> = emptyList()
    var runner :Runner = TODORunner()
    fun build():ConsoleAction = ConsoleAction(
        name = name,
        desc = desc,
        aliases = aliases,
        runner = runner
    )


}


interface Filter{ fun allow(context :Context):Boolean }

class FilteredRunnerBuilder{
    fun yes(message :String){
        filters.add(object :Filter{
            override fun allow(context :Context): Boolean {
                print(message.trim() + " (y/n) ")
                val input = context.scan.nextLine()
                return input.isNotEmpty() && input[0].toUpperCase() == 'Y' || input.isBlank()
            }
        })
    }
    fun conditional(check :() -> Boolean){
        filters.add(object :Filter {
            override fun allow(context: Context): Boolean {
                return check.invoke()
            }
        })

    }

    var final :Runner = TODORunner()

    private val filters = mutableListOf<Filter>()
    fun build() = FilteredRunner(filters,final)
}

class FilteredRunner(private val filters :List<Filter>, private val final :Runner) :Runner{
    override fun run(context: Context) {
        for(filter in filters){
            if(!filter.allow(context))
                return
        }
        final.run(context)
    }
}


class InteractiveConsoleBuilder{
    private val actions = mutableListOf<ConsoleAction>()
    var prompt :String = "$"
    fun build() = InteractiveConsole(
        actions = actions,
        prompt = prompt
    )
    fun action(block :ConsoleActionBuilder.() -> Unit){
        actions.add(ConsoleActionBuilder().apply(block).build())
    }
}



fun console(block :InteractiveConsoleBuilder.() -> Unit) = InteractiveConsoleBuilder().apply(block).build()
fun action(block :ConsoleActionBuilder.() -> Unit) = ConsoleActionBuilder().apply(block).build()
fun runner(run :(Context) -> Unit): Runner {
    return object :Runner {
        override fun run(context: Context) {
            run.invoke(context)
        }
    }
}
fun filteredRunner(block :FilteredRunnerBuilder.() -> Unit) :Runner = FilteredRunnerBuilder().apply(block).build()