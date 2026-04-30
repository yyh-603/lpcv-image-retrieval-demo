package com.example.lpcv_demo.retrieval

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.lpcv_demo.data.EmbeddingAssetLoader
import com.example.lpcv_demo.data.ImageFileResolver
import com.example.lpcv_demo.data.ImagePreprocessor
import com.example.lpcv_demo.data.TextAssetLoader
import com.example.lpcv_demo.model.RetrievalResult
import com.example.lpcv_demo.inference.SnpeImageEncoder

class ClipRetrievalEngine(
    private val context: Context
) {
    private val tag = "RetrievalLatency"
    private val embeddingDim = 768

    private val imageEncoder by lazy {
        SnpeImageEncoder(context.applicationContext)
    }

    private val texts: List<String> by lazy {
        TextAssetLoader.loadTexts(context)
    }

    private val textEmbeddings: Array<FloatArray> by lazy {
        EmbeddingAssetLoader.loadFloat32Embeddings(
            context = context,
            numRows = texts.size,
            dim = embeddingDim
        )
    }

    fun retrieveTopK(
        imageUri: Uri,
        k: Int = 5
    ): List<RetrievalResult> {
        imageEncoder.initialize()

        Log.d("MyApp", "retrieveTopK imageUri = $imageUri")

        val imagePath = ImageFileResolver.copyUriToInternalFile(
            context = context,
            uri = imageUri
        )

        Log.d("MyApp", "Copied image to internal path = $imagePath")

        val source = "gallery"
        val totalStartedAtNs = System.nanoTime()
        val preprocessStartedAtNs = System.nanoTime()
        val inputTensor = ImagePreprocessor.preprocessImageFile(imagePath)
        val preprocessLatencyMs = elapsedMs(preprocessStartedAtNs)

        return retrieveTopK(
            inputTensor = inputTensor,
            k = k,
            source = source,
            totalStartedAtNs = totalStartedAtNs,
            preprocessLatencyMs = preprocessLatencyMs
        )
    }

    fun retrieveTopK(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        k: Int = 5
    ): List<RetrievalResult> {
        imageEncoder.initialize()

        Log.d("MyApp", "retrieveTopK bitmap = ${bitmap.width} x ${bitmap.height}, rotation = $rotationDegrees")

        val source = "live_camera"
        val totalStartedAtNs = System.nanoTime()
        val preprocessStartedAtNs = System.nanoTime()
        val inputTensor = ImagePreprocessor.preprocessBitmap(
            bitmap = bitmap,
            rotationDegrees = rotationDegrees
        )
        val preprocessLatencyMs = elapsedMs(preprocessStartedAtNs)

        return retrieveTopK(
            inputTensor = inputTensor,
            k = k,
            source = source,
            totalStartedAtNs = totalStartedAtNs,
            preprocessLatencyMs = preprocessLatencyMs
        )
    }

    private fun retrieveTopK(
        inputTensor: FloatArray,
        k: Int,
        source: String,
        totalStartedAtNs: Long,
        preprocessLatencyMs: Float
    ): List<RetrievalResult> {
        val modelStartedAtNs = System.nanoTime()
        val imageEmbedding = imageEncoder.encode(inputTensor)
        val modelLatencyMs = elapsedMs(modelStartedAtNs)

        Log.d("MyApp", "SNPE image embedding dim = ${imageEmbedding.size}")

        val retrievalStartedAtNs = System.nanoTime()
        val results = SimilaritySearch.topK(
            imageEmbedding = imageEmbedding,
            textEmbeddings = textEmbeddings,
            texts = texts,
            k = k
        )
        val retrievalLatencyMs = elapsedMs(retrievalStartedAtNs)
        val totalLatencyMs = elapsedMs(totalStartedAtNs)

        Log.d(
            tag,
            "Latency source=$source " +
                "preprocess=${formatMs(preprocessLatencyMs)}ms " +
                "model=${formatMs(modelLatencyMs)}ms " +
                "retrieval=${formatMs(retrievalLatencyMs)}ms " +
                "total=${formatMs(totalLatencyMs)}ms"
        )

        return results
    }

    private fun elapsedMs(startedAtNs: Long): Float {
        return (System.nanoTime() - startedAtNs) / 1_000_000.0f
    }

    private fun formatMs(value: Float): String {
        return "%.2f".format(value)
    }
}
