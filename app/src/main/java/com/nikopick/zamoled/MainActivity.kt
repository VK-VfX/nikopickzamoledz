package com.nikopick.zamoled

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nikopick.zamoled.data.Screen
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
        setContent {
            ZamoledTheme {
                ZamoledApp()
            }
        }
    }
}
