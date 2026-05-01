package com.example.lpcv_demo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import kotlin.math.min

object ImagePreprocessor {
    private const val TAG = "MyApp"
    private const val BYTES_TO_FLOAT = 1.0f / 255.0f

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

    private val invStd = floatArrayOf(
        1.0f / std[0],
        1.0f / std[1],
        1.0f / std[2]
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

        val resizedBitmap = centerCropRotateAndResize(
            bitmap = bitmap,
            rotationDegrees = rotationDegrees,
            width = width,
            height = height
        )

        Log.d(TAG, "Preprocessed frame bitmap size = ${resizedBitmap.width} x ${resizedBitmap.height}")

        val tensor = bitmapToNormalizedNchw(resizedBitmap, width, height)

        resizedBitmap.recycle()

        Log.d(TAG, "Frame image tensor size = ${tensor.size}")
        return tensor
    }

    private fun centerCropRotateAndResize(
        bitmap: Bitmap,
        rotationDegrees: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val rotatedWidth = if (normalizedRotation == 90 || normalizedRotation == 270) {
            bitmap.height
        } else {
            bitmap.width
        }
        val rotatedHeight = if (normalizedRotation == 90 || normalizedRotation == 270) {
            bitmap.width
        } else {
            bitmap.height
        }
        val cropSize = min(rotatedWidth, rotatedHeight).toFloat()
        val cropX = (rotatedWidth - cropSize) * 0.5f
        val cropY = (rotatedHeight - cropSize) * 0.5f

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val sourceToRotated = Matrix().apply {
            postTranslate(-bitmap.width * 0.5f, -bitmap.height * 0.5f)
            postRotate(normalizedRotation.toFloat())
            postTranslate(rotatedWidth * 0.5f, rotatedHeight * 0.5f)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        Canvas(output).apply {
            scale(width / cropSize, height / cropSize)
            translate(-cropX, -cropY)
            concat(sourceToRotated)
            drawBitmap(bitmap, 0.0f, 0.0f, paint)
        }

        return output
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
        val channelSize = height * width
        val tensor = FloatArray(CHANNELS * channelSize)

        val pixels = IntArray(channelSize)
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

                val r = ((pixel shr 16) and 0xFF) * BYTES_TO_FLOAT
                val g = ((pixel shr 8) and 0xFF) * BYTES_TO_FLOAT
                val b = (pixel and 0xFF) * BYTES_TO_FLOAT

                // NCHW layout: [C, H, W]
                tensor[pixelIndex] = (r - mean[0]) * invStd[0]
                tensor[channelSize + pixelIndex] = (g - mean[1]) * invStd[1]
                tensor[channelSize * 2 + pixelIndex] = (b - mean[2]) * invStd[2]
            }
        }

        return tensor
    }
}
