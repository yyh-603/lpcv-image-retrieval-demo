package com.example.lpcv_demo.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.lpcv_demo.model.RetrievalResult
import com.example.lpcv_demo.retrieval.ClipRetrievalEngine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MyApp"
private const val TOP_K = 5
private const val LIVE_RETRIEVAL_INTERVAL_MS = 1_000L

@Composable
fun ImageRetrievalDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val retrievalEngine = remember {
        ClipRetrievalEngine(context.applicationContext)
    }

    var screenMode by remember { mutableStateOf(RetrievalScreenMode.Gallery) }

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

        Spacer(modifier = Modifier.height(16.dp))

        ModeSwitcher(
            selectedMode = screenMode,
            onModeSelected = { screenMode = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (screenMode) {
            RetrievalScreenMode.Gallery -> GalleryRetrievalContent(
                retrievalEngine = retrievalEngine
            )

            RetrievalScreenMode.LiveCamera -> LiveCameraRetrievalContent(
                retrievalEngine = retrievalEngine
            )
        }
    }
}

@Composable
private fun ModeSwitcher(
    selectedMode: RetrievalScreenMode,
    onModeSelected: (RetrievalScreenMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        RetrievalScreenMode.entries.forEachIndexed { index, mode ->
            val buttonModifier = Modifier.weight(1f)

            if (selectedMode == mode) {
                Button(
                    modifier = buttonModifier,
                    onClick = { onModeSelected(mode) }
                ) {
                    Text(mode.label)
                }
            } else {
                OutlinedButton(
                    modifier = buttonModifier,
                    onClick = { onModeSelected(mode) }
                ) {
                    Text(mode.label)
                }
            }

            if (index != RetrievalScreenMode.entries.lastIndex) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun GalleryRetrievalContent(
    retrievalEngine: ClipRetrievalEngine
) {
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

        Log.d(TAG, "Selected image uri = $uri")
    }

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
                    Log.d(TAG, "Run text retrieval from gallery image")

                    statusText = "Running retrieval..."

                    val topKResults = retrievalEngine.retrieveTopK(
                        imageUri = uri,
                        k = TOP_K
                    )

                    results.clear()
                    results.addAll(topKResults)

                    statusText = "Retrieval finished"
                } catch (e: Exception) {
                    Log.e(TAG, "Retrieval failed", e)
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

@Composable
private fun LiveCameraRetrievalContent(
    retrievalEngine: ClipRetrievalEngine
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var isAnalyzing by remember { mutableStateOf(true) }
    var statusText by remember {
        mutableStateOf(
            if (hasCameraPermission) {
                "Live camera ready"
            } else {
                "Camera permission required"
            }
        )
    }
    val results = remember {
        mutableStateListOf<RetrievalResult>()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        statusText = if (granted) {
            "Live camera ready"
        } else {
            "Camera permission denied"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Text(text = statusText)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        ) {
            Text("Grant Camera Permission")
        }

        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        LiveCameraPreview(
            retrievalEngine = retrievalEngine,
            isAnalyzing = isAnalyzing,
            onStatusChanged = { statusText = it },
            onResultsChanged = { topKResults ->
                results.clear()
                results.addAll(topKResults)
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                isAnalyzing = !isAnalyzing
                statusText = if (isAnalyzing) {
                    "Live retrieval resumed"
                } else {
                    "Live retrieval paused"
                }
            }
        ) {
            Text(if (isAnalyzing) "Stop Analysis" else "Start Analysis")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(text = statusText)

    Spacer(modifier = Modifier.height(24.dp))

    if (results.isNotEmpty()) {
        RetrievalResultCard(results = results)
    }
}

@Composable
private fun LiveCameraPreview(
    retrievalEngine: ClipRetrievalEngine,
    isAnalyzing: Boolean,
    onStatusChanged: (String) -> Unit,
    onResultsChanged: (List<RetrievalResult>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(context, lifecycleOwner, previewView, retrievalEngine, isAnalyzing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainHandler = Handler(Looper.getMainLooper())
        val isInferenceRunning = AtomicBoolean(false)
        var lastAnalyzedAtMs = 0L

        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        val canAnalyze = isAnalyzing &&
                            now - lastAnalyzedAtMs >= LIVE_RETRIEVAL_INTERVAL_MS &&
                            isInferenceRunning.compareAndSet(false, true)

                        if (!canAnalyze) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        lastAnalyzedAtMs = now

                        try {
                            val bitmap = imageProxy.toBitmapFromRgbaPlane()
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                            val topKResults = try {
                                retrievalEngine.retrieveTopK(
                                    bitmap = bitmap,
                                    rotationDegrees = rotationDegrees,
                                    k = TOP_K
                                )
                            } finally {
                                bitmap.recycle()
                            }

                            mainHandler.post {
                                onResultsChanged(topKResults)
                                onStatusChanged("Live retrieval updated")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Live retrieval failed", e)
                            mainHandler.post {
                                onStatusChanged("Live retrieval failed: ${e.message}")
                            }
                        } finally {
                            isInferenceRunning.set(false)
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                onStatusChanged(
                    if (isAnalyzing) {
                        "Live camera running"
                    } else {
                        "Live camera paused"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                onStatusChanged("Camera failed: ${e.message}")
            }
        }

        cameraProviderFuture.addListener(
            listener,
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
            analysisExecutor.shutdown()
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

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun ImageProxy.toBitmapFromRgbaPlane(): Bitmap {
    val plane = planes.first()
    val buffer = plane.buffer
    buffer.rewind()

    val rowPaddingPixels = (plane.rowStride - plane.pixelStride * width) / plane.pixelStride
    val paddedWidth = width + rowPaddingPixels
    val paddedBitmap = Bitmap.createBitmap(
        paddedWidth,
        height,
        Bitmap.Config.ARGB_8888
    )
    paddedBitmap.copyPixelsFromBuffer(buffer)

    if (paddedWidth == width) {
        return paddedBitmap
    }

    val bitmap = Bitmap.createBitmap(
        paddedBitmap,
        0,
        0,
        width,
        height
    )
    paddedBitmap.recycle()
    return bitmap
}

private enum class RetrievalScreenMode(val label: String) {
    Gallery("Gallery"),
    LiveCamera("Live Camera")
}
