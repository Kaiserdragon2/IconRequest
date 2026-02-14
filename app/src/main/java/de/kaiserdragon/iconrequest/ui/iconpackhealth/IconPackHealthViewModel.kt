package de.kaiserdragon.iconrequest.ui.iconpackhealth

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        val reportText = getFormattedReportText(report)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }

    fun getFormattedReportText(
        report: IconPackManager.IconPackReport
    ): String {
        return StringBuilder().apply {
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

    }


    fun shareComprehensiveReport(context: Context) {
        val pkg = _currentPackage.value ?: return
        val report = healthReport.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
                val zipFile = File(cacheDir, "IconPack_Diagnostic_${pkg}.zip")

                ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                    // 1. Add the Health Report as a text file
                    val reportText = getFormattedReportText(report)
                    addFileToZip(zipOut, "health_report.txt", reportText.byteInputStream())

                    iconPackManager.getAppFilterFromAssets(pkg)?.let {
                        addFileToZip(zipOut, "appfilter_assets.xml", it.byteInputStream())
                    }
                    iconPackManager.getAppFilterFromRaw(pkg)?.let {
                        addFileToZip(zipOut, "appfilter_raw.xml", it.byteInputStream())
                    }
                    iconPackManager.getAppFilterFromBinary(pkg)?.let {
                        addFileToZip(zipOut, "appfilter_xml.xml", it.byteInputStream())
                    }
                    iconPackManager.getDrawable(pkg)?.let {
                        addFileToZip(zipOut, "drawable.xml", it.byteInputStream())
                    }
                }

                // Share the resulting ZIP
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileProvider",
                    zipFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Diagnostic Bundle"))

            } catch (e: Exception) {
                Log.e("IconPackHealth", "Failed to create ZIP", e)
            }
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, fileName: String, inputStream: InputStream) {
        val entry = ZipEntry(fileName)
        zipOut.putNextEntry(entry)
        inputStream.copyTo(zipOut)
        zipOut.closeEntry()
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