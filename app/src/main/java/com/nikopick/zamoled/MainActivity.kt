package com.nikopick.zamoled

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.PhotoLibrary
import com.nikopick.zamoled.ui.ZamoledApp
import com.nikopick.zamoled.ui.ZamoledTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        Screen.init(this)
        val assetManager = applicationContext.assets
        PhotoLibrary.loader = { asset, targetWidth ->
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                assetManager.open("portraits/$asset.webp").use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                assetManager.open("portraits/$asset.webp").use { BitmapFactory.decodeStream(it, null, opts) }
            } catch (e: Exception) {
                null
            }
        }
        setContent {
            ZamoledTheme {
                ZamoledApp()
            }
        }
    }
}
