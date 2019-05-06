package systems.carson.base


enum class Release(val str: String) {
    MASTER("master"),
    BETA("beta"),
    DEV("dev")
}

object ReleaseManager {
    val release by lazy { Release.valueOf(System.getenv("RELEASE").toUpperCase()) }
}

object FlagManager {
    val debugFlags by lazy {
        System.getenv("DEBUG_FLAGS").toUpperCase()
            .split(";")
            .map { DebugFlags.valueOf(it) }
    }
}

enum class DebugFlags{
    LOW_DIFF
}