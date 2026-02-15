package com.example.archivetok.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class StringOrListDeserializer : JsonDeserializer<String> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): String? {
        return if (json.isJsonArray) {
            val array = json.asJsonArray
            if (array.size() > 0) {
                array[0].asString
            } else {
                null
            }
        } else if (json.isJsonPrimitive) {
            json.asString
        } else {
            null
        }
    }
}
