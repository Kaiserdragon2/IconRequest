package de.kaiserdragon.iconrequest.ui.iconpreview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun ColorRolePicker(
    selectedRole: ColorRole,
    onRoleSelected: (ColorRole) -> Unit
) {
    var subTabIndex by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Theme", "System", "Fixed")

    Column {
        androidx.compose.material3.SecondaryTabRow(
            selectedTabIndex = subTabIndex,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subTabIndex == index,
                    onClick = { subTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content Area
        Box(modifier = Modifier.fillMaxWidth()) {
            when (subTabIndex) {
                0 -> ThemeRolesGrid(selectedRole, onRoleSelected)
                1 -> SystemRolesPager(selectedRole, onRoleSelected)
                2 -> FixedRolesGrid(selectedRole, onRoleSelected)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ThemeRolesGrid(selectedRole: ColorRole, onRoleSelected: (ColorRole) -> Unit) {
    // Define the groups and their associated keywords
    val groupedRoles = remember {
        val allThemeRoles = ColorRole.entries.filter { !it.name.startsWith("SYS_") }

        linkedMapOf(
            "Primary" to allThemeRoles.filter { it.name.contains("PRIMARY") },
            "Secondary" to allThemeRoles.filter { it.name.contains("SECONDARY") },
            "Tertiary" to allThemeRoles.filter { it.name.contains("TERTIARY") },
            "Surface" to allThemeRoles.filter { it.name.contains("SURFACE") || it.name.contains("BACKGROUND") },
            "Error" to allThemeRoles.filter { it.name.contains("ERROR") },
            "Utility" to allThemeRoles.filter { it.name.contains("OUTLINE") || it.name.contains("SCRIM") }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedRoles.forEach { (groupName, roles) ->
            if (roles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Category Label
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // The FlowRow for chips in this category
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        roles.forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { onRoleSelected(role) },
                                label = {
                                    Text(
                                        text = role.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemRolesPager(selectedRole: ColorRole, onRoleSelected: (ColorRole) -> Unit) {
    // Reusing the logic from your previous SystemColorSelector but inside the tab
    SystemColorSelector(selectedRole, onRoleSelected)
}

@Composable
fun FixedRolesGrid(selectedRole: ColorRole, onRoleSelected: (ColorRole) -> Unit) {
    Text("Placeholder! Not implemented Yet")
}
@Composable
fun SystemColorSelector(
    selectedRole: ColorRole,
    onRoleSelected: (ColorRole) -> Unit
) {
    // Mapping display names to enum prefixes
    val paletteMap = remember {
        linkedMapOf(
            "Accent 1" to "A1",
            "Accent 2" to "A2",
            "Accent 3" to "A3",
            "Neutral 1" to "N1",
            "Neutral 2" to "N2"
        )
    }
    val tones = remember { listOf("0", "10", "50", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000") }

    // Sync palette state with selectedRole
    val selectedPaletteKey = remember(selectedRole) {
        paletteMap.entries.find { selectedRole.name.startsWith("SYS_${it.value}") }?.key ?: "Accent 1"
    }

    // Sync tone index with selectedRole
    val currentToneIndex = remember(selectedRole) {
        val tonePart = selectedRole.name.substringAfterLast("_")
        tones.indexOf(tonePart).coerceAtLeast(0).toFloat()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- Palette Selection ---
        Text("System Palette", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paletteMap.forEach { (displayName, prefix) ->
                val isSelected = selectedPaletteKey == displayName
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newRoleName = "SYS_${prefix}_${tones[currentToneIndex.toInt()]}"
                        onRoleSelected(ColorRole.valueOf(newRoleName))
                    },
                    label = { Text(displayName) }
                )
            }
        }

        // --- Tone Slider ---
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                // Display the current tone value as a Badge-style text
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = tones[currentToneIndex.toInt()],
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Slider(
                value = currentToneIndex,
                onValueChange = { index ->
                    val toneValue = tones[index.toInt()]
                    val prefix = paletteMap[selectedPaletteKey]
                    val newRoleName = "SYS_${prefix}_$toneValue"
                    onRoleSelected(ColorRole.valueOf(newRoleName))
                },
                valueRange = 0f..(tones.size - 1).toFloat(),
                steps = tones.size - 2 // This creates "snap" points for each tone
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Light", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("Dark", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}