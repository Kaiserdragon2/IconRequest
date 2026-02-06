package de.kaiserdragon.iconrequest.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sarsamurmu.adaptiveicon.AdaptiveIcon
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IconRequestExporter(private val context: Context) {

    suspend fun prepareShareIntent(selectedApps: List<AppInfo>): Intent =
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "requests").apply { mkdirs() }
            val zipFile = File(cacheDir, "IconRequest.zip")

            // Generate the data on a background thread
            zipFile.outputStream().use {
                generateZipData(selectedApps, it)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileProvider",
                zipFile
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    suspend fun saveToDownloads(selectedApps: List<AppInfo>): Boolean =
        withContext(Dispatchers.IO) {
            val fileName = "IconRequest_${System.currentTimeMillis()}.zip"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    uri?.let { outputUri ->
                        context.contentResolver.openOutputStream(outputUri)?.use {
                            generateZipData(selectedApps, it)
                        }
                        return@withContext true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadDir, fileName)
                    file.outputStream().use { generateZipData(selectedApps, it) }
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        }

    private fun generateZipData(
        selectedApps: List<AppInfo>,
        outputStream: OutputStream,
        excludeTags: Boolean = false
    ) {
        ZipOutputStream(outputStream).use { zipOut ->
            val usedNames = mutableMapOf<String, Int>()
            val appEntries = mutableListOf<Pair<AppInfo, String>>()

            // Step 1: Pre-calculate unique names to sync XML and PNGs
            selectedApps.forEach { app ->
                val baseName = getDrawableName(app)
                val count = usedNames.getOrDefault(baseName, 0)

                val finalName = if (count == 0) baseName else "${baseName}_$count"
                usedNames[baseName] = count + 1

                appEntries.add(app to finalName)
            }

            // Step 2: Generate appfilter.xml using the synced names
            val appFilterContent = buildAppFilterString(appEntries, excludeTags)
            addToZip(zipOut, "appfilter.xml", appFilterContent.toByteArray())

            // Step 3: Add Icons using the exact same synced names
            appEntries.forEach { (app, drawableName) ->
                val bitmap = app.icon.toBitmap(192)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                // Matches exactly what is in the XML
                addToZip(zipOut, "icons/$drawableName.png", stream.toByteArray())
            }
        }
    }

    fun getDrawableName(app: AppInfo): String {
        var name = app.name.lowercase()
            .replace(Regex("[^a-z0-9_]"), "_") // Only allow valid resource chars
            .replace(Regex("_+"), "_")         // Clean up double underscores
            .trim('_')

        // Prefix with underscore if the first character is a number
        if (name.isNotEmpty() && name[0].isDigit()) {
            name = "_$name"
        }

        // Fallback for empty strings (e.g., if the app name was only symbols)
        return name.ifEmpty { "icon_request" }
    }

    fun buildAppFilterString(
        entries: List<Pair<AppInfo, String>>,
        excludeTags: Boolean
    ): String {
        val builder = StringBuilder()
        if (!excludeTags) {
            builder.append("<resources>\n")
        }
        entries.forEach { (app, drawableName) ->
            builder.append(
                "  <item component=\"ComponentInfo{${app.packageName}/${app.activityName}}\" drawable=\"$drawableName\" />\n"
            )
        }
        if (!excludeTags) builder.append("</resources>")
        return builder.toString()
    }

    fun Drawable.toBitmap(size: Int = 256): Bitmap {
        // 1. Handle Adaptive Icons (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
            return AdaptiveIcon()
                .setDrawable(this)
                .setPath(AdaptiveIcon.PATH_SQUARE) // Or PATH_CIRCLE, PATH_ROUNDED_SQUARE, etc.
                .setSize(size)
                .render()
        }
        // 2. Fallback for Legacy Icons / Vector Drawables (API 23+)
        // We use the intrinsic dimensions if available, otherwise fallback to the requested size
        val width = if (intrinsicWidth > 0) intrinsicWidth else size
        val height = if (intrinsicHeight > 0) intrinsicHeight else size

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)

        // Scale the legacy bitmap to the desired size if it doesn't match
        return if (width != size || height != size) {
            Bitmap.createScaledBitmap(bitmap, size, size, true)
        } else {
            bitmap
        }
    }

    private fun addToZip(zipOut: ZipOutputStream, fileName: String, content: ByteArray) {
        zipOut.putNextEntry(ZipEntry(fileName))
        zipOut.write(content)
        zipOut.closeEntry()
    }

}