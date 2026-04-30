package com.example.lpcv_demo.data

import android.content.Context
import android.util.Log
import java.io.File

object AssetFileResolver {
    private const val TAG = "MyApp"

    fun copyAssetToInternalFile(
        context: Context,
        assetName: String,
        outputFileName: String = assetName,
        overwrite: Boolean = false
    ): String {
        val outputFile = File(context.filesDir, outputFileName)

        if (outputFile.exists() && !overwrite) {
            Log.d(TAG, "Asset already copied: ${outputFile.absolutePath}")
            return outputFile.absolutePath
        }

        context.assets.open(assetName).use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Log.d(TAG, "Copied asset $assetName to ${outputFile.absolutePath}, size=${outputFile.length()} bytes")

        return outputFile.absolutePath
    }
}