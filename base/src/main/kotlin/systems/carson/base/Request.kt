package systems.carson.base


class Request{

    enum class Response(val intent :String){
        PING("ping"),
        DECRYPT("decryptAES"),
        VERIFIED("verified"),
        SIGN_UP("sign-up"),
        BLOCK("block")
    }
    enum class Stream(val intent :String){
        NUMBERS("numbers"),
        BLOCKCHAIN_UPDATES("blockchain-updates")
    }
//    enum class Fire(val intent :String){
//      //TODO when I need to
//    }
}
