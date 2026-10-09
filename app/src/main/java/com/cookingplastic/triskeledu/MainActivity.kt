package com.cookingplastic.triskeledu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cookingplastic.triskeledu.ui.components.AppScaffold
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContent {
            TriskeleduTheme {
                AppScaffold()
            }
        }
    }
}
