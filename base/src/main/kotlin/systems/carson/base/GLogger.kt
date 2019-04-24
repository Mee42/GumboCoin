package systems.carson.base

interface GLogger {
    fun debug(s :String)//for debugging, don't comment out println's, GLogger#debug
    fun warning(s :String)//for errors
    fun fatal(s :String)//log the error and then crash
    fun info(s :String)//context and events happening

    fun printDebug()
    fun printWarnings()
    fun printFatal()
    fun printInfo()

    companion object
}

fun GLogger.Companion.logger():GLogger = Single.logger

private object Single{
    val logger by lazy { DefaultGLogger("%LEVEL%: %STR%") }
}

fun GLogger.Companion.logger(name :String):GLogger = LoggerWithName(name)


private open class DefaultGLogger(val builder :String) :GLogger{

    private enum class GLevel(val level :Int){
        DEBUG(0),
        INFO(1),
        WARNING(2),
        FATAL(3)
    }
    private var level :GLevel = GLevel.DEBUG

    protected open fun getData():List<String> = emptyList()

    private fun format(str :String,data :List<String> = getData()):String{
        var retur = builder.replaceData("LEVEL",level.name)
            .replaceData("STR",str)
        data.forEachIndexed {index, strr ->
            retur = retur.replaceData("data-$index",strr)
        }
        return retur
    }

    private fun String.replaceData(name :String, value :String):String{
        return this.replace("%${name.toUpperCase()}%",value)
    }

    override fun debug(s: String) {
        if(level.level <= GLevel.DEBUG.level)
            println(format(s))
    }

    override fun warning(s: String) {
        if(level.level <= GLevel.WARNING.level)
            System.err.println(format(s))
    }

    override fun fatal(s: String) {
        if(level.level <= GLevel.FATAL.level)
            System.err.println(format(s))
    }

    override fun info(s: String) {
        if(level.level <= GLevel.INFO.level)
            System.err.println(format(s))
    }

    override fun printDebug() {
        level = GLevel.DEBUG
    }

    override fun printWarnings() {
        level = GLevel.WARNING
    }

    override fun printFatal() {
        level = GLevel.FATAL
    }

    override fun printInfo() {
        level = GLevel.INFO
    }
}

private class LoggerWithName(val name :String) :DefaultGLogger("%LEVEL% - %DATA-0%: %STR%") {
    override fun getData(): List<String> = listOf(name)
}
