package de.kaiserdragon.iconrequest.ui.iconpackhealth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.kaiserdragon.iconrequest.data.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IconPackHealthViewModel(
    private val iconPackManager: IconPackManager
) : ViewModel() { // Use ViewModel, not AndroidViewModel

    private val _reportCache =
        MutableStateFlow<Map<String, IconPackManager.IconPackReport>>(emptyMap())
    private val _currentPackage = MutableStateFlow<String?>(null)
    val healthReport: StateFlow<IconPackManager.IconPackReport?> =
        combine(_currentPackage, _reportCache) { pkg, cache ->
            cache[pkg]
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun runHealthCheck(packageName: String) {
        _currentPackage.value = packageName

        // If we already have it in cache, we don't strictly need to show a loader
        // unless you want to force a refresh every time.
        if (_reportCache.value.containsKey(packageName)) return

        viewModelScope.launch(Dispatchers.Default) {
            _isRefreshing.value = true
            try {
                val report = iconPackManager.checkIconPackHealth(packageName)
                _reportCache.value += (packageName to report)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun shareReport(context: Context) {
        val report = healthReport.value ?: return
        val reportText = StringBuilder().apply {
            appendLine("--- Icon Pack Health Report ---")
            appendLine("Package: ${report.packPackageName}")
            appendLine("Total Entries: ${report.totalEntries}")
            appendLine("Duplicates: ${report.duplicates.size}")
            appendLine("Broken Links: ${report.missingDrawables.size}")

            if (report.duplicates.isNotEmpty()) {
                appendLine("[Duplicate Entries]")
                report.duplicates.forEach { (component, drawables) ->
                    appendLine("- $component: ${drawables.joinToString(", ")}")
                }
                appendLine()
            }

            if (report.missingDrawables.isNotEmpty()) {
                appendLine("[Broken Links]")
                report.missingDrawables.forEach { appendLine("- $it") }
            }
        }.toString()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}

class IconPackHealthViewModelFactory(
    private val iconPackManager: IconPackManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IconPackHealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IconPackHealthViewModel(iconPackManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}