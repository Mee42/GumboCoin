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
    val logger by lazy { DefaultGLogger("%LEVEL%: $PAD%STR%") }
}

fun GLogger.Companion.logger(name :String):GLogger = LoggerWithName(name)


private const val PAD = "%PAD_HERE%"
private const val PAD_LENGTH = 15

private open class DefaultGLogger(val builder :String) :GLogger{

    private enum class GLevel(val level :Int){
        DEBUG(0),
        INFO(1),
        WARNING(2),
        FATAL(3)
    }
    private var level :GLevel = GLevel.DEBUG

    protected open fun getData():Map<String,String> = emptyMap()
    private fun getData(level :String):Map<String,String> = getData() + mapOf("level" to level)



    private fun format(str :String,data :Map<String,String>):String{
        return (data + mapOf("str" to str)).toList()
            .fold(builder) { a,b -> a.replaceData(b.first,b.second) }
            .pad()
    }

    private fun String.pad():String{
        return if(!this.contains(PAD))
            this
        else
            this.substring(0,this.indexOf(PAD)).padEnd(PAD_LENGTH,' ') +
                    this.substring(this.indexOf(PAD) + PAD.length)

    }

    private fun String.replaceData(name :String, value :String):String{
        return this.replace("%${name.toUpperCase()}%",value)
    }

    override fun debug(s: String) {
        if(level.level <= GLevel.DEBUG.level)
            println(format(s,getData("DEBUG")))
    }

    override fun warning(s: String) {
        if(level.level <= GLevel.WARNING.level)
            System.err.println(format(s,getData("WARN")))
    }

    override fun fatal(s: String) {
        if(level.level <= GLevel.FATAL.level)
            System.err.println(format(s,getData("FATAL")))
    }

    override fun info(s: String) {
        if(level.level <= GLevel.INFO.level)
            System.err.println(format(s,getData("INFO")))
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

private class LoggerWithName(val name :String) :DefaultGLogger("%LEVEL% - %NAME%: $PAD%STR%") {
    override fun getData(): Map<String,String> = mapOf("name" to name) + super.getData()
}
