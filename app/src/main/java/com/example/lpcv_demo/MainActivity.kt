package com.example.lpcv_demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.lpcv_demo.ui.screen.ImageRetrievalDemoScreen
import com.example.lpcv_demo.ui.theme.Lpcv_demoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyApp", "MainActivity onCreate")

        enableEdgeToEdge()

        setContent {
            Lpcv_demoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageRetrievalDemoScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}