package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smart_home_wallpad.data.model.HomeMode
import com.example.smart_home_wallpad.data.model.HomeModeSettings
import com.example.smart_home_wallpad.data.model.ModeSetting

@Composable
fun HomeModesScreen(
    modifier: Modifier,
    modeSettings: List<HomeModeSettings>,
    activeMode: HomeMode?,
    onApplyMode: (HomeMode?) -> Unit,
    onModeSettingChange: (HomeMode, ModeSetting, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text("생활 모드", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "모드별로 기기 동작을 정하고 한 번에 적용하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(modeSettings, key = { it.mode }) { settings ->
            HomeModeCard(settings, activeMode == settings.mode, onApplyMode, onModeSettingChange)
        }
        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun HomeModeCard(
    settings: HomeModeSettings,
    isActive: Boolean,
    onApplyMode: (HomeMode?) -> Unit,
    onModeSettingChange: (HomeMode, ModeSetting, Boolean) -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WallpadIcon(settings.mode.icon, Modifier.size(28.dp), Color(0xFF3F4854))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(settings.mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(settings.mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { enabled -> onApplyMode(if (enabled) settings.mode else null) },
                )
            }
            if (isActive) {
                Spacer(Modifier.height(8.dp))
                Text("현재 적용 중", style = MaterialTheme.typography.labelLarge, color = Color(0xFF087F5B))
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFE6E9ED))
            Spacer(Modifier.height(12.dp))
            Text("모드 실행 시", style = MaterialTheme.typography.labelLarge, color = Color(0xFF52616B))
            ModeSettingRow(
                label = "조명",
                description = if (settings.lightsOn) "모든 조명 켜기" else "모든 조명 끄기",
                checked = settings.lightsOn,
                onCheckedChange = { onModeSettingChange(settings.mode, ModeSetting.LIGHTS, it) },
            )
            ModeSettingRow(
                label = "커튼",
                description = if (settings.curtainsOpen) "커튼 열기" else "커튼 닫기",
                checked = settings.curtainsOpen,
                onCheckedChange = { onModeSettingChange(settings.mode, ModeSetting.CURTAINS, it) },
            )
            ModeSettingRow(
                label = "스마트 멀티탭",
                description = if (settings.powerOutletsOn) "전체 전원 켜기" else "전체 전원 끄기",
                checked = settings.powerOutletsOn,
                onCheckedChange = { onModeSettingChange(settings.mode, ModeSetting.POWER_OUTLETS, it) },
            )
        }
    }
}

@Composable
private fun ModeSettingRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val HomeMode.title: String
    get() = when (this) {
        HomeMode.OUTING -> "외출 모드"
        HomeMode.SLEEP -> "수면 모드"
        HomeMode.EXERCISE -> "운동 모드"
    }

private val HomeMode.description: String
    get() = when (this) {
        HomeMode.OUTING -> "집을 비울 때 필요한 기기 상태"
        HomeMode.SLEEP -> "잠들기 전 편안한 환경"
        HomeMode.EXERCISE -> "운동에 맞춘 밝고 열린 환경"
    }

private val HomeMode.icon: WallpadIconType
    get() = when (this) {
        HomeMode.OUTING -> WallpadIconType.OUTING
        HomeMode.SLEEP -> WallpadIconType.SLEEP
        HomeMode.EXERCISE -> WallpadIconType.EXERCISE
    }
