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

        val inputTensor = ImagePreprocessor.preprocessImageFile(imagePath)

        return retrieveTopK(inputTensor = inputTensor, k = k)
    }

    fun retrieveTopK(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        k: Int = 5
    ): List<RetrievalResult> {
        imageEncoder.initialize()

        Log.d("MyApp", "retrieveTopK bitmap = ${bitmap.width} x ${bitmap.height}, rotation = $rotationDegrees")

        val inputTensor = ImagePreprocessor.preprocessBitmap(
            bitmap = bitmap,
            rotationDegrees = rotationDegrees
        )

        return retrieveTopK(inputTensor = inputTensor, k = k)
    }

    private fun retrieveTopK(
        inputTensor: FloatArray,
        k: Int
    ): List<RetrievalResult> {
        val imageEmbedding = imageEncoder.encode(inputTensor)

        Log.d("MyApp", "SNPE image embedding dim = ${imageEmbedding.size}")

        return SimilaritySearch.topK(
            imageEmbedding = imageEmbedding,
            textEmbeddings = textEmbeddings,
            texts = texts,
            k = k
        )
    }
}
