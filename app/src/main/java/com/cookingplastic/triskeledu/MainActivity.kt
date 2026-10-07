package com.cookingplastic.triskeledu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cookingplastic.triskeledu.ui.components.AppScaffold
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TriskeleduTheme {
                AppScaffold()
            }
        }
    }
}
