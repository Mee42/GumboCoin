package systems.carson.base


enum class GLevel(val level: Int) {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    IMPORTANT(3),
    FATAL(4)
}


open class GLogger internal constructor(private val name: String? = null) {
    fun debug(message: String)//for debugging, don't comment out println'message, GLogger#debug
            = log(GLevel.DEBUG, message)

    fun warning(message: String)//for errors
            = log(GLevel.WARNING, message)

    fun fatal(message: String)//log the error and then crash
            = log(GLevel.FATAL, message)

    fun info(message: String)//context and events happening
            = log(GLevel.INFO, message)

    fun log(level: GLevel, message: String) {
        log(
            Information(
                level,
                message,
                name
            )
        )
    }

    private fun log(i: Information) {
        GManager.log(i)
    }


    companion object
}

class Information(
    val level: GLevel,
    val message: String,
    val nameOfLogger: String?
)

interface GLog {
    val level: GLevel
    fun log(information: Information)
}

object GManager {
    private val outputs = mutableListOf<GLog>()
    fun addLoggerImpl(glog: GLog) {
        outputs += glog
    }

    internal fun log(i: Information) {
        outputs.forEach {
            if (it.level.level <= i.level.level) {
                it.log(i)
            }
        }
    }
}

private const val PAD = "%PAD_HERE%"
private const val PAD_LENGTH = 15

class FileGLogger :OutputGLogger(){
    private val log = mapOf(
        GLevel.FATAL to "/tmp/fatal.g.log",
        GLevel.IMPORTANT to "/tmp/important.g.log",
        GLevel.INFO to "/tmp/info.g.log",
        GLevel.DEBUG to "/tmp/debug.g.log",
        GLevel.WARNING to "/tmp/warning.g.log")
    override fun print(str: String, level: GLevel) {
        super.print(str, level)//TODO
    }
}

open class OutputGLogger : GLog {
    fun setLevel(level: GLevel) {
        levelI = level
    }

    override val level: GLevel
        get() = levelI
    private var levelI = GLevel.WARNING


    private val builder = "%LEVEL%: $PAD%STR%"
    private val namedBuilder = "%LEVEL% - %NAME%: $PAD%STR%"

    private fun getData(level: GLevel): Map<String, String> = mapOf("level" to level.toString())

    override fun log(information: Information) {
        val name = information.nameOfLogger
        val str = if (name != null) {
            format(namedBuilder, information.message, getData(information.level) + mapOf("name" to name))
        } else {
            format(builder, information.message, getData(information.level))
        }
        print(str, information.level)
    }

    protected open fun print(str: String, level: GLevel) {
        when (level) {
            GLevel.DEBUG -> println(str)
            else -> System.err.println(str)
        }
    }

    private fun format(builder: String, str: String, data: Map<String, String>): String {
        return (data + mapOf("str" to str)).toList()
            .fold(builder) { a, b -> a.replaceData(b.first, b.second) }
            .pad()
    }

    private fun String.replaceData(name: String, value: String): String {
        return this.replace("%${name.toUpperCase()}%", value)
    }


    private fun String.pad(): String {
        return if (!this.contains(PAD))
            this
        else
            this.substring(0, this.indexOf(PAD)).padEnd(PAD_LENGTH, ' ') +
                    this.substring(this.indexOf(PAD) + PAD.length)

    }

}

fun GLogger.Companion.logger(): GLogger = GLogger()
fun GLogger.Companion.logger(name: String): GLogger = GLogger(name)
