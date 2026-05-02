package com.example.lpcv_demo.model

data class ImageEncoderModel(
    val assetName: String,
    val displayName: String
)

object ImageEncoderModels {
    val Available: List<ImageEncoderModel> = listOf(
        ImageEncoderModel(
            assetName = "image_encoder.dlc",
            displayName = "Original"
        ),
        ImageEncoderModel(
            assetName = "image_encoder_quant.dlc",
            displayName = "Quantized"
        )
    )

    val Default: ImageEncoderModel = Available.first()
}
