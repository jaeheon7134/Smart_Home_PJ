package com.example.smart_home_wallpad.data.repository

import com.example.smart_home_wallpad.data.model.DeviceType
import com.example.smart_home_wallpad.data.model.HomeMode
import com.example.smart_home_wallpad.data.model.HomeModeSettings
import com.example.smart_home_wallpad.data.model.ModeSetting
import com.example.smart_home_wallpad.data.model.PowerOutlet
import com.example.smart_home_wallpad.data.model.SensorReading
import com.example.smart_home_wallpad.data.model.SmartDevice
import com.example.smart_home_wallpad.data.network.RoomLightStates
import com.example.smart_home_wallpad.data.network.RoomSwitchGateway
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Network implementations (MQTT or Home Assistant) can replace this class without changing UI.
 * State is deliberately committed after the simulated device acknowledgement, not on tap.
 */
class DeviceRepository {
    private val roomSwitchGateway = RoomSwitchGateway()
    private val physicalRoomLightIds = setOf("room-light-1", "room-light-2")

    private val devices = MutableStateFlow(
        listOf(
            SmartDevice("room-light-1", "방 조명 1", "내 방", DeviceType.LIGHT, isOn = true),
            SmartDevice("room-light-2", "방 조명 2", "내 방", DeviceType.LIGHT, isOn = false),
            SmartDevice("room-computer-light", "컴퓨터 조명", "내 방", DeviceType.LIGHT, isOn = true, brightness = 70, supportsBrightness = true),
            SmartDevice("room-curtain", "커튼", "내 방", DeviceType.CURTAIN, position = 45),
            SmartDevice("room-auxiliary-light", "보조등", "내 방", DeviceType.LIGHT, isOn = false, brightness = 50, supportsBrightness = true),
            SmartDevice("room-camera", "방 카메라", "내 방", DeviceType.CAMERA, isOn = true),
        ),
    )
    private val sensor = MutableStateFlow(SensorReading(24.5f, 55f, "방금 전", true))
    private val powerOutlets = MutableStateFlow(
        listOf(
            PowerOutlet("room-power-strip-1", slot = 1),
            PowerOutlet("room-power-strip-2", slot = 2),
            PowerOutlet("room-power-strip-3", slot = 3),
        ),
    )
    private val modeSettings = MutableStateFlow(
        listOf(
            HomeModeSettings(HomeMode.OUTING, lightsOn = false, curtainsOpen = false, powerOutletsOn = false),
            HomeModeSettings(HomeMode.SLEEP, lightsOn = false, curtainsOpen = false, powerOutletsOn = true),
            HomeModeSettings(HomeMode.EXERCISE, lightsOn = true, curtainsOpen = true, powerOutletsOn = true),
        ),
    )
    private val activeHomeMode = MutableStateFlow<HomeMode?>(null)

    fun observeDevices(): StateFlow<List<SmartDevice>> = devices.asStateFlow()
    fun observeSensor(): StateFlow<SensorReading> = sensor.asStateFlow()
    fun observePowerOutlets(): StateFlow<List<PowerOutlet>> = powerOutlets.asStateFlow()
    fun observeModeSettings(): StateFlow<List<HomeModeSettings>> = modeSettings.asStateFlow()
    fun observeActiveHomeMode(): StateFlow<HomeMode?> = activeHomeMode.asStateFlow()

    val isRoomSwitchConfigured: Boolean
        get() = roomSwitchGateway.isConfigured

    suspend fun toggle(deviceId: String): Result<SmartDevice> {
        val previous = devices.value.firstOrNull { it.id == deviceId }
            ?: return Result.failure(IllegalArgumentException("Unknown device"))
        return if (deviceId in physicalRoomLightIds) {
            setPhysicalRoomLight(deviceId, !previous.isOn)
        } else {
            command(deviceId) { it.copy(isOn = !it.isOn) }
        }
    }

    suspend fun refreshRoomLights(): Result<Unit> = roomSwitchGateway.getLightStates().fold(
        onSuccess = { states ->
            applyRoomLightStates(states)
            Result.success(Unit)
        },
        onFailure = { Result.failure(it) },
    )

    suspend fun setBrightness(deviceId: String, brightness: Int): Result<SmartDevice> =
        command(deviceId) { it.copy(brightness = brightness.coerceIn(0, 100), isOn = brightness > 0) }

    suspend fun setCurtainPosition(deviceId: String, position: Int): Result<SmartDevice> =
        command(deviceId) { it.copy(position = position.coerceIn(0, 100)) }

