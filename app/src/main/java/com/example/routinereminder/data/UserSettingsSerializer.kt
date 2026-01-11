package com.example.routinereminder.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings
        get() = UserSettings(0.0, 0.0, 0, Gender.MALE, ActivityLevel.SEDENTARY, 0.0)

    override suspend fun readFrom(input: InputStream): UserSettings {
        try {
            return Json.decodeFromString(
                UserSettings.serializer(), input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            return defaultValue
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(UserSettings.serializer(), t)
                .encodeToByteArray()
        )
    }
}
