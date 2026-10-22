package com.example.smart_home_wallpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smart_home_wallpad.data.model.DeviceType
import com.example.smart_home_wallpad.data.model.HomeMode
import com.example.smart_home_wallpad.data.model.ModeSetting
import com.example.smart_home_wallpad.data.model.SmartDevice
import com.example.smart_home_wallpad.data.model.WallpadUiState
import com.example.smart_home_wallpad.ui.components.DeviceCard
import com.example.smart_home_wallpad.ui.components.HomeModesScreen
import com.example.smart_home_wallpad.ui.components.PowerOutletCard
import com.example.smart_home_wallpad.ui.components.RoomLightsCard
import com.example.smart_home_wallpad.ui.components.SensorCard
import com.example.smart_home_wallpad.ui.components.WallpadIcon
import com.example.smart_home_wallpad.ui.components.WallpadIconType

private enum class Destination(val label: String, val icon: WallpadIconType) {
    HOME("홈", WallpadIconType.HOME),
    LIGHTS("조명", WallpadIconType.LIGHT),
    CURTAINS("커튼", WallpadIconType.CURTAIN),
    AUTOMATION("자동화", WallpadIconType.AUTOMATION),
    SETTINGS("설정", WallpadIconType.SETTINGS),
}

@Composable
fun WallpadApp(
    state: WallpadUiState,
    onToggleDevice: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    onCurtainPositionChange: (String, Int) -> Unit,
    onAllLights: (Boolean) -> Unit,
    onTogglePowerOutlet: (String) -> Unit,
    onPowerOutletNameChange: (String, String) -> Unit,
    onApplyMode: (HomeMode?) -> Unit,
    onModeSettingChange: (HomeMode, ModeSetting, Boolean) -> Unit,
    onMessageShown: () -> Unit,
) {
    var selected by remember { mutableStateOf(Destination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); onMessageShown() } }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFFF8F3EC), Color(0xFFF0E6D8), Color(0xFFEAD9C5))),
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 600.dp
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = { if (compact) BottomNavigation(selected) { selected = it } },
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding)
                        .padding(horizontal = if (compact) 16.dp else 34.dp),
                ) {
                    DashboardTopBar(selected, compact, state.serverOnline) { selected = it }
                    WallpadContent(
                        Modifier.weight(1f), selected, state, compact, onToggleDevice,
                        onBrightnessChange, onCurtainPositionChange, onAllLights,
                        onTogglePowerOutlet, onPowerOutletNameChange,
                        onApplyMode, onModeSettingChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    selected: Destination,
    compact: Boolean,
    serverOnline: Boolean,
    onSelect: (Destination) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("내 방", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            ) {
                Text(
                    if (serverOnline) "● 연결됨" else "● 오프라인",
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = if (serverOnline) Color(0xFF087F5B) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (!compact) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Destination.entries.forEach { destination ->
                    FilterChip(
                        selected = selected == destination,
                        onClick = { onSelect(destination) },
                        label = { Text(destination.label) },
                        leadingIcon = { WallpadIcon(destination.icon, Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = {
                    WallpadIcon(
                        type = destination.icon,
                        modifier = Modifier.size(23.dp),
                        tint = Color(0xFF3F4854),
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun WallpadContent(
    modifier: Modifier,
    destination: Destination,
    state: WallpadUiState,
    compact: Boolean,
    onToggleDevice: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    onCurtainPositionChange: (String, Int) -> Unit,
    onAllLights: (Boolean) -> Unit,
    onTogglePowerOutlet: (String) -> Unit,
    onPowerOutletNameChange: (String, String) -> Unit,
    onApplyMode: (HomeMode?) -> Unit,
    onModeSettingChange: (HomeMode, ModeSetting, Boolean) -> Unit,
) {
    when (destination) {
        Destination.HOME -> DeviceGrid(modifier, state, compact, state.devices, true, onToggleDevice, onBrightnessChange, onCurtainPositionChange, onAllLights, onTogglePowerOutlet, onPowerOutletNameChange)
        Destination.LIGHTS -> DeviceGrid(modifier, state, compact, state.devices.filter { it.type == DeviceType.LIGHT }, false, onToggleDevice, onBrightnessChange, onCurtainPositionChange, onAllLights, onTogglePowerOutlet, onPowerOutletNameChange)
        Destination.CURTAINS -> DeviceGrid(modifier, state, compact, state.devices.filter { it.type == DeviceType.CURTAIN }, false, onToggleDevice, onBrightnessChange, onCurtainPositionChange, onAllLights, onTogglePowerOutlet, onPowerOutletNameChange)
        Destination.AUTOMATION -> HomeModesScreen(
            modifier = modifier,
            modeSettings = state.modeSettings,
            activeMode = state.activeHomeMode,
            onApplyMode = onApplyMode,
            onModeSettingChange = onModeSettingChange,
        )
        Destination.SETTINGS -> Placeholder(modifier, "연결 설정", "Home Assistant 또는 MQTT 서버 정보를 설정할 수 있도록 준비된 화면입니다.")
    }
}

@Composable
private fun DeviceGrid(
    modifier: Modifier,
    state: WallpadUiState,
    compact: Boolean,
    devices: List<SmartDevice>,
    showDashboard: Boolean,
    onToggleDevice: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    onCurtainPositionChange: (String, Int) -> Unit,
    onAllLights: (Boolean) -> Unit,
    onTogglePowerOutlet: (String) -> Unit,
    onPowerOutletNameChange: (String, String) -> Unit,
) {
    val roomLightIds = setOf("room-light-1", "room-light-2")
    val roomLights = devices.filter { it.id in roomLightIds }
    val otherDevices = devices.filterNot { it.id in roomLightIds }
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(if (compact) 280.dp else 310.dp),
        contentPadding = PaddingValues(bottom = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showDashboard) {
            item(span = { GridItemSpan(maxLineSpan) }) { DashboardHeader(state, compact, onAllLights) }
            item(span = { GridItemSpan(maxLineSpan) }) { SensorCard(state.sensor) }
            items(state.powerOutlets, key = { it.id }) { outlet ->
                PowerOutletCard(outlet, onPowerOutletNameChange, onTogglePowerOutlet)
            }
        }
        if (roomLights.size == roomLightIds.size) {
            item(span = { GridItemSpan(maxLineSpan) }) { RoomLightsCard(roomLights, onToggleDevice, onBrightnessChange) }
        }
        items(otherDevices, key = { it.id }) { DeviceCard(it, onToggleDevice, onBrightnessChange, onCurtainPositionChange) }
    }
}

@Composable
private fun DashboardHeader(state: WallpadUiState, compact: Boolean, onAllLights: (Boolean) -> Unit) {
    val lights = state.devices.filter { it.type == DeviceType.LIGHT }
    val allLightsOn = lights.isNotEmpty() && lights.all { it.isOn }
    val allLightsOff = lights.isNotEmpty() && lights.none { it.isOn }
    val lightStatus = when {
        allLightsOn -> "전체 조명 · 모두 켜짐"
        allLightsOff -> "전체 조명 · 모두 꺼짐"
        else -> "전체 조명 · 일부 켜짐"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Column {
                Text("오늘의 내 방", style = MaterialTheme.typography.titleMedium, color = Color(0xFF52616B))
                Spacer(Modifier.height(5.dp))
                Text("편안한 하루를 시작하세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(lightStatus, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF52616B))
            }
            Spacer(Modifier.height(if (compact) 16.dp else 12.dp))
            Row(
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = if (compact) Modifier.weight(1f) else Modifier,
                    onClick = { onAllLights(true) },
                    enabled = lights.any { !it.isPending },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allLightsOn) Color(0xFF1677FF) else Color(0xFFF0F3F6),
                        contentColor = if (allLightsOn) Color.White else Color(0xFF425466),
                    ),
                ) { Text("전체 켜기") }
                Button(
                    modifier = if (compact) Modifier.weight(1f) else Modifier,
                    onClick = { onAllLights(false) },
                    enabled = lights.any { !it.isPending },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allLightsOff) Color(0xFF1677FF) else Color(0xFFF0F3F6),
                        contentColor = if (allLightsOff) Color.White else Color(0xFF425466),
                    ),
                ) { Text("전체 끄기") }
            }
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier, title: String, description: String) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
