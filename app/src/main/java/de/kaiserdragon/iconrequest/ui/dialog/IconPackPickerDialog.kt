package de.kaiserdragon.iconrequest.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import de.kaiserdragon.iconrequest.data.AppInfo
import de.kaiserdragon.iconrequest.data.SYSTEM_FILTER_PACKAGE

@Composable
fun IconPackPickerDialog(
    iconPacks: List<AppInfo>,
    title: String = "Select Icon Pack",
    showResetOption: Boolean = true,
    showSystemOption: Boolean = false,
    onDismiss: () -> Unit,
    onPackSelected: (AppInfo?) -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        // Use platform-specific properties to allow the dialog to be wider
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f) // Use 92% of screen width
                .fillMaxHeight(0.8f), // Limit height to 80% of screen
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // --- Header ---
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        // Slide in from right, slide out to left
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "TitleAnimation"
                ) { targetTitle ->
                    Text(
                        text = targetTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                // --- List Content ---
                LazyColumn(
                    modifier = Modifier.weight(1f), // Takes up available space between header/footer
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (showResetOption) {
                        item {
                            ListItem(
                                modifier = Modifier.clickable { onPackSelected(null) },
                                headlineContent = {
                                    Text("Show All Apps", fontWeight = FontWeight.Bold)
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.FilterListOff,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                    if (showSystemOption) {
                        val defaultIcon = context.packageManager.defaultActivityIcon
                        item {
                            ListItem(
                                modifier = Modifier.clickable {
                                    // We pass a dummy AppInfo or a specific "System" flag
                                    // For now, let's assume we pass an AppInfo with a specific package
                                    onPackSelected(
                                        AppInfo(
                                            name = "System Apps",
                                            packageName = SYSTEM_FILTER_PACKAGE,
                                            activityName = "",
                                            // Use a system drawable as a placeholder to satisfy the data class
                                            icon = defaultIcon,
                                            isSelected = false
                                        )
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        "System Apps",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = defaultIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .padding(4.dp)
                                    )
                                }
                            )
                        }
                    }
                    items(iconPacks) { pack ->
                        ListItem(
                            modifier = Modifier.clickable { onPackSelected(pack) },
                            headlineContent = { Text(pack.name) },
                            supportingContent = {
                                Text(
                                    pack.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = pack.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(4.dp)
                                )
                            }
                        )
                    }
                }

                // --- Footer ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}