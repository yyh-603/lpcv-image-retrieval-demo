# LPCV Image Retrieval Demo

A simple Android CLIP image retrieval demo. Users can select an image from the device, and the app runs an SNPE model to generate image features, compares them with bundled text embeddings, and shows the Top-K retrieval results.

## Features

- Jetpack Compose UI
- Image selection from the photo library
- SNPE image encoder inference
- Bundled text metadata and embeddings
- Top-K text retrieval results for the selected image

## Requirements

- Android Studio
- JDK 11
- Android SDK 36
- Android device with `arm64-v8a` support
- Qualcomm SNPE Android Runtime AAR
- Image encoder DLC model files

## Project Layout

```text
app/src/main/java/com/example/lpcv_demo/
├── MainActivity.kt
├── data/                 # Asset loading and image preprocessing
├── inference/            # SNPE image encoder
├── retrieval/            # Similarity search and retrieval engine
├── model/                # Data models
└── ui/                   # Compose UI and theme

app/src/main/assets/
├── image_encoder.dlc       # Not committed to Git; provide locally
├── image_encoder_quant.dlc # Not committed to Git; provide locally
├── inception_v3_quantized.dlc
├── text_embeddings.bin
└── texts.json

app/libs/
└── snpe-release.aar      # Not committed to Git; provide locally
```

## Large Local Files

GitHub regular Git repositories do not accept files larger than 100 MB, and files larger than 50 MB trigger warnings. For that reason, the following files are not committed to Git:

- `app/libs/snpe-release.aar`
- `app/src/main/assets/image_encoder.dlc`
- `app/src/main/assets/image_encoder_quant.dlc`

Before building the app, place these files at the required paths:

```text
app/libs/snpe-release.aar
app/src/main/assets/image_encoder.dlc
app/src/main/assets/image_encoder_quant.dlc
```

Create the directories first if they do not exist:

```bash
mkdir -p app/libs app/src/main/assets
```

If the team needs to version these large files together, use Git LFS:

```bash
git lfs install
git lfs track "app/libs/snpe-release.aar"
git lfs track "app/src/main/assets/image_encoder.dlc"
git lfs track "app/src/main/assets/image_encoder_quant.dlc"
git add .gitattributes
```

## Build and Run

1. Prepare `snpe-release.aar`, `image_encoder.dlc`, and `image_encoder_quant.dlc`, then place them at the paths listed above.
2. Open the project in Android Studio.
3. Wait for Gradle Sync to finish.
4. Connect an Android device with `arm64-v8a` support.
5. Run the `app` configuration.

You can also build from the command line:

```bash
./gradlew assembleDebug
```

## Notes

- The project depends on `app/libs/snpe-release.aar`, but this file is not committed to Git.
- `app/src/main/assets/image_encoder.dlc` and `app/src/main/assets/image_encoder_quant.dlc` are not committed to Git. Add them locally before building.
- Other model and retrieval data files are stored in `app/src/main/assets/`.
- The current Gradle configuration only packages `arm64-v8a`.
- If you replace the model or text data, make sure the input/output formats and embedding dimensions still match.
