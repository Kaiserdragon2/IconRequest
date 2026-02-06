package de.kaiserdragon.iconrequest.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ... your imports ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val currentTheme by settingsViewModel.themeSetting.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val useDynamicColors by settingsViewModel.useDynamicColors.collectAsState()
    val excludeResourcesTag by settingsViewModel.excludeResourcesTag.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            ListItem(
                headlineContent = { Text("App Theme") },
                supportingContent = { Text("Currently: ${currentTheme.name}") },
                leadingContent = { Icon(Icons.Default.Palette, null) },
                modifier = Modifier.clickable { showDialog = true }
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.height(IntrinsicSize.Min),
                headlineContent = { Text("Dynamic Colors") },
                supportingContent = { Text("Use colors from wallpaper (Android 12+)") },
                trailingContent = {
                    Switch(
                        checked = useDynamicColors,
                        onCheckedChange = { settingsViewModel.setDynamicColors(it) }
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight() // Fill the height determined by the text
                            .wrapContentWidth(), // Only take as much width as the icon needs
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null
                        )
                    }

                },
            )
            HorizontalDivider()
            Text(
                text = "Sharing",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            ListItem(
                modifier = Modifier.height(IntrinsicSize.Min),
                headlineContent = { Text("Pure XML Export") },
                supportingContent = { Text("Exclude <resources> tags when copying/sharing") },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight() // Fill the height determined by the text
                            .wrapContentWidth(), // Only take as much width as the icon needs
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null
                        )
                    }

                },
                trailingContent = {
                    Switch(
                        checked = excludeResourcesTag,
                        onCheckedChange = { settingsViewModel.setExcludeResourcesTag(it) }
                    )
                }
            )
            HorizontalDivider()
            Text(
                text = "Experimental",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                modifier = Modifier.height(IntrinsicSize.Min),
                headlineContent = { Text("Show System Apps") },
                supportingContent = { Text("Include all system activities in the list") },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = null)
                    }
                },
                trailingContent = {
                    val showSystemApps by settingsViewModel.showSystemApps.collectAsState()
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { settingsViewModel.setShowSystemApps(it) }
                    )
                }
            )

            HorizontalDivider()

            ListItem(
                modifier = Modifier.height(IntrinsicSize.Min),
                headlineContent = { Text("Show Widgets") },
                supportingContent = { Text("List app widgets instead of launcher activities") },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null)
                    }
                },
                trailingContent = {
                    val showWidgets by settingsViewModel.showWidgets.collectAsState()
                    Switch(
                        checked = showWidgets,
                        onCheckedChange = { settingsViewModel.setShowWidgets(it) }
                    )
                }
            )

            HorizontalDivider()
        }
    }

    if (showDialog) {
        ThemePickerDialog(
            currentTheme = currentTheme,
            onDismiss = { showDialog = false },
            onSelect = { selectedTheme ->
                settingsViewModel.setTheme(selectedTheme)
                showDialog = false
            }
        )
    }
}

@Composable
fun ThemePickerDialog(
    currentTheme: AppThemeSetting, // Use the Enum type here
    onDismiss: () -> Unit,
    onSelect: (AppThemeSetting) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                // entries is the modern replacement for values() in Kotlin enums
                AppThemeSetting.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null // Row handles the click
                        )
                        Text(
                            text = theme.name,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}