package com.example.lpcv_demo.data

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

object EmbeddingAssetLoader {
    fun loadFloat32Embeddings(
        context: Context,
        assetName: String = "text_embeddings.bin",
        numRows: Int,
        dim: Int
    ): Array<FloatArray> {
        val bytes = context.assets.open(assetName).use {
            it.readBytes()
        }

        val expectedBytes = numRows * dim * 4
        require(bytes.size == expectedBytes) {
            "Invalid embedding file size. Expected $expectedBytes bytes, got ${bytes.size} bytes."
        }

        val buffer = ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)

        return Array(numRows) {
            FloatArray(dim) {
                buffer.float
            }
        }
    }
}