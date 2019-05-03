package systems.carson.base

class Request {
    enum class Response(val intent: String) {
        PING("ping"),
        DECRYPT("decrypt"),
        VERIFIED("verified"),
        SIGN_UP("sign_up"),
        BLOCK("block"),
        BLOCKCHAIN("blockchain"),
        TRANSACTION("transaction"),
        MONEY("money"),
        DATA("data"),
        VERIFY("verify")
    }

    enum class Stream(val intent: String) {
        NUMBERS("numbers"),
        BLOCKCHAIN_UPDATES("blockchain_updates")
    }
}

