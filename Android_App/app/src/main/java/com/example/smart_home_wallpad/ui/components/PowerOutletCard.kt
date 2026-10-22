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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun PowerOutletCard(
    outlet: PowerOutlet,
    onNameSave: (String, String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember(outlet.id) { mutableStateOf(false) }
    var draftName by remember(outlet.id) { mutableStateOf(outlet.connectedDeviceName) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF8FAFB)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WallpadIcon(WallpadIconType.POWER_STRIP, Modifier.size(30.dp), Color(0xFF425466))
                Spacer(Modifier.width(12.dp))
                Text(
                    "스마트 멀티탭 ${outlet.slot}구",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = outlet.isOn,
                    onCheckedChange = { onToggle(outlet.id) },
                    enabled = outlet.isOnline && !outlet.isPending,
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F7FA))
                    .padding(14.dp),
            ) {
                Text("연결된 기기", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isEditing) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("기기 이름 입력") },
                        singleLine = true,
                        enabled = !outlet.isPending,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                onNameSave(outlet.id, draftName.trim())
                                isEditing = false
                            },
                            enabled = !outlet.isPending,
                        ) { Text("저장") }
                        TextButton(
                            onClick = {
                                draftName = outlet.connectedDeviceName
                                isEditing = false
                            },
                            enabled = !outlet.isPending,
                        ) { Text("취소") }
                    }
                } else {
                    Text(
                        outlet.connectedDeviceName.ifBlank { "연결된 기기 없음" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (outlet.connectedDeviceName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = { isEditing = true },
                        enabled = !outlet.isPending,
                    ) { Text("기기 이름 설정") }
                }
            }
        }
    }
}
