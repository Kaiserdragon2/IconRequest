package de.kaiserdragon.iconrequest.ui.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


enum class AppThemeSetting {
    System, Light, Dark,
    LightMediumContrast, LightHighContrast,
    DarkMediumContrast, DarkHighContrast
}

class SettingsViewModel(context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    // Existing states
    private val _themeSetting = MutableStateFlow(
        AppThemeSetting.valueOf(
            prefs.getString("theme_key", AppThemeSetting.System.name) ?: AppThemeSetting.System.name
        )
    )
    val themeSetting = _themeSetting.asStateFlow()

    private val _useDynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors_key", false))
    val useDynamicColors = _useDynamicColors.asStateFlow()

    private val _excludeResourcesTag =
        MutableStateFlow(prefs.getBoolean("exclude_resource_key", false))
    val excludeResourcesTag = _excludeResourcesTag.asStateFlow()

    // NEW: Show System Apps
    private val _showSystemApps = MutableStateFlow(prefs.getBoolean("show_system_apps_key", false))
    val showSystemApps = _showSystemApps.asStateFlow()

    // NEW: Show Widgets
    private val _showWidgets = MutableStateFlow(prefs.getBoolean("show_widgets_key", false))
    val showWidgets = _showWidgets.asStateFlow()

    fun setTheme(theme: AppThemeSetting) {
        _themeSetting.value = theme
        prefs.edit { putString("theme_key", theme.name) }
    }

    fun setDynamicColors(enabled: Boolean) {
        _useDynamicColors.value = enabled
        prefs.edit { putBoolean("dynamic_colors_key", enabled) }
    }

    fun setExcludeResourcesTag(exclude: Boolean) {
        _excludeResourcesTag.value = exclude
        // Fixed: Used correct key here
        prefs.edit { putBoolean("exclude_resource_key", exclude) }
    }

    fun setShowSystemApps(enabled: Boolean) {
        viewModelScope.launch {
            _showSystemApps.value = enabled
            // If system apps enabled, disable widgets
            if (enabled) {
                _showWidgets.value = false
            }

            prefs.edit {
                putBoolean("show_system_apps_key", enabled)
                if (enabled) putBoolean("show_widgets_key", false)
            }
        }
    }

    fun setShowWidgets(enabled: Boolean) {
        viewModelScope.launch {
            _showWidgets.value = enabled
            // If widgets enabled, disable system apps
            if (enabled) {
                _showSystemApps.value = false
            }

            prefs.edit {
                putBoolean("show_widgets_key", enabled)
                if (enabled) putBoolean("show_system_apps_key", false)
            }
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}