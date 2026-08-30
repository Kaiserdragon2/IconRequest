package de.kaiserdragon.iconrequest.ui.iconpreview

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import androidx.compose.ui.res.colorResource


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
    private val _isDarkMode = MutableStateFlow(initialDarkMode)
    val isDarkMode = _isDarkMode.asStateFlow()
    fun toggleDarkMode() { _isDarkMode.value = !_isDarkMode.value }
    private val _primaryColor = MutableStateFlow(Color(0xFFFFEB3B)) // Simulation of system_accent1_700
    val primaryColor = _primaryColor.asStateFlow()

    private val _useSystemDynamic = MutableStateFlow(false)
    val useSystemDynamic = _useSystemDynamic.asStateFlow()

    fun setUseSystemDynamic(enabled: Boolean) { _useSystemDynamic.value = enabled }
    fun updateColors(primary: Color, container: Color) {
        _primaryColor.value = primary
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
    private val _themeStyle = MutableStateFlow(ThemeStyle.TONAL_SPOT)
    val themeStyle: StateFlow<ThemeStyle> = _themeStyle.asStateFlow()
    fun setThemeStyle(style: ThemeStyle) {
        _themeStyle.value = style
    }
    private val _foregroundColorRole = MutableStateFlow(ColorRole.PRIMARY)
    val foregroundColorRole: StateFlow<ColorRole> = _foregroundColorRole
    private val _backgroundColorRole = MutableStateFlow(ColorRole.PRIMARY_CONTAINER)
    val backgroundColorRole: StateFlow<ColorRole> = _backgroundColorRole
    fun setForegroundColorRole(role: ColorRole) { _foregroundColorRole.value = role }
    fun setBackgroundColorRole(role: ColorRole) { _backgroundColorRole.value = role }
    fun ColorRole.toMaterialColor(scheme: ColorScheme): Color {
        return when (this) {
                    ColorRole.PRIMARY -> scheme.primary
                    ColorRole.ON_PRIMARY -> scheme.onPrimary
                    ColorRole.PRIMARY_CONTAINER -> scheme.primaryContainer
                    ColorRole.ON_PRIMARY_CONTAINER -> scheme.onPrimaryContainer
                    ColorRole.INVERSE_PRIMARY -> scheme.inversePrimary
                    ColorRole.PRIMARY_FIXED -> scheme.primaryFixed
                    ColorRole.PRIMARY_FIXED_DIM -> scheme.primaryFixedDim
                    ColorRole.ON_PRIMARY_FIXED -> scheme.onPrimaryFixed
                    ColorRole.ON_PRIMARY_FIXED_VARIANT -> scheme.onPrimaryFixedVariant

                    ColorRole.SECONDARY -> scheme.secondary
                    ColorRole.ON_SECONDARY -> scheme.onSecondary
                    ColorRole.SECONDARY_CONTAINER -> scheme.secondaryContainer
                    ColorRole.ON_SECONDARY_CONTAINER -> scheme.onSecondaryContainer
                    ColorRole.SECONDARY_FIXED -> scheme.secondaryFixed
                    ColorRole.SECONDARY_FIXED_DIM -> scheme.secondaryFixedDim
                    ColorRole.ON_SECONDARY_FIXED -> scheme.onSecondaryFixed
                    ColorRole.ON_SECONDARY_FIXED_VARIANT -> scheme.onSecondaryFixedVariant

                    ColorRole.TERTIARY -> scheme.tertiary
                    ColorRole.ON_TERTIARY -> scheme.onTertiary
                    ColorRole.TERTIARY_CONTAINER -> scheme.tertiaryContainer
                    ColorRole.ON_TERTIARY_CONTAINER -> scheme.onTertiaryContainer
                    ColorRole.TERTIARY_FIXED -> scheme.tertiaryFixed
                    ColorRole.TERTIARY_FIXED_DIM -> scheme.tertiaryFixedDim
                    ColorRole.ON_TERTIARY_FIXED -> scheme.onTertiaryFixed
                    ColorRole.ON_TERTIARY_FIXED_VARIANT -> scheme.onTertiaryFixedVariant

                    ColorRole.SURFACE -> scheme.surface
                    ColorRole.ON_SURFACE -> scheme.onSurface
                    ColorRole.SURFACE_VARIANT -> scheme.surfaceVariant
                    ColorRole.ON_SURFACE_VARIANT -> scheme.onSurfaceVariant
                    ColorRole.SURFACE_TINT -> scheme.surfaceTint
                    ColorRole.INVERSE_SURFACE -> scheme.inverseSurface
                    ColorRole.INVERSE_ON_SURFACE -> scheme.inverseOnSurface
                    ColorRole.SURFACE_BRIGHT -> scheme.surfaceBright
                    ColorRole.SURFACE_DIM -> scheme.surfaceDim
                    ColorRole.SURFACE_CONTAINER -> scheme.surfaceContainer
                    ColorRole.SURFACE_CONTAINER_LOW -> scheme.surfaceContainerLow
                    ColorRole.SURFACE_CONTAINER_LOWEST -> scheme.surfaceContainerLowest
                    ColorRole.SURFACE_CONTAINER_HIGH -> scheme.surfaceContainerHigh
                    ColorRole.SURFACE_CONTAINER_HIGHEST -> scheme.surfaceContainerHighest

                    ColorRole.OUTLINE -> scheme.outline
                    ColorRole.OUTLINE_VARIANT -> scheme.outlineVariant
                    ColorRole.SCRIM -> scheme.scrim
                    ColorRole.BACKGROUND -> scheme.background
                    ColorRole.ON_BACKGROUND -> scheme.onBackground

                    ColorRole.ERROR -> scheme.error
                    ColorRole.ON_ERROR -> scheme.onError
                    ColorRole.ERROR_CONTAINER -> scheme.errorContainer
                    ColorRole.ON_ERROR_CONTAINER -> scheme.onErrorContainer

            else -> scheme.primary
                }
            }

    @Composable
    fun ColorRole.toColor(scheme: ColorScheme): Color {
        val context = LocalContext.current

        // Check if it's a system color by prefix
        if (this.name.startsWith("SYS_")) {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val resId = getSystemResId(this.name)
                if (resId != 0) colorResource(resId) else Color.Gray
            } else {
                Color.Gray // Fallback for Pre-Android 12
            }
        }

        // Default to Material 3 mapping
        return this.toMaterialColor(scheme)
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSystemResId(enumName: String): Int {
        return when (enumName) {
            // --- Accent 1 (Primary Hue) ---
            "SYS_A1_0" -> android.R.color.system_accent1_0
            "SYS_A1_10" -> android.R.color.system_accent1_10
            "SYS_A1_50" -> android.R.color.system_accent1_50
            "SYS_A1_100" -> android.R.color.system_accent1_100
            "SYS_A1_200" -> android.R.color.system_accent1_200
            "SYS_A1_300" -> android.R.color.system_accent1_300
            "SYS_A1_400" -> android.R.color.system_accent1_400
            "SYS_A1_500" -> android.R.color.system_accent1_500
            "SYS_A1_600" -> android.R.color.system_accent1_600
            "SYS_A1_700" -> android.R.color.system_accent1_700
            "SYS_A1_800" -> android.R.color.system_accent1_800
            "SYS_A1_900" -> android.R.color.system_accent1_900
            "SYS_A1_1000" -> android.R.color.system_accent1_1000

            // --- Accent 2 (Secondary Hue) ---
            "SYS_A2_0" -> android.R.color.system_accent2_0
            "SYS_A2_10" -> android.R.color.system_accent2_10
            "SYS_A2_50" -> android.R.color.system_accent2_50
            "SYS_A2_100" -> android.R.color.system_accent2_100
            "SYS_A2_200" -> android.R.color.system_accent2_200
            "SYS_A2_300" -> android.R.color.system_accent2_300
            "SYS_A2_400" -> android.R.color.system_accent2_400
            "SYS_A2_500" -> android.R.color.system_accent2_500
            "SYS_A2_600" -> android.R.color.system_accent2_600
            "SYS_A2_700" -> android.R.color.system_accent2_700
            "SYS_A2_800" -> android.R.color.system_accent2_800
            "SYS_A2_900" -> android.R.color.system_accent2_900
            "SYS_A2_1000" -> android.R.color.system_accent2_1000

            // --- Accent 3 (Tertiary Hue) ---
            "SYS_A3_0" -> android.R.color.system_accent3_0
            "SYS_A3_10" -> android.R.color.system_accent3_10
            "SYS_A3_50" -> android.R.color.system_accent3_50
            "SYS_A3_100" -> android.R.color.system_accent3_100
            "SYS_A3_200" -> android.R.color.system_accent3_200
            "SYS_A3_300" -> android.R.color.system_accent3_300
            "SYS_A3_400" -> android.R.color.system_accent3_400
            "SYS_A3_500" -> android.R.color.system_accent3_500
            "SYS_A3_600" -> android.R.color.system_accent3_600
            "SYS_A3_700" -> android.R.color.system_accent3_700
            "SYS_A3_800" -> android.R.color.system_accent3_800
            "SYS_A3_900" -> android.R.color.system_accent3_900
            "SYS_A3_1000" -> android.R.color.system_accent3_1000

            // --- Neutral 1 (Background/Surface) ---
            "SYS_N1_0" -> android.R.color.system_neutral1_0
            "SYS_N1_10" -> android.R.color.system_neutral1_10
            "SYS_N1_50" -> android.R.color.system_neutral1_50
            "SYS_N1_100" -> android.R.color.system_neutral1_100
            "SYS_N1_200" -> android.R.color.system_neutral1_200
            "SYS_N1_300" -> android.R.color.system_neutral1_300
            "SYS_N1_400" -> android.R.color.system_neutral1_400
            "SYS_N1_500" -> android.R.color.system_neutral1_500
            "SYS_N1_600" -> android.R.color.system_neutral1_600
            "SYS_N1_700" -> android.R.color.system_neutral1_700
            "SYS_N1_800" -> android.R.color.system_neutral1_800
            "SYS_N1_900" -> android.R.color.system_neutral1_900
            "SYS_N1_1000" -> android.R.color.system_neutral1_1000

            // --- Neutral 2 (Surface Variant) ---
            "SYS_N2_0" -> android.R.color.system_neutral2_0
            "SYS_N2_10" -> android.R.color.system_neutral2_10
            "SYS_N2_50" -> android.R.color.system_neutral2_50
            "SYS_N2_100" -> android.R.color.system_neutral2_100
            "SYS_N2_200" -> android.R.color.system_neutral2_200
            "SYS_N2_300" -> android.R.color.system_neutral2_300
            "SYS_N2_400" -> android.R.color.system_neutral2_400
            "SYS_N2_500" -> android.R.color.system_neutral2_500
            "SYS_N2_600" -> android.R.color.system_neutral2_600
            "SYS_N2_700" -> android.R.color.system_neutral2_700
            "SYS_N2_800" -> android.R.color.system_neutral2_800
            "SYS_N2_900" -> android.R.color.system_neutral2_900
            "SYS_N2_1000" -> android.R.color.system_neutral2_1000

            else -> 0
        }
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

enum class ColorRole(val label: String) {
    // Primary
    PRIMARY("Primary"),
    ON_PRIMARY("On Primary"),
    PRIMARY_CONTAINER("Primary Container"),
    ON_PRIMARY_CONTAINER("On Primary Container"),
    INVERSE_PRIMARY("Inverse Primary"),
    PRIMARY_FIXED("Primary Fixed"),
    PRIMARY_FIXED_DIM("Primary Fixed Dim"),
    ON_PRIMARY_FIXED("On Primary Fixed"),
    ON_PRIMARY_FIXED_VARIANT("On Primary Fixed Variant"),

    // Secondary
    SECONDARY("Secondary"),
    ON_SECONDARY("On Secondary"),
    SECONDARY_CONTAINER("Secondary Container"),
    ON_SECONDARY_CONTAINER("On Secondary Container"),
    SECONDARY_FIXED("Secondary Fixed"),
    SECONDARY_FIXED_DIM("Secondary Fixed Dim"),
    ON_SECONDARY_FIXED("On Secondary Fixed"),
    ON_SECONDARY_FIXED_VARIANT("On Secondary Fixed Variant"),

    // Tertiary
    TERTIARY("Tertiary"),
    ON_TERTIARY("On Tertiary"),
    TERTIARY_CONTAINER("Tertiary Container"),
    ON_TERTIARY_CONTAINER("On Tertiary Container"),
    TERTIARY_FIXED("Tertiary Fixed"),
    TERTIARY_FIXED_DIM("Tertiary Fixed Dim"),
    ON_TERTIARY_FIXED("On Tertiary Fixed"),
    ON_TERTIARY_FIXED_VARIANT("On Tertiary Fixed Variant"),

    // Neutral / Surface
    SURFACE("Surface"),
    ON_SURFACE("On Surface"),
    SURFACE_VARIANT("Surface Variant"),
    ON_SURFACE_VARIANT("On Surface Variant"),
    SURFACE_TINT("Surface Tint"),
    INVERSE_SURFACE("Inverse Surface"),
    INVERSE_ON_SURFACE("Inverse On Surface"),
    SURFACE_BRIGHT("Surface Bright"),
    SURFACE_DIM("Surface Dim"),
    SURFACE_CONTAINER("Surface Container"),
    SURFACE_CONTAINER_LOW("Surface Container Low"),
    SURFACE_CONTAINER_LOWEST("Surface Container Lowest"),
    SURFACE_CONTAINER_HIGH("Surface Container High"),
    SURFACE_CONTAINER_HIGHEST("Surface Container Highest"),

    // Utility
    OUTLINE("Outline"),
    OUTLINE_VARIANT("Outline Variant"),
    SCRIM("Scrim"),
    BACKGROUND("Background"),
    ON_BACKGROUND("On Background"),

    // Error
    ERROR("Error"),
    ON_ERROR("On Error"),
    ERROR_CONTAINER("Error Container"),
    ON_ERROR_CONTAINER("On Error Container"),

    SYS_A1_0("A1 0"), SYS_A1_10("A1 10"), SYS_A1_50("A1 50"), SYS_A1_100("A1 100"),
    SYS_A1_200("A1 200"), SYS_A1_300("A1 300"), SYS_A1_400("A1 400"), SYS_A1_500("A1 500"),
    SYS_A1_600("A1 600"), SYS_A1_700("A1 700"), SYS_A1_800("A1 800"), SYS_A1_900("A1 900"), SYS_A1_1000("A1 1000"),

    // System Accent 2 (Secondary)
    SYS_A2_0("A2 0"), SYS_A2_10("A2 10"), SYS_A2_50("A2 50"), SYS_A2_100("A2 100"),
    SYS_A2_200("A2 200"), SYS_A2_300("A2 300"), SYS_A2_400("A2 400"), SYS_A2_500("A2 500"),
    SYS_A2_600("A2 600"), SYS_A2_700("A2 700"), SYS_A2_800("A2 800"), SYS_A2_900("A2 900"), SYS_A2_1000("A2 1000"),

    // System Accent 3 (Tertiary)
    SYS_A3_0("A3 0"), SYS_A3_10("A3 10"), SYS_A3_50("A3 50"), SYS_A3_100("A3 100"),
    SYS_A3_200("A3 200"), SYS_A3_300("A3 300"), SYS_A3_400("A3 400"), SYS_A3_500("A3 500"),
    SYS_A3_600("A3 600"), SYS_A3_700("A3 700"), SYS_A3_800("A3 800"), SYS_A3_900("A3 900"), SYS_A3_1000("A3 1000"),

    // System Neutral 1 (Surface/Background)
    SYS_N1_0("N1 0"), SYS_N1_10("N1 10"), SYS_N1_50("N1 50"), SYS_N1_100("N1 100"),
    SYS_N1_200("N1 200"), SYS_N1_300("N1 300"), SYS_N1_400("N1 400"), SYS_N1_500("N1 500"),
    SYS_N1_600("N1 600"), SYS_N1_700("N1 700"), SYS_N1_800("N1 800"), SYS_N1_900("N1 900"), SYS_N1_1000("N1 1000"),

    // System Neutral 2 (Surface Variant)
    SYS_N2_0("N2 0"), SYS_N2_10("N2 10"), SYS_N2_50("N2 50"), SYS_N2_100("N2 100"),
    SYS_N2_200("N2 200"), SYS_N2_300("N2 300"), SYS_N2_400("N2 400"), SYS_N2_500("N2 500"),
    SYS_N2_600("N2 600"), SYS_N2_700("N2 700"), SYS_N2_800("N2 800"), SYS_N2_900("N2 900"), SYS_N2_1000("N2 1000")
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