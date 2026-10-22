package com.example.smart_home_wallpad.data.network

import com.example.smart_home_wallpad.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RoomLightStates(
    val light1On: Boolean,
    val light2On: Boolean,
)

/** HTTP client for the PC relay server that controls the ESP32 switch. */
class RoomSwitchGateway(
    private val baseUrl: String = BuildConfig.ROOM_SWITCH_SERVER_URL,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    suspend fun setLightState(lightId: String, isOn: Boolean): Result<Unit> = try {
        val switch = when (lightId) {
            "room-light-1" -> "room-light-1"
            "room-light-2" -> "room-light-2"
            else -> throw IllegalArgumentException("Unsupported room light: $lightId")
        }
        request("/api/room/switch?switch=$switch&state=${if (isOn) "on" else "off"}")
        Result.success(Unit)
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    suspend fun getLightStates(): Result<RoomLightStates> = try {
        val response = request("/api/room/status")
        val payload = JSONObject(response)
        Result.success(RoomLightStates(
            light1On = payload.getJSONObject("upper").getString("state") == "on",
            light2On = payload.getJSONObject("lower").getString("state") == "on",
        ))
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun request(path: String): String = withContext(Dispatchers.IO) {
        check(isConfigured) {
            "Server address is missing. Set roomSwitchServerUrl in local.properties."
        }
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(responseCode in 200..299) { "Server returned HTTP $responseCode: $body" }
            body
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000
    }
}
