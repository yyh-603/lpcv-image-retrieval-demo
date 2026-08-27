# LPCV Image Retrieval Demo

簡易的 Android CLIP Image Retrieval Demo。使用者可以從裝置選擇一張圖片，App 會透過 SNPE 模型產生影像特徵，並和內建文字 embedding 做相似度比對，顯示 Top-K 檢索結果。

## 功能

- 使用 Jetpack Compose 建立 UI
- 從相簿選擇圖片
- 使用 SNPE 執行影像 encoder 模型
- 載入內建文字資料與 embedding
- 顯示圖片與最相近的文字檢索結果

## 專案需求

- Android Studio
- JDK 11
- Android SDK 36
- 支援 `arm64-v8a` 的 Android 裝置
- Qualcomm SNPE Android Runtime AAR
- Image encoder DLC 模型檔

## 主要檔案

```text
app/src/main/java/com/example/lpcv_demo/
├── MainActivity.kt
├── data/                 # Asset 載入、圖片前處理
├── inference/            # SNPE image encoder
├── retrieval/            # 相似度搜尋與 retrieval engine
├── model/                # 資料模型
└── ui/                   # Compose UI 與主題

app/src/main/assets/
├── image_encoder.dlc     # 不放入 Git，需本機自行準備
├── image_encoder_quant.dlc # 不放入 Git，需本機自行準備
├── inception_v3_quantized.dlc
├── text_embeddings.bin
└── texts.json

app/libs/
└── snpe-release.aar      # 不放入 Git，需本機自行準備
```

## 大檔案準備

GitHub 一般 Git repository 不接受超過 100 MB 的檔案，且 50 MB 以上會收到警告。因此下列檔案不會放入 Git：

- `app/libs/snpe-release.aar`
- `app/src/main/assets/image_encoder.dlc`
- `app/src/main/assets/image_encoder_quant.dlc`

建置前請自行將檔案放到對應位置：

```text
app/libs/snpe-release.aar
app/src/main/assets/image_encoder.dlc
app/src/main/assets/image_encoder_quant.dlc
```

若目錄不存在，請先建立：

```bash
mkdir -p app/libs app/src/main/assets
```

如果團隊需要一起管理這些大檔案，建議改用 Git LFS：

```bash
git lfs install
git lfs track "app/libs/snpe-release.aar"
git lfs track "app/src/main/assets/image_encoder.dlc"
git lfs track "app/src/main/assets/image_encoder_quant.dlc"
git add .gitattributes
```

## 建置與執行

1. 準備 `snpe-release.aar`、`image_encoder.dlc` 與 `image_encoder_quant.dlc`，並放到上方指定位置。
2. 使用 Android Studio 開啟專案。
3. 等待 Gradle Sync 完成。
4. 連接支援 `arm64-v8a` 的 Android 裝置。
5. 執行 `app` configuration。

也可以使用命令列建置：

```bash
./gradlew assembleDebug
```

## 注意事項

- 專案依賴 `app/libs/snpe-release.aar`，但此檔案不提交到 Git。
- `app/src/main/assets/image_encoder.dlc` 與 `app/src/main/assets/image_encoder_quant.dlc` 不提交到 Git，建置前需自行放入。
- 其他模型與檢索資料放在 `app/src/main/assets/`。
- 目前 Gradle 設定只打包 `arm64-v8a`。
- 若更換模型或文字資料，請確認輸入輸出格式與 embedding 維度一致。
