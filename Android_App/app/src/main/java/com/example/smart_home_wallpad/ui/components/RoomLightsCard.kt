package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smart_home_wallpad.data.model.SmartDevice

@Composable
fun RoomLightsCard(
    lights: List<SmartDevice>,
    onToggle: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WallpadIcon(WallpadIconType.CEILING_LIGHT, Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text("방 조명", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                lights.forEach { light ->
                    RoomLightControl(
                        light = light,
                        onToggle = onToggle,
                        onBrightnessChange = onBrightnessChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomLightControl(
    light: SmartDevice,
    onToggle: (String) -> Unit,
    onBrightnessChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var brightness by remember(light.id, light.brightness) { mutableStateOf((light.brightness ?: 0).toFloat()) }

    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFBF6EE))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(light.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = light.isOn,
                onCheckedChange = { onToggle(light.id) },
                enabled = light.isOnline && !light.isPending,
            )
        }
        if (light.supportsBrightness) {
            Text("밝기 ${brightness.toInt()}%", style = MaterialTheme.typography.labelMedium)
            ControlSlider(
                value = brightness,
                onValueChange = { brightness = it },
                onValueChangeFinished = { onBrightnessChange(light.id, brightness.toInt()) },
                enabled = light.isOnline && !light.isPending,
                accentColor = Color(0xFFF5B942),
            )
        }
    }
}
