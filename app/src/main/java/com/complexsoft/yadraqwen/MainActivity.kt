package com.complexsoft.yadraqwen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.complexsoft.yadraqwen.ui.screens.ChatRootScreen
import com.complexsoft.yadraqwen.ui.theme.YadraQwenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YadraQwenTheme {
                ChatRootScreen()
            }
        }
    }
}