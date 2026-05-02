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
import androidx.lifecycle.lifecycleScope
import com.example.lpcv_demo.retrieval.ClipRetrievalEngine
import com.example.lpcv_demo.ui.screen.ImageRetrievalDemoScreen
import com.example.lpcv_demo.ui.theme.Lpcv_demoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var retrievalEngine: ClipRetrievalEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyApp", "MainActivity onCreate")

        retrievalEngine = ClipRetrievalEngine(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            retrievalEngine.preloadAllModels()
        }

        enableEdgeToEdge()

        setContent {
            Lpcv_demoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageRetrievalDemoScreen(
                        retrievalEngine = retrievalEngine,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (::retrievalEngine.isInitialized) {
            retrievalEngine.close()
        }
        super.onDestroy()
    }
}
