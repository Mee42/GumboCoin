package systems.carson.base


enum class Release(val str: String) {
    MASTER("master"),
    BETA("beta"),
    DEV("dev")
}

enum class DevFlags{
    LOW_DIFF
}