package com.example.lpcv_demo.data

import android.content.Context
import org.json.JSONArray

object TextAssetLoader {
    fun loadTexts(
        context: Context,
        assetName: String = "texts.json"
    ): List<String> {
        val jsonString = context.assets.open(assetName).bufferedReader().use {
            it.readText()
        }

        val jsonArray = JSONArray(jsonString)
        val texts = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            texts.add(jsonArray.getString(i))
        }

        return texts
    }
}