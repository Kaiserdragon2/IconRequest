package de.kaiserdragon.iconrequest.ui.iconpreview

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconPackHealthViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale

import coil.size.Size
import de.kaiserdragon.iconrequest.ui.IconShape
import de.kaiserdragon.iconrequest.ui.iconpackhealth.IconGridPreviewViewModel

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconGridPreviewScreen(
    packageName: String,
    viewModel: IconGridPreviewViewModel,
    onBack: () -> Unit
) {
    val selectedShape by viewModel.selectedShape.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val testColor by viewModel.testColor.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val allIcons by viewModel.iconList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val primaryColor by viewModel.primaryColor.collectAsState()
    val containerColor by viewModel.containerColor.collectAsState()
    val useSystemDynamic by viewModel.useSystemDynamic.collectAsState()

    val targetColorScheme = when {
        !useSystemDynamic -> if (isDarkMode) dynamicDarkColorScheme(LocalContext.current)
        else dynamicLightColorScheme(LocalContext.current)
        else -> if (isDarkMode) {
            darkColorScheme(primary = primaryColor, primaryContainer = containerColor)
        } else {
            lightColorScheme(primary = primaryColor, primaryContainer = containerColor)
        }
    }

    // Filter icons based on search
    val filteredIcons = remember(searchQuery, allIcons) {
        if (searchQuery.isEmpty()) allIcons
        else allIcons.filter { it.contains(searchQuery, ignoreCase = true) }
    }

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
                        IconButton(onClick = { viewModel.setUseSystemDynamic(!useSystemDynamic) }) {
                            Icon(
                                Icons.Default.Palette, // You'll need to import this
                                contentDescription = "Toggle Dynamic",
                                tint = if (useSystemDynamic) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        // Shape Toggle Button
                        IconButton(onClick = {
                            val nextShape = IconShape.values()[
                                (selectedShape.ordinal + 1) % IconShape.values().size
                            ]
                            viewModel.updateShape(nextShape)
                        }) {
                            Icon(Icons.Default.Category, contentDescription = "Change Shape")
                        }
                    }
                )
                if (useSystemDynamic) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val presets = listOf(
                            Color(0xFF6750A4) to Color(0xFFEADDFF),
                            Color(0xFF386A20) to Color(0xFFB7F397),
                            Color(0xFF0061A4) to Color(0xFFD1E4FF)
                        )

                        presets.forEach { (primary, container) ->
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(container)
                                    .border(2.dp, primary, CircleShape)
                                    .clickable { viewModel.updateColors(primary, container) }
                            )
                        }
                    }
                }
                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search icons...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Text(
                    text = "Current Shape: ${selectedShape.label}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        if (isRefreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {MaterialTheme(colorScheme = targetColorScheme) {
            Surface() {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredIcons, key = { it }) { iconName ->
                        IconCard(iconName, packageName, selectedShape)
                    }
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
        shape: IconShape
    ) {
        val context = LocalContext.current
        val primary = MaterialTheme.colorScheme.primary
        val container = MaterialTheme.colorScheme.primaryContainer
        val iconPackContext = remember(packageName) {
            try { context.createPackageContext(packageName, 0) } catch (e: Exception) { context }
        }

        val drawable = remember(iconName, packageName) {
            try {
                val res = iconPackContext.resources
                val id = res.getIdentifier(iconName, "drawable", packageName)
                if (id == 0) return@remember null
                res.getDrawable(id, iconPackContext.theme)
            } catch (e: Exception) { null }
        }

        Card(
            modifier = Modifier.padding(4.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(getShape(shape)), // Apply YOUR custom shape here
                    contentAlignment = Alignment.Center
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    drawable is android.graphics.drawable.AdaptiveIconDrawable) {

                    // 1. Test the Background (system_accent1_100)
                        Image(
                            painter = rememberAsyncImagePainter(drawable.background),
                            contentDescription = iconName,
                            colorFilter = ColorFilter.tint(container),
                            modifier = Modifier.fillMaxSize().scale(1.5f),
                            contentScale = ContentScale.FillBounds
                        )
                        // Foreground automatically takes the "Primary" color
                        Image(
                            painter = rememberAsyncImagePainter(drawable.foreground),
                            contentDescription = iconName,
                            colorFilter = ColorFilter.tint(primary),
                            modifier = Modifier.fillMaxSize().scale(1.5f),
                            contentScale = ContentScale.FillBounds
                        )
                    } else if (drawable != null) {
                        // Legacy non-adaptive icon
                        Image(
                            painter = rememberAsyncImagePainter(drawable),
                            contentDescription = iconName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = iconName.replace("_", " ").trim(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

@Composable
fun getShape(shape: IconShape) = when (shape) {
    IconShape.Circle -> CircleShape
    IconShape.Squircle -> RoundedCornerShape(25.dp) // Smooth, organic curve
    IconShape.RoundedSquare -> RoundedCornerShape(8.dp) // Standard radius
    IconShape.Square -> RectangleShape // Standard square
}