    suspend fun togglePowerOutlet(outletId: String): Result<PowerOutlet> {
        val previous = powerOutlets.value.firstOrNull { it.id == outletId }
            ?: return Result.failure(IllegalArgumentException("Unknown power outlet"))
        if (!previous.isOnline) return Result.failure(IllegalStateException("Power outlet is offline"))
        powerOutlets.value = powerOutlets.value.map {
            if (it.id == outletId) it.copy(isPending = true) else it
        }
        delay(350)
        val updated = previous.copy(isOn = !previous.isOn, isPending = false)
        powerOutlets.value = powerOutlets.value.map { if (it.id == outletId) updated else it }
        return Result.success(updated)
    }

    fun updatePowerOutletName(outletId: String, name: String) {
        powerOutlets.value = powerOutlets.value.map {
            if (it.id == outletId) it.copy(connectedDeviceName = name) else it
        }
    }

    fun updateModeSetting(mode: HomeMode, setting: ModeSetting, enabled: Boolean) {
        modeSettings.value = modeSettings.value.map { profile ->
            if (profile.mode != mode) profile else when (setting) {
                ModeSetting.LIGHTS -> profile.copy(lightsOn = enabled)
                ModeSetting.CURTAINS -> profile.copy(curtainsOpen = enabled)
                ModeSetting.POWER_OUTLETS -> profile.copy(powerOutletsOn = enabled)
            }
        }
    }

    suspend fun applyMode(mode: HomeMode?): Result<Unit> {
        if (mode == null) {
            activeHomeMode.value = null
            return Result.success(Unit)
        }
        val profile = modeSettings.value.firstOrNull { it.mode == mode }
            ?: return Result.failure(IllegalArgumentException("Unknown home mode"))

        devices.value = devices.value.map { device ->
            when (device.type) {
                DeviceType.LIGHT, DeviceType.CURTAIN -> device.copy(isPending = true)
                else -> device
            }
        }
        powerOutlets.value = powerOutlets.value.map { it.copy(isPending = true) }
        delay(450)
        devices.value = devices.value.map { device ->
            when (device.type) {
                DeviceType.LIGHT -> device.copy(isOn = profile.lightsOn, isPending = false)
                DeviceType.CURTAIN -> device.copy(position = if (profile.curtainsOpen) 100 else 0, isPending = false)
                else -> device
            }
        }
        powerOutlets.value = powerOutlets.value.map {
            it.copy(isOn = profile.powerOutletsOn, isPending = false)
        }
        activeHomeMode.value = mode
        return Result.success(Unit)
    }

    suspend fun setAllLights(enabled: Boolean): Result<Unit> {
        for (id in physicalRoomLightIds) {
            val result = setPhysicalRoomLight(id, enabled)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
        }
        devices.value = devices.value.map {
            if (it.type == DeviceType.LIGHT && it.id !in physicalRoomLightIds) {
                it.copy(isOn = enabled, isPending = false)
            } else {
                it
            }
        }
        return Result.success(Unit)
    }

    private suspend fun setPhysicalRoomLight(deviceId: String, enabled: Boolean): Result<SmartDevice> {
        val previous = devices.value.firstOrNull { it.id == deviceId }
            ?: return Result.failure(IllegalArgumentException("Unknown room light"))
        if (!previous.isOnline) return Result.failure(IllegalStateException("Room light is offline"))

        devices.value = devices.value.map {
            if (it.id == deviceId) it.copy(isPending = true) else it
        }
        return roomSwitchGateway.setLightState(deviceId, enabled).fold(
            onSuccess = {
                val updated = previous.copy(isOn = enabled, isPending = false)
                devices.value = devices.value.map { if (it.id == deviceId) updated else it }
                Result.success(updated)
            },
            onFailure = { exception ->
                devices.value = devices.value.map {
                    if (it.id == deviceId) previous.copy(isPending = false) else it
                }
                Result.failure(exception)
            },
        )
    }

    private fun applyRoomLightStates(states: RoomLightStates) {
        val stateById = mapOf(
            "room-light-1" to states.light1On,
            "room-light-2" to states.light2On,
        )
        devices.value = devices.value.map { device ->
            stateById[device.id]?.let { device.copy(isOn = it, isPending = false) } ?: device
        }
    }

    private suspend fun command(deviceId: String, transform: (SmartDevice) -> SmartDevice): Result<SmartDevice> {
        val previous = devices.value.firstOrNull { it.id == deviceId }
            ?: return Result.failure(IllegalArgumentException("Unknown device"))
        if (!previous.isOnline) return Result.failure(IllegalStateException("Device is offline"))
        devices.value = devices.value.map { if (it.id == deviceId) it.copy(isPending = true) else it }
        delay(350)
        val updated = transform(previous).copy(isPending = false)
        devices.value = devices.value.map { if (it.id == deviceId) updated else it }
        return Result.success(updated)
    }
}
