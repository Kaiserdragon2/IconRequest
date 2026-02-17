package de.kaiserdragon.iconrequest.ui.iconpreview

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import de.kaiserdragon.iconrequest.ui.IconShape
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconGridPreviewViewModel

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

    val context = LocalContext.current

    val iconPackContext = remember(packageName, isDarkMode) {
        try {
            val pkgContext = context.createPackageContext(packageName, 0)

            // Create a new configuration based on the package context
            val config = android.content.res.Configuration(pkgContext.resources.configuration)

            // Force the UI Mode based on your ViewModel state
            val nightMode = if (isDarkMode) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            } else {
                android.content.res.Configuration.UI_MODE_NIGHT_NO
            }

            // Apply the night mode while preserving other bit flags
            config.uiMode = nightMode or (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv())

            // Return a context with the overridden configuration
            pkgContext.createConfigurationContext(config)
        } catch (e: Exception) {
            context
        }
    }

    val targetColorScheme = remember(useSystemDynamic, isDarkMode, primaryColor) {
        if (!useSystemDynamic) {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            viewModel.generateSchemeFromSeed(primaryColor, isDarkMode)
        }
    }

    // Filter icons based on search
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
                            IconCard(iconName, packageName, iconPackContext, selectedShape)
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

    val colorPresets = listOf(
        Color(0xFF6750A4), Color(0xFF386A20), Color(0xFF0061A4),
        Color(0xFFBA1A1A), Color(0xFF006874), Color(0xFF7D5260),
        Color(0xFF825500), Color(0xFF006D3B), Color(0xFF435893),
        Color(0xFF984061), Color(0xFF6B5E00), Color(0xFF005AC1)
    )

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
            Text("Preview Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            // Appearance Toggles
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = isDarkMode,
                    onClick = { viewModel.toggleDarkMode() },
                    label = { Text("Dark Mode") },
                    leadingIcon = {
                        Icon(
                            if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            null
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = useSystemDynamic,
                    onClick = { viewModel.setUseSystemDynamic(!useSystemDynamic) },
                    label = { Text("Custom Theme") }
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Primary Color", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                colorPresets.forEach { primary ->
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primary)
                            .clickable {
                                viewModel.updateColors(
                                    primary,
                                    primary
                                )
                            }
                            .border(
                                width = if (viewModel.primaryColor.value == primary) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            ),contentAlignment = Alignment.Center
                    ){
                        if (currentPrimary == primary) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Icon Shape", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconShape.entries.forEach { shape ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateShape(shape) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(getShape(shape))
                                .background(if (selectedShape == shape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(shape.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun IconCard(
    iconName: String,
    packageName: String,
    iconPackContext: Context,
    shape: IconShape
) {
    val colorScheme = MaterialTheme.colorScheme
    val currentShape = getShape(shape)

    val themedDrawable = remember(iconName, colorScheme,iconPackContext) {
        try {
            val res = iconPackContext.resources
            val id = res.getIdentifier(iconName, "drawable", packageName)

            if (id != 0) {
                // Use the iconPackContext theme for initial load
                val drawable = res.getDrawable(id, iconPackContext.theme)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                    // Inject the generated theme colors directly into the layers
                    drawable.background.mutate().setTint(colorScheme.primaryContainer.toArgb())
                    drawable.foreground.mutate().setTint(colorScheme.primary.toArgb())
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
                    // SHAPE INJECTION: We apply the mask here in the UI layer
                    .clip(currentShape),
                    //.background(colorScheme.primaryContainer),
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
fun getShape(shape: IconShape) = when (shape) {
    IconShape.Square -> RectangleShape
    IconShape.Circle -> CircleShape
    IconShape.Squircle -> RoundedCornerShape(25.dp)
    IconShape.RoundedSquare -> RoundedCornerShape(8.dp)
}