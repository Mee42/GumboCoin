package com.gumbocoin.cli

class ConsoleActionBuilder{
    var name :String = ""
    var desc :String = ""
    var aliases :List<String> = emptyList()
    var runner : Runner = TODORunner
    fun build(): ConsoleAction = ConsoleAction(
        name = name,
        desc = desc,
        aliases = aliases,
        runner = runner
    )


}


interface Filter{
    fun allow(context : Context):Boolean
    fun errorMessage():String? = null//Note: make into a builder and dynamically generate errors?
}

class FilteredRunnerBuilder{
    fun yes(message :String){
        filters.add(object : Filter {
            override fun allow(context : Context): Boolean {
                print(message.trim() + " (y/n) ")
                val input = context.scan.nextLine()
                return input.isNotEmpty() && input[0].toUpperCase() == 'Y' || input.isBlank()
            }
        })
    }
    fun conditional(check :(Context) -> Boolean){
        filters.add(object : Filter {
            override fun allow(context: Context): Boolean {
                return check.invoke(context)
            }
        })
    }
    fun conditional(errorMessage :String, check: (Context) -> Boolean){
        filters.add(object : Filter {
            override fun allow(context: Context): Boolean {
                return check(context)
            }
            override fun errorMessage(): String? {
                return errorMessage
            }
        })
    }

    var final : Runner = TODORunner
    fun runnerr(run :(Context) -> Unit) {
        final = object : Runner {
            override fun run(context: Context) {
                run.invoke(context)
            }
        }
    }


    private val filters = mutableListOf<Filter>()
    fun build(): FilteredRunner {
        if(final == TODORunner)
            error("OOF")
        return FilteredRunner(filters, final)
    }
}

class FilteredRunner(private val filters :List<Filter>, private val final : Runner) :
    Runner {
    override fun run(context: Context) {
        for(filter in filters){
            if(!filter.allow(context)) {
                filter.errorMessage()?.let { println(it) }
                return
            }
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
    fun action(block : ConsoleActionBuilder.() -> Unit){
        actions.add(ConsoleActionBuilder().apply(block).build())
    }
}



fun console(block : InteractiveConsoleBuilder.() -> Unit) = InteractiveConsoleBuilder().apply(block).build()
fun action(block : ConsoleActionBuilder.() -> Unit) = ConsoleActionBuilder().apply(block).build()
fun runner(run :(Context) -> Unit): Runner {
    return object : Runner {
        override fun run(context: Context) {
            run.invoke(context)
        }
    }
}

class SplitBuilder{
    var one : Runner = TODORunner
    var two : Runner = TODORunner
    fun build(): SplitRunner {

        if(one == TODORunner || two == TODORunner)
            error("OOF")

        return SplitRunner(one, two)
    }
}
class SplitRunner(private val one : Runner, private val two : Runner):
    Runner {
    override fun run(context: Context) {
        one.run(context)
        two.run(context)
    }
}

fun split(block : SplitBuilder.() -> Unit): Runner = SplitBuilder().apply(block).build()

fun filteredRunner(block : FilteredRunnerBuilder.() -> Unit) : Runner = FilteredRunnerBuilder().apply(block).build()

class SwitchBuilder{
    var conditional: ((Context) -> Boolean)? = null
    var truthy : Runner = TODORunner
    var falsy : Runner = TODORunner
    fun build(): Switch {
        if(truthy == TODORunner || falsy == TODORunner)
            error("OOF")
        return Switch(conditional!!, truthy, falsy)
    }
}

fun switchy(block : SwitchBuilder.() -> Unit): Runner = SwitchBuilder().apply(block).build()

class Switch(
    val conditional :(Context) -> Boolean,
    private val truthy : Runner,
    private val falsy : Runner
): Runner {
    override fun run(context: Context) {
        if(conditional(context)){
            truthy.run(context)
        }else{
            falsy.run(context)
        }
    }
}





























