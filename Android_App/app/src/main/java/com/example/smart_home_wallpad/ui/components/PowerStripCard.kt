package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.smart_home_wallpad.data.model.PowerOutlet

@Composable
fun PowerStripCard(
    outlets: List<PowerOutlet>,
    onNameSave: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WallpadIcon(WallpadIconType.POWER_STRIP, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("스마트 멀티탭", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "1~3구에 연결할 기기를 기록하고, 각 구의 전원만 제어합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            outlets.sortedBy { it.slot }.forEach { outlet ->
                PowerOutletRow(outlet, onNameSave, onToggle)
            }
        }
    }
}

@Composable
private fun PowerOutletRow(
    outlet: PowerOutlet,
    onNameSave: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    var isEditing by remember(outlet.id) { mutableStateOf(false) }
    var draftName by remember(outlet.id) { mutableStateOf(outlet.connectedDeviceName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F7FA))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${outlet.slot}구", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(10.dp))
            Text(
                text = outlet.connectedDeviceName.ifBlank { "연결 기기 미지정" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (outlet.connectedDeviceName.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Switch(
                checked = outlet.isOn,
                onCheckedChange = { onToggle(outlet.id) },
                enabled = outlet.isOnline && !outlet.isPending,
            )
        }
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("연결할 기기 입력") },
                    singleLine = true,
                    enabled = !outlet.isPending,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onNameSave(outlet.id, draftName.trim())
                        isEditing = false
                    },
                    enabled = !outlet.isPending,
                ) { Text("저장") }
            }
            TextButton(
                onClick = {
                    draftName = outlet.connectedDeviceName
                    isEditing = false
                },
                enabled = !outlet.isPending,
            ) { Text("취소") }
        } else {
            TextButton(
                onClick = {
                    draftName = outlet.connectedDeviceName
                    isEditing = true
                },
                enabled = !outlet.isPending,
            ) { Text("수정") }
        }
    }
}
