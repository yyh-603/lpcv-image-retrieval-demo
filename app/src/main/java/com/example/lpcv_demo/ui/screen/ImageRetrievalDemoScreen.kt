package com.example.lpcv_demo.ui.screen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.lpcv_demo.model
    .RetrievalResult
import com.example.lpcv_demo.retrieval.ClipRetrievalEngine

@Composable
fun ImageRetrievalDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val retrievalEngine = remember {
        ClipRetrievalEngine(context.applicationContext)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val results = remember {
        mutableStateListOf<RetrievalResult>()
    }

    var statusText by remember {
        mutableStateOf("No image selected")
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        results.clear()

        statusText = if (uri == null) {
            "No image selected"
        } else {
            "Image selected"
        }

        Log.d("MyApp", "Selected image uri = $uri")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CLIP Image Retrieval Demo",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                imagePickerLauncher.launch("image/*")
            }
        ) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = statusText)

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedImageUri),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val uri = selectedImageUri

                    if (uri == null) {
                        statusText = "Please select an image first"
                        return@Button
                    }

                    try {
                        Log.d("MyApp", "Run text retrieval with fake image embedding")

                        statusText = "Running retrieval..."

                        val topKResults = retrievalEngine.retrieveTopK(
                            imageUri = uri,
                            k = 5
                        )

                        results.clear()
                        results.addAll(topKResults)

                        statusText = "Retrieval finished"
                    } catch (e: Exception) {
                        Log.e("MyApp", "Retrieval failed", e)
                        statusText = "Retrieval failed: ${e.message}"
                    }
                }
            ) {
                Text("Run Retrieval")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (results.isNotEmpty()) {
            RetrievalResultCard(results = results)
        }
    }
}

@Composable
private fun RetrievalResultCard(results: List<RetrievalResult>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Top-${results.size} Results",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            results.forEach { result ->
                Text(
                    text = "${result.rank}. ${result.text}"
                )

                Text(
                    text = "score = ${"%.4f".format(result.score)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}