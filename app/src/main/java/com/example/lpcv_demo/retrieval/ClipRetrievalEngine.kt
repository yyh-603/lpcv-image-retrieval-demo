package com.example.lpcv_demo.retrieval

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.lpcv_demo.data.EmbeddingAssetLoader
import com.example.lpcv_demo.data.ImageFileResolver
import com.example.lpcv_demo.data.ImagePreprocessor
import com.example.lpcv_demo.data.TextAssetLoader
import com.example.lpcv_demo.inference.SnpeImageEncoder
import com.example.lpcv_demo.model.ImageEncoderModel
import com.example.lpcv_demo.model.ImageEncoderModels
import com.example.lpcv_demo.model.RetrievalResult
import java.io.Closeable

class ClipRetrievalEngine(
    private val context: Context
) : Closeable {
    private val tag = "RetrievalLatency"
    private val embeddingDim = 768
    val availableModels: List<ImageEncoderModel> = ImageEncoderModels.Available

    var selectedModel: ImageEncoderModel = ImageEncoderModels.Default
        private set

    var modelVersion: Int = 0
        private set

    private val imageEncoders: Map<ImageEncoderModel, SnpeImageEncoder> =
        availableModels.associateWith { model -> createImageEncoder(model) }

    private var imageEncoder: SnpeImageEncoder = imageEncoders.getValue(selectedModel)

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

    @Synchronized
    fun preloadAllModels() {
        val startedAtNs = System.nanoTime()
        imageEncoders.forEach { (model, encoder) ->
            val modelStartedAtNs = System.nanoTime()
            encoder.initialize()
            Log.d(
                tag,
                "Preloaded model=${model.displayName} " +
                    "asset=${model.assetName} " +
                    "elapsed=${formatMs(elapsedMs(modelStartedAtNs))}ms"
            )
        }
        Log.d(
            tag,
            "Preloaded ${imageEncoders.size} image encoder models " +
                "total=${formatMs(elapsedMs(startedAtNs))}ms"
        )
    }

    @Synchronized
    fun selectModel(model: ImageEncoderModel): Boolean {
        if (model == selectedModel) {
            return false
        }

        selectedModel = model
        imageEncoder = imageEncoders.getValue(model)
        modelVersion += 1
        Log.d("MyApp", "Selected image encoder model = ${model.displayName} (${model.assetName})")
        return true
    }

    @Synchronized
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

    @Synchronized
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
                "model=${selectedModel.displayName} asset=${selectedModel.assetName} " +
                "preprocess=${formatMs(preprocessLatencyMs)}ms " +
                "inference=${formatMs(modelLatencyMs)}ms " +
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

    private fun createImageEncoder(model: ImageEncoderModel): SnpeImageEncoder {
        return SnpeImageEncoder(
            context = context.applicationContext,
            model = model
        )
    }

    @Synchronized
    override fun close() {
        imageEncoders.values.forEach { encoder ->
            encoder.close()
        }
    }
}
