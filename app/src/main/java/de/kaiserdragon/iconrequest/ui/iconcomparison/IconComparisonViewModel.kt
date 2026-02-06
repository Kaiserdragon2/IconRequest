package de.kaiserdragon.iconrequest.ui.iconcomparison

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.kaiserdragon.iconrequest.data.AppInfo
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.data.IconRequestExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ComparisonFilter {
    ALL,
    MISSING_IN_A,
    MISSING_IN_B,
    DIFFERENCES_ONLY // Show if either is missing
}

class IconComparisonViewModel(
    private val iconPackManager: IconPackManager,
    private val iconRequestExporter: IconRequestExporter
) : ViewModel() {
    // In IconComparisonViewModel.kt
    private val _rawResults = MutableStateFlow<List<IconPackManager.ComparisonResult>>(emptyList())
    private val _filterMode = MutableStateFlow(ComparisonFilter.ALL)
    val filterMode = _filterMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _fullPackMode = MutableStateFlow(false) // Toggle state
    val fullPackMode = _fullPackMode.asStateFlow()

    // In IconComparisonViewModel.kt
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _statsA = MutableStateFlow(PackStats(0, 0, 0))
    val statsA = _statsA.asStateFlow()

    private val _statsB = MutableStateFlow(PackStats(0, 0, 0))
    val statsB = _statsB.asStateFlow()

    data class PackStats(
        val totalAppsSupported: Int,
        val uniqueIconsUsed: Int,
        val uniqueIconsDesigned: Int
    )

    val filteredResults = combine(_rawResults, _filterMode, _searchQuery) { results, mode, query ->
        results.filter { result ->
            val matchesSearch = result.appName.contains(query, ignoreCase = true) ||
                    result.packageName.contains(query, ignoreCase = true)
            val matchesFilter = when (mode) {
                ComparisonFilter.ALL -> true
                ComparisonFilter.MISSING_IN_A -> result.packAIcon == null && result.packBIcon != null
                ComparisonFilter.MISSING_IN_B -> result.packBIcon == null && result.packAIcon != null
                ComparisonFilter.DIFFERENCES_ONLY -> result.packAIcon == null || result.packBIcon == null
            }
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(mode: ComparisonFilter) {
        _filterMode.value = mode
    }

    fun toggleMode(packA: String, packB: String, installedApps: List<AppInfo>) {
        _fullPackMode.value = !_fullPackMode.value
        runComparison(packA, packB, installedApps)
    }

    private fun getAppKey(packageName: String, activityName: String) = "$packageName|$activityName"

    // Helper to create a stable ID for ComparisonResults
    fun getResultId(result: IconPackManager.ComparisonResult): String {
        return "${result.packageName}|${result.activityName}"
    }

    fun toggleSelection(result: IconPackManager.ComparisonResult) {
        val id = getResultId(result)
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun toggleSelection(packageName: String, activityName: String) {
        val key = getAppKey(packageName, activityName)
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _selectedIds.value = current
    }

    fun toggleAllVisible(
        visibleResults: List<IconPackManager.ComparisonResult>,
        isSelected: Boolean
    ) {
        val current = _selectedIds.value.toMutableSet()
        visibleResults.forEach { result ->
            val key = getAppKey(result.packageName, result.activityName)
            if (isSelected) current.add(key) else current.remove(key)
        }
        _selectedIds.value = current
    }

    fun clearSelections() {
        _selectedIds.value = emptySet()
    }

    private val comparisonCache = mutableMapOf<String, ComparisonCacheEntry>()

    data class ComparisonCacheEntry(
        val results: List<IconPackManager.ComparisonResult>,
        val statsA: PackStats,
        val statsB: PackStats
    )

    fun runComparison(packA: String, packB: String, allApps: List<AppInfo>) {
        val cacheKey = "${packA}_${packB}_${_fullPackMode.value}"

        // Check Cache
        comparisonCache[cacheKey]?.let { cached ->
            _rawResults.value = cached.results
            _statsA.value = cached.statsA
            _statsB.value = cached.statsB
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            // If fullPackMode is true, we pass null to the manager to get ALL icons
            val (results, mapA, mapB) = iconPackManager.comparePacks(
                packA,
                packB,
                if (_fullPackMode.value) null else allApps
            )
            _statsA.value = PackStats(
                totalAppsSupported = mapA.size,
                uniqueIconsUsed = mapA.values.distinct().size,
                uniqueIconsDesigned = iconPackManager.getUniqueDrawableCount(packA)
            )
            _statsB.value = PackStats(
                totalAppsSupported = mapB.size,
                uniqueIconsUsed = mapB.values.distinct().size,
                uniqueIconsDesigned = iconPackManager.getUniqueDrawableCount(packB)
            )
            val entry = ComparisonCacheEntry(results, _statsA.value, _statsB.value)
            comparisonCache[cacheKey] = entry

            _rawResults.value = results
            _isProcessing.value = false
        }
    }

    private fun mapToAppInfo(
        result: IconPackManager.ComparisonResult,
        context: Context,
        packAPackage: String,
        packBPackage: String
    ): AppInfo {
        val pm = context.packageManager

        // Determine which drawable name to use (A has priority, then B)
        val drawableName = result.packAIcon ?: result.packBIcon
        val targetPackage = if (result.packAIcon != null) packAPackage else packBPackage

        val iconDrawable = if (drawableName != null) {
            try {
                val res = pm.getResourcesForApplication(targetPackage)
                val id = res.getIdentifier(drawableName, "drawable", targetPackage)
                if (id != 0) res.getDrawable(id, null) else null
            } catch (e: Exception) {
                null
            }
        } else null

        return AppInfo(
            name = result.appName,
            packageName = result.packageName,
            activityName = result.activityName,
            // Use pack icon if found, otherwise fallback to system icon
            icon = iconDrawable ?: try {
                pm.getActivityIcon(ComponentName(result.packageName, result.activityName))
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }
        )
    }

    private fun getSelectedAppInfos(
        context: Context,
        packA: String,
        packB: String
    ): List<AppInfo> {
        val selectedSet = _selectedIds.value
        return _rawResults.value
            .filter { getResultId(it) in selectedSet }
            .map { mapToAppInfo(it, context, packA, packB) } // Pass packages here
    }

    fun shareRequest(context: Context, packA: String, packB: String) {
        val selectedApps = getSelectedAppInfos(context, packA, packB)
        if (selectedApps.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val intent = iconRequestExporter.prepareShareIntent(selectedApps)
                context.startActivity(Intent.createChooser(intent, "Send Icon Request"))
            } catch (e: Exception) {
                Log.e("IconComparison", "Sharing failed", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveZipToDevice(context: Context, packA: String, packB: String) {
        val selectedApps = getSelectedAppInfos(context, packA, packB)
        if (selectedApps.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            val success = iconRequestExporter.saveToDownloads(selectedApps)
            withContext(Dispatchers.Main) {
                val message = if (success) "Saved to Downloads" else "Failed to save"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            _isProcessing.value = false
        }
    }

    fun copyToClipboard(context: Context, excludeTags: Boolean, packA: String, packB: String) {
        val selectedApps = getSelectedAppInfos(context, packA, packB)
        if (selectedApps.isEmpty()) return

        val appEntries = selectedApps.map { it to iconRequestExporter.getDrawableName(it) }
        val text = iconRequestExporter.buildAppFilterString(appEntries, excludeTags)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AppFilter", text))
        Toast.makeText(context, "AppFilter copied", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, excludeTags: Boolean, packA: String, packB: String) {
        val selectedApps = getSelectedAppInfos(context, packA, packB)
        if (selectedApps.isEmpty()) return

        val appEntries = selectedApps.map { it to iconRequestExporter.getDrawableName(it) }
        val text = iconRequestExporter.buildAppFilterString(appEntries, excludeTags)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Text"))
    }
}


class IconComparisonViewModelFactory(
    private val iconPackManager: IconPackManager,
    private val exporter: IconRequestExporter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IconComparisonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IconComparisonViewModel(iconPackManager, iconRequestExporter = exporter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}