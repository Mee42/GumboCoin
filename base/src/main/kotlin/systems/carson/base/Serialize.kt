package systems.carson.base

import com.google.gson.GsonBuilder


fun serialize(obj :Any, changes :(GsonBuilder) -> GsonBuilder = { it }) :String{
    return changes(GsonBuilder()).create().toJson(obj)
}

inline fun <reified T> deserialize(string :String) :T{
    return GsonBuilder().create().fromJson(string,T::class.java)
}