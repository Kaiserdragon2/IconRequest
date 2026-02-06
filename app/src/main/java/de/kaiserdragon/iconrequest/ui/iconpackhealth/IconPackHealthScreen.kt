package de.kaiserdragon.iconrequest.ui.iconpackhealth

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import de.kaiserdragon.iconrequest.data.IconPackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackHealthScreen(
    viewModel: IconPackHealthViewModel, // Pass the ID instead of the whole old ViewModel
    onBack: () -> Unit
) {
    val report by viewModel.healthReport.collectAsState()
    val currentReport = report

    // State to control collapsing (Default to false if you want them closed initially)
    var showDuplicates by remember { mutableStateOf(false) }
    var showBrokenLinks by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pm = remember { context.packageManager }

    // Resolve the name of the pack for the TopBar
    val packName = remember(report) {
        report?.let {
            try {
                val info = pm.getApplicationInfo(it.packPackageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                it.packPackageName
            }
        } ?: "Health Report"
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(packName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {

                    // Add the Share Button here
                    if (currentReport != null) {
                        IconButton(onClick = { viewModel.shareReport(context) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (currentReport == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing for cleaner lists
            ) {
                item { HealthSummaryCard(currentReport) }

                // --- DUPLICATES SECTION ---
                if (currentReport.duplicates.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            title = "Duplicate Entries",
                            count = currentReport.duplicates.size,
                            isExpanded = showDuplicates,
                            onToggle = { showDuplicates = !showDuplicates }
                        )
                    }

                    // Only show items if expanded
                    if (showDuplicates) {
                        items(currentReport.duplicates.entries.toList()) { entry ->
                            DuplicateItemCard(
                                component = entry.key,
                                drawableNames = entry.value,
                                iconPackPackage = currentReport.packPackageName
                            )
                        }
                    }
                }

                // --- BROKEN LINKS SECTION ---
                if (currentReport.missingDrawables.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            title = "Broken Links",
                            count = currentReport.missingDrawables.size,
                            isExpanded = showBrokenLinks,
                            onToggle = { showBrokenLinks = !showBrokenLinks }
                        )
                    }

                    // Only show items if expanded
                    if (showBrokenLinks) {
                        items(currentReport.missingDrawables) { missing ->
                            MissingDrawableCard(missing)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthSummaryCard(report: IconPackManager.IconPackReport) {
    val healthScore = if (report.totalEntries == 0) 100
    else ((1 - ((report.missingDrawables.size.toFloat() + report.duplicates.size.toFloat()) / report.totalEntries)) * 100).toInt()

    val context = LocalContext.current
    val pm = remember { context.packageManager }

    // Get pack icon
    val packIcon = remember(report.packPackageName) {
        try {
            pm.getApplicationIcon(report.packPackageName)
        } catch (e: Exception) {
            null
        }
    }

    val healthColor = when {
        healthScore > 99 -> Color(0xFF4CAF50) // Green
        healthScore > 90 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.error // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (packIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(packIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(4.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Text(
                    "Health Status",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$healthScore%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = healthColor
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthStatItem("Mappings", report.totalEntries.toString())
                HealthStatItem(
                    "Duplicates",
                    report.duplicates.size.toString(),
                    color = if (report.duplicates.isNotEmpty()) MaterialTheme.colorScheme.error else null
                )
                HealthStatItem(
                    "Broken",
                    report.missingDrawables.size.toString(),
                    color = if (report.missingDrawables.isNotEmpty()) MaterialTheme.colorScheme.error else null
                )
            }
        }
    }
}

@Composable
fun HealthStatItem(label: String, value: String, color: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@SuppressLint("UseCompatLoadingForDrawables", "DiscouragedApi")
@Composable
fun DuplicateItemCard(
    component: String,
    drawableNames: List<String>,
    iconPackPackage: String
) {
    if (iconPackPackage.isEmpty()) return
    val context = LocalContext.current

    val packRes = remember(iconPackPackage) {
        try {
            context.packageManager.getResourcesForApplication(iconPackPackage)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(component, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                drawableNames.forEach { name ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val drawableId =
                            packRes?.getIdentifier(name, "drawable", iconPackPackage) ?: 0
                        if (drawableId != 0 && packRes != null) {
                            val iconPainter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(packRes.getDrawable(drawableId, null))
                                    .build()
                            )
                            Image(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            // Fallback icon if not found
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text("?", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        Text(name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MissingDrawableCard(missingText: String) {
    // Logic to split "Component: X -> Missing Drawable: Y" if you used that format
    val parts = missingText.split(" -> ")
    val component = parts.getOrNull(0) ?: missingText
    val drawable = parts.getOrNull(1) ?: "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Missing Resource",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = drawable,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = component,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))

            // FIX: Apply background directly to the Text using Modifier
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(rotationState)
        )
    }
}