package de.kaiserdragon.iconrequest.ui.iconpreview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import coil.compose.rememberAsyncImagePainter
import de.kaiserdragon.iconrequest.ui.IconShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconGridPreviewScreen(
    packageName: String,
    viewModel: IconGridPreviewViewModel,
    onBack: () -> Unit
) {
    val selectedShape by viewModel.selectedShape.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val allIcons by viewModel.iconList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val primaryColor by viewModel.primaryColor.collectAsState()
    val useSystemDynamic by viewModel.useSystemDynamic.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val contrast by viewModel.contrast.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val foregroundRole by viewModel.foregroundColorRole.collectAsState()
    val backgroundRole by viewModel.backgroundColorRole.collectAsState()

    val context = LocalContext.current

    val iconPackContext = remember(packageName, isDarkMode) {
        try {
            val pkgContext = context.createPackageContext(packageName, 0)
            val config = android.content.res.Configuration(pkgContext.resources.configuration)
            val nightMode = if (isDarkMode) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            } else {
                android.content.res.Configuration.UI_MODE_NIGHT_NO
            }
            config.uiMode = nightMode or (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())
            pkgContext.createConfigurationContext(config)
        } catch (e: Exception) {
            context
        }
    }
    val targetColorScheme = remember(useSystemDynamic, isDarkMode, primaryColor,contrast,themeStyle) {
        if (!useSystemDynamic) {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            // Use the new HCT-based generation
            viewModel.generateScheme(primaryColor, contrast,isDarkMode,
                themeStyle
            )
        }
    }
    val filteredIcons = remember(searchQuery, allIcons) {
        if (searchQuery.isEmpty()) allIcons
        else allIcons.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    MaterialTheme(colorScheme = targetColorScheme) {
        Surface {
            LaunchedEffect(packageName) {
                viewModel.loadIconPreview(packageName)
            }
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("Icon Browser") },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Palette, contentDescription = "Customize")
                                }
                            }
                        )
                        // Search Bar
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            placeholder = { Text("Search icons...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            ) { padding ->
                if (isRefreshing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 90.dp),
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredIcons, key = { it }) { iconName ->
                            IconCard(
                                viewModel,
                                iconName,
                                packageName,
                                iconPackContext,
                                selectedShape,
                                foregroundRole,
                                backgroundRole
                            )
                        }
                    }

                }
                if (showSettings) {
                    SettingsBottomSheet(
                        viewModel = viewModel,
                        onDismiss = { showSettings = false },
                        sheetState = sheetState
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: IconGridPreviewViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val selectedShape by viewModel.selectedShape.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val useSystemDynamic by viewModel.useSystemDynamic.collectAsState()
    val currentPrimary by viewModel.primaryColor.collectAsState()
    val contrast by viewModel.contrast.collectAsState()
    var showColorWheel by remember { mutableStateOf(false) }
    val themeStyle by viewModel.themeStyle.collectAsState()

    val colorPresets = remember {
        listOf(
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFFF44336), // Red
            Color(0xFF9C27B0)  // Purple
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = isDarkMode != viewModel.initialDarkMode,
                    onClick = { viewModel.toggleDarkMode() },
                    label = {
                        Text(if (isDarkMode) "Dark Mode" else "Light Mode")
                    },
                    leadingIcon = {
                        Icon(
                            if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = (useSystemDynamic),
                    onClick = { viewModel.setUseSystemDynamic(!useSystemDynamic) },
                    label = { Text("Custom Theme") }
                )
            }

            if (useSystemDynamic) {
                Text("Theme Style", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = themeStyle == style,
                            onClick = { viewModel.setThemeStyle(style) },
                            label = { Text(style.name.replace("_", " ").lowercase().capitalize()) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Seed Color", style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { showColorWheel = !showColorWheel }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Custom Color",
                            tint = if (showColorWheel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (showColorWheel) {
                    ColorPickerFull(
                        selectedColor = currentPrimary,
                        contrast = contrast,
                        onColorChanged = { viewModel.updateColors(it, it) },
                        onContrastChanged = { viewModel.updateContrast(it) }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                    )
                    {
                        colorPresets.forEach { presetColor ->
                            val isSelected = currentPrimary == presetColor
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(presetColor)
                                    .clickable {
                                        // Update via standard Color
                                        viewModel.updateColors(presetColor, presetColor)
                                    }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = viewModel.getContrastColor(currentPrimary),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                var showLayerOverlay by remember { mutableStateOf(false) }
                Button(
                    onClick = { showLayerOverlay = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Palette, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Customize Layer Colors")
                }
                if (showLayerOverlay) {
                    IconColorOverlay(
                        viewModel = viewModel,
                        onDismiss = { showLayerOverlay = false }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Icon Shape", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()), // Allow horizontal scrolling
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconShape.entries.forEach { shapeEntry ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateShape(shapeEntry) }
                            .padding(end = 8.dp, top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp) // Slightly larger for detail
                                .clip(shapeEntry.shape)
                                .background(
                                    if (selectedShape == shapeEntry) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (selectedShape == shapeEntry) Color.Transparent
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = shapeEntry.shape
                                )
                        )
                        Text(shapeEntry.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconColorOverlay(
    viewModel: IconGridPreviewViewModel,
    onDismiss: () -> Unit
) {
    var activeLayerTab by remember { mutableIntStateOf(0) } // 0 = Foreground, 1 = Background
    val foregroundRole by viewModel.foregroundColorRole.collectAsState()
    val backgroundRole by viewModel.backgroundColorRole.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Pick a random icon from the list for the preview
    val allIcons by viewModel.iconList.collectAsState()
    val previewIcon = remember { allIcons.randomOrNull() ?: "" }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Header & Close ---
                TopAppBar(
                    title = { Text("Layer Lab") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Check, contentDescription = "Done")
                        }
                    }
                )

                // --- 1. Live Preview Section ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // We use a simplified version of your IconCard logic here
                        IconPreviewLarge(
                            viewModel = viewModel,
                            iconName = previewIcon,
                            foregroundRole = foregroundRole,
                            backgroundRole = backgroundRole
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Live Preview", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // --- 2. Main Layer Tabs (Foreground vs Background) ---
                TabRow(selectedTabIndex = activeLayerTab) {
                    Tab(
                        selected = activeLayerTab == 0,
                        onClick = { activeLayerTab = 0 },
                        text = { Text("Foreground") }
                    )
                    Tab(
                        selected = activeLayerTab == 1,
                        onClick = { activeLayerTab = 1 },
                        text = { Text("Background") }
                    )
                }

                // --- 3. Sub-Category Selector (Theme, System, Fixed) ---
                val currentRole = if (activeLayerTab == 0) foregroundRole else backgroundRole
                val onRoleChange: (ColorRole) -> Unit = {
                    if (activeLayerTab == 0) viewModel.setForegroundColorRole(it)
                    else viewModel.setBackgroundColorRole(it)
                }

                Box(modifier = Modifier.padding(16.dp)) {
                    ColorRolePicker(
                        selectedRole = currentRole,
                        onRoleSelected = onRoleChange
                    )
                }
            }
        }
    }
}
@Composable
fun IconPreviewLarge(
    viewModel: IconGridPreviewViewModel,
    iconName: String,
    foregroundRole: ColorRole,
    backgroundRole: ColorRole
) {
    val colorScheme = MaterialTheme.colorScheme
    val fgColor = with(viewModel) { foregroundRole.toColor(colorScheme) }
    val bgColor = with(viewModel) { backgroundRole.toColor(colorScheme) }
    val shape by viewModel.selectedShape.collectAsState()

    // Simplified preview box
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(shape.shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // Here you would put your Image logic with the foreground tint
        // For the sake of the preview, we show the foreground role label
        Text(
            text = "FG",
            color = fgColor,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
@Composable
fun IconCard(
    viewModel: IconGridPreviewViewModel,
    iconName: String,
    packageName: String,
    iconPackContext: Context,
    shapeEntry: IconShape,
    foregroundRole: ColorRole,
    backgroundRole: ColorRole
) {
    val colorScheme = MaterialTheme.colorScheme
    val useSystemDynamic by viewModel.useSystemDynamic.collectAsState()

    // Resolve the actual colors based on user selection
    val fgColor = with(viewModel) { foregroundRole.toColor(colorScheme) }
    val bgColor = with(viewModel) { backgroundRole.toColor(colorScheme) }

    val themedDrawable = remember(iconName, colorScheme,iconPackContext,fgColor,bgColor) {
        try {
            val res = iconPackContext.resources
            val id = res.getIdentifier(iconName, "drawable", packageName)

            if (id != 0) {
               val drawable = ResourcesCompat.getDrawable(res,id,iconPackContext.theme)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable && useSystemDynamic) {
                    // Inject the generated theme colors directly into the layers
                    drawable.background.mutate().setTint(bgColor.toArgb())
                    drawable.foreground.mutate().setTint(fgColor.toArgb())
                }
                drawable
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(shapeEntry.shape),
                contentAlignment = Alignment.Center
            ) {
               when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && themedDrawable is AdaptiveIconDrawable -> {
                        // Background Layer
                        Image(
                            painter = rememberAsyncImagePainter(themedDrawable.background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        // Foreground Layer
                        Image(
                            painter = rememberAsyncImagePainter(themedDrawable.foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.5f),
                            contentScale = ContentScale.FillBounds
                        )
                   }

                    themedDrawable != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(themedDrawable),
                            contentDescription = iconName,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = iconName.replace("_", " ").trim(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
           )
        }
    }
}

@Composable
fun ColorPickerFull(
    selectedColor: Color,
    contrast: Float,
    onColorChanged: (Color) -> Unit,
    onContrastChanged: (Float) -> Unit
) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ColorWheel(
            selectedColor = selectedColor,
            onColorChanged = onColorChanged,
            modifier = Modifier.size(200.dp).padding(2.dp)
        )
        Text(
            text = "Seed: #${Integer.toHexString(selectedColor.toArgb()).uppercase()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Contrast: ${if (contrast > 0) "+" else ""}${String.format("%.1f", contrast)}",
            style = MaterialTheme.typography.labelSmall
        )
        Slider(
            value = contrast,
            onValueChange = onContrastChanged,
            valueRange = -1f..1f,
            steps = 19,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Soft", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Standard", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("High", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

