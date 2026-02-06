package de.kaiserdragon.iconrequest.ui.iconrequest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.kaiserdragon.iconrequest.data.AppInfo
import de.kaiserdragon.iconrequest.data.AppRepository
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.data.IconRequestExporter
import de.kaiserdragon.iconrequest.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IconRequestViewModel(
    private val repository: AppRepository,
    private val iconPackManager: IconPackManager,
    private val iconRequestExporter: IconRequestExporter,
    private val settingsViewModel: SettingsViewModel
) : ViewModel() {
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _selectedFilterPack = MutableStateFlow<AppInfo?>(null)
    val selectedFilterPack = _selectedFilterPack.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _installedIconPacks = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedIconPacks: StateFlow<List<AppInfo>> = _installedIconPacks.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsViewModel.showSystemApps,
                settingsViewModel.showWidgets
            ) { system, widgets ->
                system to widgets
            }.collect { (system, widgets) ->
                loadApps(system, widgets)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun loadApps(showSystem: Boolean, showWidgets: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.getInstalledApps(showSystem, showWidgets).collect { apps ->
                _appList.value = apps
                _isProcessing.value = false
            }
        }
    }

    fun setIconPackFilter(iconPack: AppInfo?) {
        _selectedFilterPack.value = iconPack
    }

    fun loadIconPacks(context: Context) {
        viewModelScope.launch {
            // Query the package manager for apps with icon pack intent filters
            _installedIconPacks.value = repository.getIconPacks()
        }
    }

    // In IconRequestViewModel.kt
    fun toggleSelection(packageName: String, activityName: String) {
        _appList.value = _appList.value.map { app ->
            if (app.packageName == packageName && app.activityName == activityName) {
                app.copy(isSelected = !app.isSelected)
            } else {
                app
            }
        }
    }

    fun toggleAll(ids: List<Pair<String, String>>, isSelected: Boolean) {
        val idSet = ids.toSet()
        _appList.value = _appList.value.map { app ->
            if (idSet.contains(app.packageName to app.activityName)) {
                app.copy(isSelected = isSelected)
            } else {
                app
            }
        }
    }

    fun clearSelections() {
        _appList.value = _appList.value.map { it.copy(isSelected = false) }
    }

    @OptIn(FlowPreview::class)
    val filteredApps = combine(
        _appList,
        _searchQuery.debounce(300),
        _selectedFilterPack
    ) { apps, query, filterPack ->
        var list = apps

        // First: Filter out apps already in the icon pack
        if (filterPack != null) {
            list = repository.getMissingApps(list, filterPack.packageName)
        }

        // Second: Apply search query
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun shareRequest(context: Context) {
        val selectedApps = _appList.value.filter { it.isSelected }
        if (selectedApps.isEmpty()) return

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Let the exporter handle the file creation and intent generation
                val intent = iconRequestExporter.prepareShareIntent(selectedApps)
                context.startActivity(Intent.createChooser(intent, "Send Icon Request"))
            } catch (e: Exception) {
                Log.e("IconRequest", "Sharing failed", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveZipToDevice(context: Context) {
        val selectedApps = _appList.value.filter { it.isSelected }
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

    fun copyToClipboard(context: Context, excludeTags: Boolean) {
        val selectedApps = _appList.value.filter { it.isSelected }
        if (selectedApps.isEmpty()) return
        val appEntries = mutableListOf<Pair<AppInfo, String>>()
        selectedApps.forEach { app ->
            val baseName = iconRequestExporter.getDrawableName(app)
            appEntries.add(app to baseName)
        }
        val text = iconRequestExporter.buildAppFilterString(appEntries, excludeTags)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AppFilter", text)
        clipboard.setPrimaryClip(clip)

        // Toast to notify user
        Toast.makeText(context, "AppFilter copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareAsText(context: Context, excludeTags: Boolean) {
        val selectedApps = _appList.value.filter { it.isSelected }
        if (selectedApps.isEmpty()) return
        val appEntries = mutableListOf<Pair<AppInfo, String>>()
        selectedApps.forEach { app ->
            val baseName = iconRequestExporter.getDrawableName(app)
            appEntries.add(app to baseName)
        }
        val text = iconRequestExporter.buildAppFilterString(appEntries, excludeTags)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Icon Request AppFilter")
        }
        context.startActivity(Intent.createChooser(intent, "Share AppFilter Text"))
    }
}

class IconRequestViewModelFactory(
    private val repository: AppRepository,
    private val iconPackManager: IconPackManager,
    private val exporter: IconRequestExporter,
    private val settingsViewModel: SettingsViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IconRequestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IconRequestViewModel(
                repository,
                iconPackManager,
                exporter,
                settingsViewModel
            ) as T // Pass it here
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}