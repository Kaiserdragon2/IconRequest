package de.kaiserdragon.iconrequest.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory


class IconPackManager(private val context: Context) {

    // Returns a list of apps that are recognized as Icon Packs
    fun getInstalledIconPacks(): List<AppInfo> {
        val pm = context.packageManager
        // Common intent filters for icon packs
        val filters = arrayOf(
            "org.adw.ActivityStarter.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME"
        )

        val iconPacks = mutableListOf<ResolveInfo>()
        filters.forEach { action ->
            iconPacks.addAll(pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA))
        }

        return iconPacks.distinctBy { it.activityInfo.packageName }.map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                activityName = it.activityInfo.name,
                icon = it.loadIcon(pm),
                isSelected = false
            )
        }
    }

    // Parses the appfilter.xml from the chosen icon pack APK
    @SuppressLint("DiscouragedApi")
    fun getSupportedComponents(iconPackPackage: String): Set<String> {
        val components = mutableSetOf<String>()
        try {
            val iconPackContext = context.createPackageContext(iconPackPackage, 0)
            val res = iconPackContext.resources

            //Try to find the Resource ID for res/xml/appfilter.xml
            val resId = res.getIdentifier("appfilter", "xml", iconPackPackage)

            val parser: XmlPullParser = if (resId != 0) {
                // Found in resources
                res.getXml(resId)
            } else {
                // Fallback: Try assets/appfilter.xml
                val inputStream = iconPackContext.assets.open("appfilter.xml")
                val factory = XmlPullParserFactory.newInstance()
                factory.newPullParser().apply {
                    setInput(inputStream, "UTF-8")
                }
            }

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    component?.let {
                        // Extract: ComponentInfo{com.package/com.package.Activity} -> com.package/com.package.Activity
                        val cleaned = it.substringAfter("{").substringBefore("}")
                        components.add(cleaned)
                    }
                }
                eventType = parser.next()
            }

        } catch (e: Exception) {
            // This will now catch both the case where the file is missing
            // and any XML parsing errors.
            Log.e("IconPackManager", "Error parsing appfilter for $iconPackPackage", e)
        }
        return components
    }

    data class IconPackReport(
        val packPackageName: String,
        val duplicates: Map<String, List<String>>, // Key: Component, Value: List of Drawable Names
        val missingDrawables: List<String>,
        val totalEntries: Int
    )

    @SuppressLint("DiscouragedApi")
    fun checkIconPackHealth(iconPackPackage: String): IconPackReport {
        val componentMap = mutableMapOf<String, MutableList<String>>()
        val missingDrawables = mutableListOf<String>()
        var total = 0

        try {
            val packContext = context.createPackageContext(iconPackPackage, 0)
            val res = packContext.resources
            val existingDrawables = getDrawableResourceNames(res, iconPackPackage)

            val resId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (resId == 0) return IconPackReport(iconPackPackage, emptyMap(), emptyList(), 0)

            val parser = res.getXml(resId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    total++
                    val component = parser.getAttributeValue(null, "component")
                        ?.substringAfter("{")?.substringBefore("}")
                    val drawableName = parser.getAttributeValue(null, "drawable")

                    if (component != null && drawableName != null) {
                        componentMap.getOrPut(component) { mutableListOf() }.add(drawableName)

                        if (!existingDrawables.contains(drawableName)) {
                            missingDrawables.add("Component: $component -> Missing Drawable: $drawableName")
                        }
                    }
                }
                eventType = parser.next()
            }

            val duplicateMap = componentMap.filter { it.value.size > 1 }
            return IconPackReport(iconPackPackage, duplicateMap, missingDrawables, total)

        } catch (e: Exception) {
            Log.e("Checker", "Failed to check pack", e)
            return IconPackReport(iconPackPackage, emptyMap(), emptyList(), 0)
        }
    }

    private fun getDrawableResourceNames(res: Resources, packageName: String): Set<String> {
        mutableSetOf<String>()
        return object : HashSet<String>() {
            private val cache = mutableMapOf<String, Boolean>()

            @SuppressLint("DiscouragedApi")
            override fun contains(element: String): Boolean {
                return cache.getOrPut(element) {
                    res.getIdentifier(element, "drawable", packageName) != 0
                }
            }
        }
    }

    data class ComparisonResult(
        val appName: String,
        val packageName: String,
        val activityName: String,
        val packAIcon: String?, // Drawable name in Pack A
        val packBIcon: String?  // Drawable name in Pack B
    )

    // Helper to get a Map of Component -> Drawable Name
    private fun parseAppFilter(iconPackPackage: String): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        try {
            val packContext = context.createPackageContext(iconPackPackage, 0)
            val res = packContext.resources
            val resId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (resId == 0) return emptyMap()

            val parser = res.getXml(resId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        mapping[component] = drawable
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to parse for comparison", e)
        }
        return mapping
    }

    fun getUniqueDrawableCount(iconPackPackage: String): Int {
        try {
            val packContext = context.createPackageContext(iconPackPackage, 0)
            val res = packContext.resources
            val resId = res.getIdentifier("drawable", "xml", iconPackPackage)
            if (resId == 0) return 0

            val drawables = mutableSetOf<String>()
            val parser = res.getXml(resId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                // Usually <item drawable="icon_name" />
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    if (drawableName != null) drawables.add(drawableName)
                }
                eventType = parser.next()
            }
            return drawables.size
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to parse drawable.xml", e)
            return 0
        }
    }

    data class ComparisonData(
        val results: List<ComparisonResult>,
        val mapA: Map<String, String>,
        val mapB: Map<String, String>
    )

    fun comparePacks(
        packA: String,
        packB: String,
        installedApps: List<AppInfo>? = null
    ): ComparisonData { // Updated return type
        val mapA = if (packA == SYSTEM_FILTER_PACKAGE) emptyMap() else parseAppFilter(packA)
        val mapB = if (packB == SYSTEM_FILTER_PACKAGE) emptyMap() else parseAppFilter(packB)

        val results = installedApps?.// Mode: Installed Apps Only
        map { app ->
            val componentKey = "ComponentInfo{${app.packageName}/${app.activityName}}"
            ComparisonResult(
                appName = app.name,
                packageName = app.packageName,
                activityName = app.activityName,
                packAIcon = if (packA == SYSTEM_FILTER_PACKAGE) "placeholder" else mapA[componentKey],
                packBIcon = if (packB == SYSTEM_FILTER_PACKAGE) "placeholder" else mapB[componentKey]
            )
        }
            ?: // Mode: Full Icon Pack Comparison
            (mapA.keys + mapB.keys).distinct().map { key ->
                val component = key.substringAfter("{").substringBefore("}")
                ComparisonResult(
                    appName = "",
                    packageName = component.substringBefore("/"),
                    activityName = component.substringAfter("/"),
                    packAIcon = if (packA == SYSTEM_FILTER_PACKAGE) "placeholder" else mapA[key],
                    packBIcon = if (packB == SYSTEM_FILTER_PACKAGE) "placeholder" else mapB[key]
                )
            }

        return ComparisonData(results, mapA, mapB)
    }
}