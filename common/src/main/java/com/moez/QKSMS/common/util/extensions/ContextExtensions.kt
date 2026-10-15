/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util.extensions

import android.app.job.JobScheduler
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import org.prauga.messages.util.tryOrNull

fun Context.getColorCompat(colorRes: Int): Int {
    // return black as a default color, in case an invalid color ID was passed in
    return tryOrNull { ContextCompat.getColor(this, colorRes) } ?: Color.BLACK
}

fun Context.getColorStateListCompat(colorStateListRes: Int): ColorStateList {
    // return black as a default color, in case an invalid color ID was passed in
    return tryOrNull { ContextCompat.getColorStateList(this, colorStateListRes) }
        ?: ColorStateList.valueOf(Color.BLACK)
}

/** Tries to resolve a resource id from the current theme, based on the [attributeId] */
fun Context.resolveThemeAttribute(attributeId: Int, default: Int = 0): Int {
    val outValue = TypedValue()
    val wasResolved = theme.resolveAttribute(attributeId, outValue, true)

    return if (wasResolved) outValue.resourceId else default
}

fun Context.resolveThemeBoolean(attributeId: Int, default: Boolean = false): Boolean {
    val outValue = TypedValue()
    val wasResolved = theme.resolveAttribute(attributeId, outValue, true)

    return if (wasResolved) outValue.data != 0 else default
}

fun Context.resolveThemeColor(attributeId: Int, default: Int = 0): Int {
    val outValue = TypedValue()
    val wasResolved = theme.resolveAttribute(attributeId, outValue, true)

    return if (wasResolved) getColorCompat(outValue.resourceId) else default
}

fun Context.resolveThemeColorStateList(attributeId: Int, default: Int = 0): ColorStateList {
    val outValue = TypedValue()
    val wasResolved = theme.resolveAttribute(attributeId, outValue, true)

    return getColorStateListCompat(if (wasResolved) outValue.resourceId else default)
}

fun Context.makeToast(@StringRes res: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, res, duration).show()
}

fun Context.makeToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.isInstalled(packageName: String): Boolean {
    return tryOrNull(false) { packageManager.getApplicationInfo(packageName, 0).enabled } ?: false
}

val Context.versionCode: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager
                .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .longVersionCode
                .toInt()
        } else {
            packageManager.getPackageInfo(packageName, 0).versionCode
        }
    }
val Context.jobScheduler: JobScheduler
    get() = getSystemService()!!
