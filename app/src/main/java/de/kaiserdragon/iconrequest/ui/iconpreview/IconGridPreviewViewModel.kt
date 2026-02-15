package de.kaiserdragon.iconrequest.ui.iconpackhealth

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.ui.IconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser


class IconGridPreviewViewModel(private val iconPackManager: IconPackManager) : ViewModel() {
    private val _iconList = MutableStateFlow<List<String>>(emptyList())
    val iconList: StateFlow<List<String>> = _iconList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedShape = MutableStateFlow(IconShape.Circle)
    val selectedShape: StateFlow<IconShape> = _selectedShape.asStateFlow()
    // Inside IconGridPreviewViewModel
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _testColor = MutableStateFlow(Color(0xFFD0BCFF)) // Default Purple
    val testColor = _testColor.asStateFlow()

    fun toggleDarkMode() { _isDarkMode.value = !_isDarkMode.value }
    fun updateTestColor(color: Color) { _testColor.value = color }

    private val _primaryColor = MutableStateFlow(Color(0xFF6750A4)) // Simulation of system_accent1_700
    val primaryColor = _primaryColor.asStateFlow()

    private val _containerColor = MutableStateFlow(Color(0xFFEADDFF)) // Simulation of system_accent1_100
    val containerColor = _containerColor.asStateFlow()
    private val _useSystemDynamic = MutableStateFlow(false)
    val useSystemDynamic = _useSystemDynamic.asStateFlow()

    fun setUseSystemDynamic(enabled: Boolean) { _useSystemDynamic.value = enabled }
    fun updateColors(primary: Color, container: Color) {
        _primaryColor.value = primary
        _containerColor.value = container
    }
    fun updateShape(shape: IconShape) {
        _selectedShape.value = shape
    }

    fun loadIconPreview(packageName: String) {
        // Optimization: Don't reload if we already have the list
        if (_iconList.value.isNotEmpty()) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _iconList.value = emptyList()
            val icons = withContext(Dispatchers.IO) {
                fetchDrawableNames(packageName)
            }
            _iconList.value = icons
            _isRefreshing.value = false
        }
    }


    @SuppressLint("RestrictedApi")
    fun generateSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
        val hct = Hct.fromInt(seed.toArgb())
        // TonalSpot is the standard Material 3 look
        val scheme = SchemeTonalSpot(hct, dark, 0.0)

        return ColorScheme(
            primary = Color(scheme.primary),
            onPrimary = Color(scheme.onPrimary),
            primaryContainer = Color(scheme.primaryContainer),
            onPrimaryContainer = Color(scheme.onPrimaryContainer),
            inversePrimary = Color(scheme.inversePrimary),
            secondary = Color(scheme.secondary),
            onSecondary = Color(scheme.onSecondary),
            secondaryContainer = Color(scheme.secondaryContainer),
            onSecondaryContainer = Color(scheme.onSecondaryContainer),
            tertiary = Color(scheme.tertiary),
            onTertiary = Color(scheme.onTertiary),
            tertiaryContainer = Color(scheme.tertiaryContainer),
            onTertiaryContainer = Color(scheme.onTertiaryContainer),
            background = Color(scheme.background),
            onBackground = Color(scheme.onBackground),
            surface = Color(scheme.surface),
            onSurface = Color(scheme.onSurface),
            surfaceVariant = Color(scheme.surfaceVariant),
            onSurfaceVariant = Color(scheme.onSurfaceVariant),
            surfaceTint = Color(scheme.primary),
            inverseSurface = Color(scheme.inverseSurface),
            inverseOnSurface = Color(scheme.inverseOnSurface),
            error = Color(scheme.error),
            onError = Color(scheme.onError),
            errorContainer = Color(scheme.errorContainer),
            onErrorContainer = Color(scheme.onErrorContainer),
            outline = Color(scheme.outline),
            outlineVariant = Color(scheme.outlineVariant),
            scrim = Color(scheme.scrim),
            surfaceBright = Color(scheme.surfaceBright),
            surfaceDim = Color(scheme.surfaceDim),
            surfaceContainer = Color(scheme.surfaceContainer),
            surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
            surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
            surfaceContainerLow = Color(scheme.surfaceContainerLow),
            surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        )
    }


    private fun fetchDrawableNames(packageName: String): List<String> {
        val iconNames = mutableSetOf<String>()
        try {
            val context = iconPackManager.getContext()
            val packContext = context.createPackageContext(packageName, 0)
            val res = packContext.resources

            // 1. Try drawable.xml (Best for a complete preview)
            val drawableXmlId = res.getIdentifier("drawable", "xml", packageName)
            if (drawableXmlId != 0) {
                val parser = res.getXml(drawableXmlId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        parser.getAttributeValue(null, "drawable")?.let { iconNames.add(it) }
                    }
                    eventType = parser.next()
                }
            }

            // 2. Fallback: appfilter.xml (If drawable.xml is missing)
            if (iconNames.isEmpty()) {
                val appFilterId = res.getIdentifier("appfilter", "xml", packageName)
                if (appFilterId != 0) {
                    val parser = res.getXml(appFilterId)
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                            parser.getAttributeValue(null, "drawable")?.let { iconNames.add(it) }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IconPreview", "Failed to fetch drawables for $packageName", e)
        }

        // Filter out common non-icon resources and sort
        return iconNames.filter { name ->
            !name.startsWith("abc_") &&
                    !name.startsWith("notification_") &&
                    !name.startsWith("design_")
        }.sorted()
    }
}

class IconGridPreviewViewModelFactory(private val iconPackManager: IconPackManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IconGridPreviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Use the lowercase 'iconPackManager' instance here
            return IconGridPreviewViewModel(iconPackManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}