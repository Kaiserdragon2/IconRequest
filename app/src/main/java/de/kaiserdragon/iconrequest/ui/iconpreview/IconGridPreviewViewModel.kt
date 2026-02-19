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
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import de.kaiserdragon.iconrequest.data.IconPackManager
import de.kaiserdragon.iconrequest.ui.IconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser


class IconGridPreviewViewModel(
    private val iconPackManager: IconPackManager,
    val initialDarkMode: Boolean
) : ViewModel() {
    private val _iconList = MutableStateFlow<List<String>>(emptyList())
    val iconList: StateFlow<List<String>> = _iconList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedShape = MutableStateFlow(IconShape.Square)
    val selectedShape: StateFlow<IconShape> = _selectedShape.asStateFlow()
    // Inside IconGridPreviewViewModel
    private val _isDarkMode = MutableStateFlow(initialDarkMode)
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

    private val _hue = MutableStateFlow(210f)    // Default Blue
    val hue = _hue.asStateFlow()

    private val _chroma = MutableStateFlow(48f)  // 0 is grey, 48 is M3 standard
    val chroma = _chroma.asStateFlow()

    private val _tone = MutableStateFlow(50f)    // 0 is black, 100 is white
    val tone = _tone.asStateFlow()

    fun updateHct(h: Float? = null, c: Float? = null, t: Float? = null) {
        h?.let { _hue.value = it }
        c?.let { _chroma.value = it }
        t?.let { _tone.value = it }
    }

    private val _contrast = MutableStateFlow(0.0f)
    val contrast = _contrast.asStateFlow()

    fun updateContrast(value: Float) {
        _contrast.value = value
    }

    fun getContrastColor(color: Color): Color {
        // Calculate relative luminance
        val argb = color.toArgb()
        val r = android.graphics.Color.red(argb) / 255.0
        val g = android.graphics.Color.green(argb) / 255.0
        val b = android.graphics.Color.blue(argb) / 255.0

        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

        // Threshold is 0.5, but 0.179 is often used for better perceptual results
        return if (luminance > 0.179) Color.Black else Color.White
    }
    enum class ThemeStyle {
        TONAL_SPOT,
        EXPRESSIVE,
        VIBRANT,
        SPRITZ,
        RAINBOW,
        FRUIT_SALAD,
        MONOCHROME
    }
    private val _themeStyle = MutableStateFlow(ThemeStyle.TONAL_SPOT)
    val themeStyle: StateFlow<ThemeStyle> = _themeStyle.asStateFlow()

    fun setThemeStyle(style: ThemeStyle) {
        _themeStyle.value = style
    }

    @SuppressLint("RestrictedApi")
    fun generateScheme(seedColor: Color, contrast: Float, isDark: Boolean, themeStyle: ThemeStyle): ColorScheme {
        // Convert standard Color to HCT for the M3 Engine
        val hctSeed = Hct.fromInt(seedColor.toArgb())

        // Create the scheme using the contrast level (-1.0 to 1.0)
        val scheme = when (themeStyle) {
            ThemeStyle.EXPRESSIVE -> SchemeExpressive(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.TONAL_SPOT -> SchemeTonalSpot(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.VIBRANT    -> SchemeVibrant(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.RAINBOW    -> SchemeRainbow(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.FRUIT_SALAD-> SchemeFruitSalad(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.MONOCHROME -> SchemeMonochrome(hctSeed, isDark, contrast.toDouble())
            ThemeStyle.SPRITZ     -> SchemeContent(hctSeed, isDark, contrast.toDouble())
        }

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
            surfaceTint = Color(scheme.surfaceVariant), // Usually primary or variant
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
            primaryFixed = Color(scheme.primaryFixed),
            onPrimaryFixed = Color(scheme.onPrimaryFixed),
            primaryFixedDim = Color(scheme.primaryFixedDim),
            onPrimaryFixedVariant = Color(scheme.onPrimaryFixedVariant),
            secondaryFixed = Color(scheme.secondaryFixed),
            onSecondaryFixed = Color(scheme.onSecondaryFixed),
            secondaryFixedDim = Color(scheme.secondaryFixedDim),
            onSecondaryFixedVariant = Color(scheme.onSecondaryFixedVariant),
            tertiaryFixed = Color(scheme.tertiaryFixed),
            onTertiaryFixed = Color(scheme.onTertiaryFixed),
            tertiaryFixedDim = Color(scheme.tertiaryFixedDim),
            onTertiaryFixedVariant = Color(scheme.onTertiaryFixedVariant)
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

class IconGridPreviewViewModelFactory(
    private val iconPackManager: IconPackManager,
    private val isSystemInDarkTheme: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IconGridPreviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Use the lowercase 'iconPackManager' instance here
            return IconGridPreviewViewModel(iconPackManager, isSystemInDarkTheme) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}