package com.example.archivetok.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class StringListDeserializer : JsonDeserializer<List<String>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<String> {
        return if (json.isJsonArray) {
            val array = json.asJsonArray
            val list = mutableListOf<String>()
            array.forEach { element ->
                if (!element.isJsonNull) {
                    list.add(element.asString)
                }
            }
            list
        } else if (json.isJsonPrimitive) {
            listOf(json.asString)
        } else {
            emptyList()
        }
    }
}
