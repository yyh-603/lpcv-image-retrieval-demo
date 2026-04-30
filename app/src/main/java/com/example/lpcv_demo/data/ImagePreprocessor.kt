package com.example.lpcv_demo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import kotlin.math.min

object ImagePreprocessor {
    private const val TAG = "MyApp"

    const val INPUT_WIDTH = 224
    const val INPUT_HEIGHT = 224
    const val CHANNELS = 3

    private val mean = floatArrayOf(
        0.48145466f,
        0.4578275f,
        0.40821073f
    )

    private val std = floatArrayOf(
        0.26862954f,
        0.26130258f,
        0.27577711f
    )

    fun preprocessImageFile(
        imagePath: String,
        width: Int = INPUT_WIDTH,
        height: Int = INPUT_HEIGHT
    ): FloatArray {
        val originalBitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IllegalArgumentException("Failed to decode image: $imagePath")

        Log.d(TAG, "Original bitmap size = ${originalBitmap.width} x ${originalBitmap.height}")

        val croppedBitmap = centerCropToSquare(originalBitmap)
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, true)

        Log.d(TAG, "Preprocessed bitmap size = ${resizedBitmap.width} x ${resizedBitmap.height}")

        val tensor = bitmapToNormalizedNchw(resizedBitmap, width, height)

        if (croppedBitmap != originalBitmap) {
            croppedBitmap.recycle()
        }

        if (resizedBitmap != croppedBitmap) {
            resizedBitmap.recycle()
        }

        originalBitmap.recycle()

        Log.d(TAG, "Image tensor size = ${tensor.size}")
        Log.d(TAG, "Image tensor first values = ${tensor.take(8)}")

        return tensor
    }

    fun preprocessBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        width: Int = INPUT_WIDTH,
        height: Int = INPUT_HEIGHT
    ): FloatArray {
        Log.d(TAG, "Original frame bitmap size = ${bitmap.width} x ${bitmap.height}")

        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
        val croppedBitmap = centerCropToSquare(rotatedBitmap)
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, true)

        Log.d(TAG, "Preprocessed frame bitmap size = ${resizedBitmap.width} x ${resizedBitmap.height}")

        val tensor = bitmapToNormalizedNchw(resizedBitmap, width, height)

        if (croppedBitmap != rotatedBitmap) {
            croppedBitmap.recycle()
        }

        if (resizedBitmap != croppedBitmap) {
            resizedBitmap.recycle()
        }

        if (rotatedBitmap != bitmap) {
            rotatedBitmap.recycle()
        }

        Log.d(TAG, "Frame image tensor size = ${tensor.size}")
        return tensor
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360

        if (normalizedRotation == 0) {
            return bitmap
        }

        val matrix = Matrix().apply {
            postRotate(normalizedRotation.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun centerCropToSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)

        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2

        return Bitmap.createBitmap(
            bitmap,
            xOffset,
            yOffset,
            size,
            size
        )
    }

    private fun bitmapToNormalizedNchw(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): FloatArray {
        val tensor = FloatArray(CHANNELS * height * width)

        val pixels = IntArray(width * height)
        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixelIndex = y * width + x
                val pixel = pixels[pixelIndex]

                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                val rNorm = (r - mean[0]) / std[0]
                val gNorm = (g - mean[1]) / std[1]
                val bNorm = (b - mean[2]) / std[2]

                // NCHW layout: [C, H, W]
                tensor[0 * height * width + y * width + x] = rNorm
                tensor[1 * height * width + y * width + x] = gNorm
                tensor[2 * height * width + y * width + x] = bNorm
            }
        }

        return tensor
    }
}
