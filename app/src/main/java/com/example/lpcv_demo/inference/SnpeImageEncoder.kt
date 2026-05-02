package com.example.lpcv_demo.inference

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.lpcv_demo.data.AssetFileResolver
import com.example.lpcv_demo.model.ImageEncoderModel
import com.qualcomm.qti.snpe.FloatTensor
import com.qualcomm.qti.snpe.NeuralNetwork
import com.qualcomm.qti.snpe.SNPE
import com.qualcomm.qti.snpe.Tensor
import java.io.Closeable
import java.util.Arrays

class SnpeImageEncoder(
    private val context: Context,
    val model: ImageEncoderModel
) : Closeable {
    private var neuralNetwork: NeuralNetwork? = null
    private var inputLayer: String = INPUT_LAYER
    private var outputLayer: String? = null

    fun initialize() {
        if (neuralNetwork != null) {
            return
        }

        val application = context.applicationContext as? Application
            ?: throw IllegalStateException("SNPE requires an Application context")

        val modelPath = AssetFileResolver.copyAssetToInternalFile(
            context = context,
            assetName = model.assetName,
            outputFileName = model.assetName,
            overwrite = true
        )
        val modelFile = java.io.File(modelPath)

        Log.d(
            TAG,
            "Loading SNPE model name=${model.displayName} asset=${model.assetName} " +
                "path=$modelPath size=${modelFile.length()} bytes"
        )

        val inputDimensions = hashMapOf(INPUT_LAYER to INPUT_DIMENSIONS.copyOf())
        val network = SNPE.NeuralNetworkBuilder(application)
            .setDebugEnabled(false)
            .setRuntimeOrder(NeuralNetwork.Runtime.DSP)
            .setPerformanceProfile(NeuralNetwork.PerformanceProfile.BURST)
            .setModel(modelFile)
            .setInputDimensions(inputDimensions)
            .setCpuFallbackEnabled(true)
            .setUseUserSuppliedBuffers(false)
            .setUnsignedPD(true)
            .setRuntimeCheckOption(NeuralNetwork.RuntimeCheckOption.UNSIGNEDPD_CHECK)
            .build()

        val inputNames = network.inputTensorsNames
        require(inputNames.contains(INPUT_LAYER)) {
            "Expected SNPE input '$INPUT_LAYER', got $inputNames"
        }
        require(inputNames.size == 1) {
            "Expected one SNPE input tensor, got $inputNames"
        }

        val outputNames = network.outputTensorsNames
        require(outputNames.size == 1) {
            "Expected one SNPE output tensor, got $outputNames"
        }

        inputLayer = INPUT_LAYER
        outputLayer = outputNames.first()
        neuralNetwork = network

        val shape = network.inputTensorsShapes[inputLayer]
        Log.d(TAG, "SNPE input layer = $inputLayer")
        Log.d(TAG, "SNPE output layer = $outputLayer")
        Log.d(TAG, "SNPE input shape = ${shape?.contentToString()}")
        Log.d(TAG, "SNPE runtime = ${network.runtime}")
        Log.d(TAG, "Loaded SNPE model name=${model.displayName} asset=${model.assetName}")
    }

    fun encode(inputTensor: FloatArray): FloatArray {
        require(inputTensor.size == INPUT_TENSOR_SIZE) {
            "Invalid image input tensor size. Expected $INPUT_TENSOR_SIZE, got ${inputTensor.size}"
        }

        val network = neuralNetwork ?: run {
            initialize()
            neuralNetwork ?: throw IllegalStateException("SNPE image encoder failed to initialize")
        }

        val shape = network.inputTensorsShapes[inputLayer]
            ?: throw IllegalStateException("Missing SNPE input shape for $inputLayer")
        require(Arrays.equals(shape, INPUT_DIMENSIONS)) {
            "Unexpected SNPE input shape. Expected ${INPUT_DIMENSIONS.contentToString()}, got ${shape.contentToString()}"
        }

        val input = network.createFloatTensor(*shape)
        val inputs = hashMapOf<String, FloatTensor>()
        var outputs: Map<String, FloatTensor>? = null

        try {
            input.write(inputTensor, 0, inputTensor.size)
            inputs[inputLayer] = input
            outputs = network.execute(inputs)

            val outputName = outputLayer
                ?: throw IllegalStateException("SNPE output layer is not initialized")
            val output = outputs[outputName]
                ?: throw IllegalStateException("Missing SNPE output tensor '$outputName'. Available outputs: ${outputs.keys}")

            val embedding = FloatArray(output.size)
            output.read(embedding, 0, embedding.size)

            require(embedding.size == EMBEDDING_DIM) {
                "Invalid image embedding size. Expected $EMBEDDING_DIM, got ${embedding.size}"
            }

            Log.d(TAG, "SNPE image embedding size = ${embedding.size}")
            return embedding
        } finally {
            releaseTensors(inputs.values)
            outputs?.let { releaseTensors(it.values) }
        }
    }

    override fun close() {
        neuralNetwork?.release()
        neuralNetwork = null
        outputLayer = null
    }

    private fun releaseTensors(tensors: Collection<Tensor>) {
        tensors.forEach { it.release() }
    }

    companion object {
        private const val TAG = "SnpeImageEncoder"
        private const val INPUT_LAYER = "image"
        private const val EMBEDDING_DIM = 768
        private val INPUT_DIMENSIONS = intArrayOf(1, 3, 224, 224)
        private const val INPUT_TENSOR_SIZE = 1 * 3 * 224 * 224
    }
}
