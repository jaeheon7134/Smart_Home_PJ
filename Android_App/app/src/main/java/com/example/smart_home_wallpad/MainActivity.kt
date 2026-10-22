package com.example.smart_home_wallpad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_home_wallpad.ui.WallpadApp
import com.example.smart_home_wallpad.ui.theme.WallpadTheme
import com.example.smart_home_wallpad.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallpadTheme {
                val viewModel: HomeViewModel = viewModel()
                val state = viewModel.uiState.collectAsStateWithLifecycle()
                WallpadApp(
                    state = state.value,
                    onToggleDevice = viewModel::toggleDevice,
                    onBrightnessChange = viewModel::setBrightness,
                    onCurtainPositionChange = viewModel::setCurtainPosition,
                    onAllLights = viewModel::setAllLights,
                    onTogglePowerOutlet = viewModel::togglePowerOutlet,
                    onPowerOutletNameChange = viewModel::updatePowerOutletName,
                    onApplyMode = viewModel::applyMode,
                    onModeSettingChange = viewModel::updateModeSetting,
                    onMessageShown = viewModel::clearMessage,
                )
            }
        }
    }
}
