package com.example.routinereminder.ui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonCodec {
    private val gson = Gson()

    /** Encode a single object to JSON string */
    fun <T> encode(value: T): String {
        return gson.toJson(value)
    }

    /** Decode a single object from JSON string */
    fun <T> decode(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Encode a list of objects to JSON string */
    fun <T> encodeList(list: List<T>): String {
        return gson.toJson(list)
    }

    /** Decode a list of objects from JSON string (non-inline to avoid compiler issues) */
    fun <T> decodeList(json: String): List<T> {
        return try {
            val type = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
