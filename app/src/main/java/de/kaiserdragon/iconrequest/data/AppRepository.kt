package de.kaiserdragon.iconrequest.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppRepository(private val context: Context) {
    private val iconPackManager = IconPackManager(context)
    fun getInstalledApps(
        showSystemApps: Boolean = false,
        showWidgets: Boolean = false
    ): Flow<List<AppInfo>> = flow {
        val pm = context.packageManager

        val intent = when {
            // Equivalent to your prepareDataShortcuts (Widgets/Shortcuts)
            showWidgets -> {
                Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
            }
            // Equivalent to your prepareDataActionMain (System/All Activities)
            showSystemApps -> {
                Intent(Intent.ACTION_MAIN)
            }
            // Standard Launcher filter
            else -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            }
        }

        val apps = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                icon = resolveInfo.loadIcon(pm)
            )
        }.sortedBy { it.name.lowercase() }

        emit(apps)
    }.flowOn(Dispatchers.IO) // Moves the work off the UI thread

    fun getIconPacks() = iconPackManager.getInstalledIconPacks()

    fun getMissingApps(allApps: List<AppInfo>, iconPackPackage: String): List<AppInfo> {
        val supported = iconPackManager.getSupportedComponents(iconPackPackage)
        return allApps.filter { app ->
            val comp = "${app.packageName}/${app.activityName}"
            !supported.contains(comp)
        }
    }
}