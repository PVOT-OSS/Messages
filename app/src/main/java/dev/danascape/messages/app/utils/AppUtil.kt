// From https://medium.com/@KaushalVasava/android-15-edge-to-edge-support-how-to-implement-in-xml-views-59a65d73f1c9

package dev.danascape.messages.app.utils

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object AppUtil {
    fun Activity.applyEdgeToEdgeInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val view = findViewById<View>(android.R.id.content)
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
                )
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )?.isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )?.isAppearanceLightNavigationBars = true
                v.updatePadding(
                    left = bars.left,
                    top = bars.top,
                    right = bars.right,
                    bottom = bars.bottom,
                )
                windowInsets
            }
        }
    }
}