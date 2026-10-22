package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smart_home_wallpad.data.model.DeviceType
import com.example.smart_home_wallpad.data.model.SmartDevice

@Composable
fun DeviceCard(
    device: SmartDevice,
    onToggle: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    onCurtainPositionChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFF8FAFB),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WallpadIcon(deviceIcon(device), Modifier.size(30.dp), tint = Color(0xFF425466))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(device.room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (device.type != DeviceType.CURTAIN) {
                    Switch(
                        checked = device.isOn,
                        enabled = device.isOnline && !device.isPending,
                        onCheckedChange = { onToggle(device.id) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFBF6EE))
                    .padding(14.dp),
            ) {
                when (device.type) {
                    DeviceType.LIGHT -> LightControls(device, onBrightnessChange)
                    DeviceType.CURTAIN -> CurtainControls(device, onCurtainPositionChange)
                    DeviceType.CAMERA -> CameraPreview(device)
                    else -> DeviceStatus(device)
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(device: SmartDevice) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3F3027), Color(0xFF94704D), Color(0xFFE4B861)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WallpadIcon(WallpadIconType.CAMERA, Modifier.size(36.dp), tint = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (device.isOn) "카메라 영상 영역" else "카메라가 꺼져 있습니다",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (device.isOn) {
                Text(
                    "영상 연결 전",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB9C3D0),
                )
            }
        }
    }
}

@Composable
private fun LightControls(device: SmartDevice, onBrightnessChange: (String, Int) -> Unit) {
    if (device.supportsBrightness) {
        var value by remember(device.id, device.brightness) { mutableStateOf((device.brightness ?: 0).toFloat()) }
        Text("밝기 ${value.toInt()}%", style = MaterialTheme.typography.labelLarge)
        ControlSlider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onBrightnessChange(device.id, value.toInt()) },
            enabled = device.isOnline && !device.isPending,
            accentColor = Color(0xFFF5B942),
        )
    } else {
        DeviceStatus(device)
    }
}

@Composable
private fun CurtainControls(device: SmartDevice, onCurtainPositionChange: (String, Int) -> Unit) {
    var value by remember(device.id, device.position) { mutableStateOf((device.position ?: 0).toFloat()) }
    val isOpen = value >= 95f
    val isClosed = value <= 5f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("열림 ${value.toInt()}%", style = MaterialTheme.typography.labelLarge)
        Text(if (device.isPending) "응답 대기 중" else "위치 피드백", style = MaterialTheme.typography.labelSmall)
    }
    ControlSlider(
        value = value,
        onValueChange = { value = it },
        onValueChangeFinished = { onCurtainPositionChange(device.id, value.toInt()) },
        enabled = device.isOnline && !device.isPending,
        accentColor = Color(0xFFF5B942),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { onCurtainPositionChange(device.id, 100) },
            label = { Text("열기") },
            enabled = !device.isPending,
            colors = curtainChipColors(isOpen),
        )
        AssistChip(
            onClick = { onCurtainPositionChange(device.id, 0) },
            label = { Text("닫기") },
            enabled = !device.isPending,
            colors = curtainChipColors(isClosed),
        )
        AssistChip(
            onClick = { onCurtainPositionChange(device.id, device.position ?: 0) },
            label = { Text("정지") },
            enabled = !device.isPending,
            colors = curtainChipColors(!isOpen && !isClosed),
        )
    }
}

@Composable
private fun curtainChipColors(selected: Boolean) = AssistChipDefaults.assistChipColors(
    containerColor = if (selected) Color(0xFFFFD775) else Color(0xFFF5F1EA),
    labelColor = Color(0xFF4A4035),
)

@Composable
private fun DeviceStatus(device: SmartDevice) {
    val label = when {
        device.isPending -> "기기 응답 대기 중"
        !device.isOnline -> "오프라인"
        device.isOn -> "켜짐 · 동기화됨"
        else -> "꺼짐 · 동기화됨"
    }
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun deviceIcon(device: SmartDevice): WallpadIconType = if (device.id == "room-auxiliary-light") {
    WallpadIconType.TABLE_LAMP
} else if (device.id == "room-light-1" || device.id == "room-light-2") {
    WallpadIconType.CEILING_LIGHT
} else {
    when (device.type) {
        DeviceType.LIGHT -> WallpadIconType.LIGHT
        DeviceType.CURTAIN -> WallpadIconType.CURTAIN
        DeviceType.CAMERA -> WallpadIconType.CAMERA
        DeviceType.SENSOR -> WallpadIconType.THERMOMETER
        DeviceType.SMART_THINGS -> WallpadIconType.PLUG
    }
}
