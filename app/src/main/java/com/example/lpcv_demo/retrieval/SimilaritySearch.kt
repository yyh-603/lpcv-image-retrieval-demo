package com.example.lpcv_demo.retrieval

import com.example.lpcv_demo.model.RetrievalResult
import kotlin.math.sqrt

object SimilaritySearch {
    fun topK(
        imageEmbedding: FloatArray,
        textEmbeddings: Array<FloatArray>,
        texts: List<String>,
        k: Int
    ): List<RetrievalResult> {
        require(textEmbeddings.size == texts.size) {
            "textEmbeddings size ${textEmbeddings.size} does not match texts size ${texts.size}"
        }

        require(textEmbeddings.isNotEmpty()) {
            "textEmbeddings is empty"
        }

        val dim = imageEmbedding.size

        for ((index, emb) in textEmbeddings.withIndex()) {
            require(emb.size == dim) {
                "Embedding dimension mismatch at row $index: image dim=$dim, text dim=${emb.size}"
            }
        }

        val normalizedImage = l2Normalize(imageEmbedding)

        val scores = FloatArray(textEmbeddings.size)

        for (i in textEmbeddings.indices) {
            scores[i] = dot(normalizedImage, textEmbeddings[i])
        }

        return scores.indices
            .sortedByDescending { scores[it] }
            .take(k)
            .mapIndexed { rankIndex, textIndex ->
                RetrievalResult(
                    rank = rankIndex + 1,
                    text = texts[textIndex],
                    score = scores[textIndex]
                )
            }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0.0f

        for (i in a.indices) {
            sum += a[i] * b[i]
        }

        return sum
    }

    private fun l2Normalize(x: FloatArray): FloatArray {
        var sum = 0.0f

        for (v in x) {
            sum += v * v
        }

        val norm = sqrt(sum)

        if (norm == 0.0f) {
            return x.copyOf()
        }

        return FloatArray(x.size) { i ->
            x[i] / norm
        }
    }
}