package com.nikopick.zamoled

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.ArtLibrary
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
        val appAssets = applicationContext.assets
        ArtLibrary.loader = { asset, targetWidth ->
            try {
                val path = "art/$asset.webp"
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                appAssets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                // Decode no larger than needed: a thumbnail should not inflate a full-size picture.
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                appAssets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
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
