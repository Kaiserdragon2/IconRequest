package de.kaiserdragon.iconrequest.ui.iconcomparison

import android.content.ComponentName
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.kaiserdragon.iconrequest.data.AppInfo
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.data.MAX_REQUEST_SIZE
import de.kaiserdragon.iconrequest.data.SYSTEM_FILTER_PACKAGE
import de.kaiserdragon.iconrequest.ui.settings.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconComparisonScreen(
    viewModel: IconComparisonViewModel,
    settingsViewModel: SettingsViewModel,
    allApps: List<AppInfo>,
    packAPackage: String,
    packBPackage: String,
    onBack: () -> Unit
) {
    val results by viewModel.filteredResults.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val fullPackMode by viewModel.fullPackMode.collectAsState()

    val statsA by viewModel.statsA.collectAsState()
    val statsB by viewModel.statsB.collectAsState()

    // Inside IconComparisonScreen.kt
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val excludeTags by settingsViewModel.excludeResourcesTag.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val allFilteredSelected = remember(results, selectedIds) {
        results.isNotEmpty() && results.all { selectedIds.contains("${it.packageName}|${it.activityName}") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (!isSearchActive) {
                        IconButton(onClick = {
                            if (isSearchActive) isSearchActive = false else onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        // Search Input Field
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search apps...") },
                            modifier = Modifier
                                .fillMaxWidth(),
                            //.focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Comparison")
                    }
                },
                actions = {
                    // Search toggle button
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")// Clear search when closing
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Toggle Search"
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = {
                        viewModel.toggleAllVisible(results, !allFilteredSelected)
                    }) {
                        Icon(
                            imageVector = if (allFilteredSelected) Icons.Default.SelectAll else Icons.Default.Deselect,
                            contentDescription = "Select Filtered"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedIds.isNotEmpty()) {
                var showMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val isOverLimit = selectedIds.size > MAX_REQUEST_SIZE

                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (isOverLimit) {
                                Toast.makeText(
                                    context,
                                    "Selection too large (Max $MAX_REQUEST_SIZE)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showMenu = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                        text = { Text("Request (${selectedIds.size})") }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share ZIP") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                showMenu = false; viewModel.shareRequest(
                                context,
                                packAPackage,
                                packBPackage
                            )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save ZIP to Downloads") },
                            leadingIcon = { Icon(Icons.Default.Save, null) },
                            onClick = {
                                showMenu = false; viewModel.saveZipToDevice(
                                context,
                                packAPackage,
                                packBPackage
                            )
                            }
                        )
                        HorizontalDivider()// Visual separator
                        DropdownMenuItem(
                            text = { Text("Copy AppFilter Text") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                showMenu = false; viewModel.copyToClipboard(
                                context,
                                excludeTags,
                                packAPackage,
                                packBPackage
                            )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Text Only") },
                            leadingIcon = { Icon(Icons.Default.Description, null) },
                            onClick = {
                                showMenu = false; viewModel.shareAsText(
                                context,
                                excludeTags,
                                packAPackage,
                                packBPackage
                            )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            ComparisonStatsCard(
                results = results,
                totalIconsA = statsA.uniqueIconsDesigned,
                totalIconsB = statsB.uniqueIconsDesigned,
                uniqueUsedA = statsA.uniqueIconsUsed,
                uniqueUsedB = statsB.uniqueIconsUsed,
                packAPackage,
                packBPackage
            )
            if (isProcessing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // 2. The Comparison List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { result ->
                        // Create the reference ID
                        val resultId = viewModel.getResultId(result)

                        // Check if this ID is in the selected set
                        val isSelected by viewModel.selectedIds.collectAsState()
                        val itemIsSelected = isSelected.contains(resultId)

                        ComparisonItem(
                            result = result,
                            packAPackage = packAPackage,
                            packBPackage = packBPackage,
                            isSelected = itemIsSelected, // Pass the calculated boolean
                            onToggle = { viewModel.toggleSelection(result) } // Pass the toggle action
                        )
                    }
                }
            }
            // 2. Bottom Sheet for Filters and Toggle
            if (showFilterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = sheetState
                ) {
                    FilterSettingsContent(
                        filterMode = filterMode,
                        fullPackMode = fullPackMode,
                        onFilterSelected = { viewModel.setFilter(it) },
                        onToggleFullPack = {
                            viewModel.toggleMode(
                                packAPackage,
                                packBPackage,
                                allApps
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSettingsContent(
    filterMode: ComparisonFilter,
    fullPackMode: Boolean,
    onFilterSelected: (ComparisonFilter) -> Unit,
    onToggleFullPack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        Text("Settings & Filters", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // Full Pack Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Full Pack Mode", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Compare all icons regardless of installed apps",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = fullPackMode,
                onCheckedChange = { onToggleFullPack() }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Filter Results", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonFilter.entries.forEach { mode ->
                FilterChip(
                    selected = filterMode == mode,
                    onClick = { onFilterSelected(mode) },
                    label = {
                        // Cleaner way to format the enum names
                        val label = mode.name
                            .replace("_", " ")
                            .lowercase()
                            .split(" ")
                            .joinToString(" ") { word ->
                                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }
                        Text(label)
                    }
                )
            }
        }
    }
}

@Composable
fun ComparisonIcon(
    drawableName: String?,
    packageName: String,
    activityName: String,
    iconPackPackageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconModel = remember(drawableName, iconPackPackageName, packageName, activityName) {
        if (iconPackPackageName == SYSTEM_FILTER_PACKAGE) {
            // Path A: Load directly from System
            try {
                context.packageManager.getActivityIcon(
                    ComponentName(packageName, activityName)
                )
            } catch (e: Exception) {
                null
            }
        } else {
            // Path B: Resolve resource ID from Icon Pack
            if (drawableName == null) return@remember null
            try {
                val packRes = context.packageManager.getResourcesForApplication(iconPackPackageName)
                val id = packRes.getIdentifier(drawableName, "drawable", iconPackPackageName)
                if (id != 0) "android.resource://$iconPackPackageName/$id" else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        if (iconModel == null) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Missing",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun ComparisonItem(
    result: IconPackManager.ComparisonResult,
    packAPackage: String,
    packBPackage: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val packAMissing = result.packAIcon == null
    val packBMissing = result.packBIcon == null
    val isPartialMatch = packAMissing xor packBMissing
    val isBothMissing = packAMissing && packBMissing
    packAMissing || packBMissing

    val displayName = remember(result.appName, result.packageName) {
        result.appName.ifEmpty {
            try {
                val info = context.packageManager.getApplicationInfo(result.packageName, 0)
                context.packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                result.packageName // Fallback to com.package.name
            }
        }
    }

    val borderColor = when {
        isPartialMatch -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f) // Warning color
        isBothMissing -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else -> null
    }

    val containerColor = when {
        isPartialMatch -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        isBothMissing -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else if (borderColor != null) BorderStroke(1.dp, borderColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pack A Icon (Left)
            ComparisonIcon(
                result.packAIcon,
                result.packageName,
                result.activityName,
                packAPackage,
                Modifier.weight(1f)
            )

            // App Details (Center)
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Optional: Show activity if it's a specific component
                if (result.activityName.isNotEmpty()) {
                    Text(
                        text = result.activityName.substringAfterLast("."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
            // Pack B Icon (Right)
            ComparisonIcon(
                result.packBIcon,
                result.packageName,
                result.activityName,
                packBPackage,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ComparisonHeader(
    packAPackage: String,
    packBPackage: String
) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Resolve pack info (Icon and Name)
    val packA = remember(packAPackage) { pm.getApplicationInfo(packAPackage, 0) }
    val packB = remember(packBPackage) { pm.getApplicationInfo(packBPackage, 0) }

    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pack A Info
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = pm.getApplicationIcon(packA),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = pm.getApplicationLabel(packA).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // VS Spacer
            Text(
                text = "VS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Pack B Info
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = pm.getApplicationIcon(packB),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = pm.getApplicationLabel(packB).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ComparisonStatsCard(
    results: List<IconPackManager.ComparisonResult>,
    totalIconsA: Int,
    totalIconsB: Int,
    uniqueUsedA: Int,
    uniqueUsedB: Int,
    packAPackage: String,
    packBPackage: String
) {
    if (results.isEmpty() && totalIconsA == 0) return

    // State for collapsing
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val currentVisibleTotal = results.size
    val coveredInA = remember(results) { results.count { it.packAIcon != null } }
    val coveredInB = remember(results) { results.count { it.packBIcon != null } }
    val common =
        remember(results) { results.count { it.packAIcon != null && it.packBIcon != null } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(), // Smoothly resizes the card when content disappears
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        onClick = { isExpanded = !isExpanded } // Entire card acts as a toggle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ALWAYS VISIBLE: Header and Title
            ComparisonHeader(packAPackage = packAPackage, packBPackage = packBPackage)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Global Statistics",
                    style = MaterialTheme.typography.titleMedium,
                )
                // Visual cue for expand/collapse
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // COLLAPSIBLE SECTION
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Icons",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DetailedStatColumn(totalIconsA, uniqueUsedA)
                        DetailedStatColumn(totalIconsB, uniqueUsedB)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    Text(
                        "Current Filter Coverage",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "", value = coveredInA, total = currentVisibleTotal)
                        StatItem(label = "Common", value = common, total = currentVisibleTotal)
                        StatItem(label = "", value = coveredInB, total = currentVisibleTotal)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedStatColumn(total: Int, uniqueUsed: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${String.format("%,d", total)} Total", style = MaterialTheme.typography.bodyMedium)
        Text(
            "${String.format("%,d", uniqueUsed)} Used",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatItem(label: String, value: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        val percent = if (total > 0) (value * 100 / total) else 0
        Text(text = "$percent%", style = MaterialTheme.typography.labelSmall)
    }
}