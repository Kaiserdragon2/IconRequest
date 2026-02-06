package de.kaiserdragon.iconrequest.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.kaiserdragon.iconrequest.R
import de.kaiserdragon.iconrequest.ui.dialog.IconPackPickerDialog
import de.kaiserdragon.iconrequest.ui.iconrequest.IconRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLaunchRequest: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHealth: (String) -> Unit,
    onNavigateToComparison: (String, String) -> Unit,
    viewModel: IconRequestViewModel
) {
    var showPackDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var firstPackForCompare by remember {
        mutableStateOf<de.kaiserdragon.iconrequest.data.AppInfo?>(
            null
        )
    }
    val scrollState = rememberScrollState()
    val iconPacks by viewModel.installedIconPacks.collectAsState()
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    LaunchedEffect(Unit) { viewModel.loadIconPacks(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IconRequest", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "You have ${iconPacks.size} icon packs installed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        modifier = Modifier.alpha(0.3f)
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .height(32.dp)
            )
            // --- HERO SECTION ---
            Card(
                onClick = onLaunchRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                interactionSource = interactionSource,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 30.dp, y = 30.dp) // Peak out of the corner
                            .alpha(0.1f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "New Icon Request",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Select apps and share",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- UTILITY SECTION HEADER ---
            Text(
                text = "Developer Tools",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // --- GRID SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Health Check Card
                HomeToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Health Check",
                    description = "Find duplicate mappings and missing drawables",
                    icon = Icons.Default.HealthAndSafety,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { showPackDialog = true }
                )

                // Comparison Card
                HomeToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Compare",
                    description = "Check coverage against another pack",
                    icon = Icons.Default.Compare,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { showCompareDialog = true }
                )

            }
        }
    }


    // Show the picker dialog when the button is clicked
    if (showPackDialog) {
        IconPackPickerDialog(
            iconPacks = iconPacks,
            title = "Check Icon Pack Health", // More relevant title
            showResetOption = false, // Hide "Show All" because it makes no sense here
            showSystemOption = false,
            onDismiss = { showPackDialog = false },
            onPackSelected = { pack ->
                if (pack != null) onNavigateToHealth(pack.packageName)
                showPackDialog = false
            }
        )
    }

    // Logic for Comparison Picker
    if (showCompareDialog) {
        IconPackPickerDialog(
            iconPacks = iconPacks,
            title = if (firstPackForCompare == null) "Select First Pack" else "Select Second Pack",
            showResetOption = false,
            showSystemOption = true,
            onDismiss = {
                showCompareDialog = false
                firstPackForCompare = null // Reset selection if canceled
            },
            onPackSelected = { pack ->
                if (pack != null) {
                    if (firstPackForCompare == null) {
                        // Step 1: Store the first pack and keep dialog open
                        firstPackForCompare = pack
                    } else {
                        // Step 2: We have both! Navigate
                        onNavigateToComparison(
                            firstPackForCompare!!.packageName,
                            pack.packageName
                        )
                        showCompareDialog = false
                        firstPackForCompare = null // Reset for next time
                    }
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeToolCard(
    modifier: Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            )
        }
    }
}