package com.example.lpcv_demo.data

import android.content.Context
import android.net.Uri
import java.io.File

object ImageFileResolver {
    fun copyUriToInternalFile(
        context: Context,
        uri: Uri,
        fileName: String = "selected_image.jpg"
    ): String {
        val outputFile = File(context.filesDir, fileName)

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "Failed to open input stream for uri: $uri"
            }

            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outputFile.absolutePath
    }
}