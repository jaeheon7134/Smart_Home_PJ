package com.example.smart_home_wallpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smart_home_wallpad.data.model.WallpadUiState
import com.example.smart_home_wallpad.data.model.HomeMode
import com.example.smart_home_wallpad.data.model.ModeSetting
import com.example.smart_home_wallpad.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: DeviceRepository = DeviceRepository(),
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    init {
        if (repository.isRoomSwitchConfigured) {
            viewModelScope.launch {
                repository.refreshRoomLights().exceptionOrNull()?.let {
                    message.value = "방 조명 상태를 서버에서 가져오지 못했습니다."
                }
            }
        }
    }

    val uiState = combine(
        combine(
            repository.observeDevices(),
            repository.observePowerOutlets(),
            repository.observeSensor(),
            message,
        ) { devices, powerOutlets, sensor, notice ->
            WallpadUiState(devices, powerOutlets, sensor, serverOnline = true, message = notice)
        },
        combine(repository.observeModeSettings(), repository.observeActiveHomeMode()) { settings, activeMode ->
            settings to activeMode
        },
    ) { baseState, (settings, activeMode) ->
        baseState.copy(modeSettings = settings, activeHomeMode = activeMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallpadUiState())

    fun toggleDevice(id: String) = runCommand { repository.toggle(id).fold(
        onSuccess = { "${it.name} 상태가 동기화되었습니다." },
        onFailure = { "기기 응답을 받지 못했습니다." },
    ) }

    fun setBrightness(id: String, value: Int) = runCommand { repository.setBrightness(id, value).fold(
        onSuccess = { "밝기 ${it.brightness}%가 적용되었습니다." },
        onFailure = { "밝기를 변경하지 못했습니다." },
    ) }

    fun setCurtainPosition(id: String, value: Int) = runCommand { repository.setCurtainPosition(id, value).fold(
        onSuccess = { "커튼 위치 ${it.position}%가 동기화되었습니다." },
        onFailure = { "커튼을 제어하지 못했습니다." },
    ) }

    fun togglePowerOutlet(id: String) = runCommand { repository.togglePowerOutlet(id).fold(
        onSuccess = { "멀티탭 ${it.slot}구를 ${if (it.isOn) "켰습니다" else "껐습니다"}." },
        onFailure = { "멀티탭을 제어하지 못했습니다." },
    ) }

    fun updatePowerOutletName(id: String, name: String) {
        repository.updatePowerOutletName(id, name)
    }

    fun setAllLights(enabled: Boolean) = runCommand { repository.setAllLights(enabled).fold(
        onSuccess = { if (enabled) "모든 조명을 켰습니다." else "모든 조명을 껐습니다." },
        onFailure = { "일괄 제어에 실패했습니다." },
    ) }

    fun applyMode(mode: HomeMode?) = runCommand { repository.applyMode(mode).fold(
        onSuccess = { if (mode == null) "활성 모드를 해제했습니다." else "${mode.label}를 적용했습니다." },
        onFailure = { "모드를 적용하지 못했습니다." },
    ) }

    fun updateModeSetting(mode: HomeMode, setting: ModeSetting, enabled: Boolean) {
        repository.updateModeSetting(mode, setting, enabled)
    }

    fun clearMessage() { message.value = null }

    private fun runCommand(action: suspend () -> String) {
        viewModelScope.launch { message.value = action() }
    }
}

private val HomeMode.label: String
    get() = when (this) {
        HomeMode.OUTING -> "외출 모드"
        HomeMode.SLEEP -> "수면 모드"
        HomeMode.EXERCISE -> "운동 모드"
    }
