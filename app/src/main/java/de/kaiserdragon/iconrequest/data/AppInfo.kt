package de.kaiserdragon.iconrequest.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val isSelected: Boolean = false
)