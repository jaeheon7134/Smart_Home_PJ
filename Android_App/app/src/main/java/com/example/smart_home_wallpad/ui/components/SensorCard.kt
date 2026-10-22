package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smart_home_wallpad.data.model.SensorReading

@Composable
fun SensorCard(reading: SensorReading, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                WallpadIcon(WallpadIconType.INDOOR_ENVIRONMENT, tint = Color(0xFFC58A35))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Column {
                    Text("실내 환경", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("온도와 습도", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SensorValue(
                    label = "온도",
                    value = "${reading.temperature}°C",
                    icon = WallpadIconType.TEMPERATURE,
                    backgroundColor = Color(0xFFFBF6EE),
                    valueColor = Color(0xFF9A622B),
                    modifier = Modifier.weight(1f),
                )
                SensorValue(
                    label = "습도",
                    value = "${reading.humidity.toInt()}%",
                    icon = WallpadIconType.HUMIDITY,
                    backgroundColor = Color(0xFFFBF6EE),
                    valueColor = Color(0xFF7A8070),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (reading.isOnline) "센서 연결됨 · ${reading.updatedAt}" else "센서 오프라인",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SensorValue(
    label: String,
    value: String,
    icon: WallpadIconType,
    backgroundColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            WallpadIcon(icon, modifier = Modifier.size(20.dp).padding(end = 4.dp), tint = valueColor)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
