package de.kaiserdragon.iconrequest.ui.iconrequest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import de.kaiserdragon.iconrequest.data.AppInfo
import de.kaiserdragon.iconrequest.ui.dialog.IconPackPickerDialog
import de.kaiserdragon.iconrequest.ui.settings.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconRequestScreen(
    viewModel: IconRequestViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val apps by viewModel.filteredApps.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Track if search mode is active
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery) {
        apps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val filteredIds = remember(filteredApps) {
        filteredApps.map { it.packageName to it.activityName }
    }
    val allFilteredSelected = remember(filteredApps) {
        filteredApps.isNotEmpty() && filteredApps.all { it.isSelected }
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showPackDialog by remember { mutableStateOf(false) }
    val iconPacks by viewModel.installedIconPacks.collectAsState()
    val selectedFilter by viewModel.selectedFilterPack.collectAsState()
    val excludeTags by settingsViewModel.excludeResourcesTag.collectAsState()

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
            focusManager.clearFocus()
        } else onBack()
    }
    Box(modifier = Modifier.fillMaxSize()) {
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
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search apps...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            Text("Icon Request")
                        }
                    },
                    actions = {
                        val context = LocalContext.current
                        // Search toggle button
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = "" // Clear search when closing
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Toggle Search"
                            )
                        }
                        IconButton(onClick = {
                            viewModel.loadIconPacks(context)
                            showPackDialog = true
                        }) {
                            // Change icon color if a filter is active to alert the user
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (selectedFilter != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        // Show the dialog when state is true
                        if (showPackDialog) {
                            IconPackPickerDialog(
                                iconPacks = iconPacks,
                                title = "Filter by Icon Pack",
                                showResetOption = true,
                                onDismiss = { showPackDialog = false },
                                onPackSelected = { pack ->
                                    viewModel.setIconPackFilter(pack)
                                    showPackDialog = false
                                }
                            )
                        }
                        IconButton(onClick = {
                            viewModel.toggleAll(filteredIds, !allFilteredSelected)
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
                val selectedCount = apps.count { it.isSelected }

                if (selectedCount > 0) {

                    var showMenu by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        ExtendedFloatingActionButton(
                            onClick = { showMenu = true },
                            icon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                            text = { Text("Request (${apps.count { it.isSelected }})") }
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share ZIP") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = { showMenu = false; viewModel.shareRequest(context) }
                            )
                            DropdownMenuItem(
                                text = { Text("Save ZIP to Downloads") },
                                leadingIcon = { Icon(Icons.Default.Save, null) },
                                onClick = { showMenu = false; viewModel.saveZipToDevice(context) }
                            )
                            HorizontalDivider()// Visual separator
                            DropdownMenuItem(
                                text = { Text("Copy AppFilter Text") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                onClick = {
                                    showMenu = false; viewModel.copyToClipboard(
                                    context,
                                    excludeTags
                                )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Text Only") },
                                leadingIcon = { Icon(Icons.Default.Description, null) },
                                onClick = {
                                    showMenu = false; viewModel.shareAsText(
                                    context,
                                    excludeTags
                                )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(filteredApps) { index, app ->
                    // Using index + package ensures it's unique even if the app repeats
                    key("${app.packageName}_$index") {
                        AppRequestItem(
                            appInfo = app,
                            onToggle = {
                                viewModel.toggleSelection(
                                    app.packageName,
                                    app.activityName
                                )
                            }
                        )
                    }
                }
            }

        }
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // Dim the background
                    .pointerInput(Unit) {}, // Block clicks from passing through
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating ZIP...")
                    }
                }
            }
        }
    }
}

@Composable
fun AppRequestItem(appInfo: AppInfo, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // Set a fixed height for the row
            .clickable { onToggle() },
        headlineContent = {
            Text(
                text = appInfo.name,
                maxLines = 1, // Prevent height expansion
                overflow = TextOverflow.Ellipsis // Adds "..." if name is too long
            )
        },
        supportingContent = {
            Text(
                text = appInfo.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall // Smaller font helps fit content
            )
        },
        leadingContent = {
            // Box ensures the icon area stays a specific size regardless of image source
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(appInfo.icon)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED) // Ensure disk caching is on
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        trailingContent = {
            Checkbox(
                checked = appInfo.isSelected,
                onCheckedChange = null // Click is handled by the ListItem modifier
            )
        }
    )
}