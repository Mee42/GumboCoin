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
        VERIFY("verify"),

        SUBMIT_KEY_FILE("submit_key_file"),
        GET_KEY_FILE("get_key_file")
    }

    enum class Stream(val intent: String) {
        NUMBERS("numbers"),
        BLOCKCHAIN_UPDATES("blockchain_updates")
    }
}

