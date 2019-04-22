package systems.carson.base

fun serialize(obj :Any) :String{
    return gson.toJson(obj)
}

inline fun <reified T> deserialize(string :String) :T{
    return gson.fromJson(string,T::class.java)
}