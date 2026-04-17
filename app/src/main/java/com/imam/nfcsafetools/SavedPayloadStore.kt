package com.imam.nfcsafetools

import android.content.Context
import android.util.Base64
import org.json.JSONObject

class SavedPayloadStore(context: Context) {
    private val prefs = context.getSharedPreferences("nfc_safe_tools", Context.MODE_PRIVATE)

    fun save(name: String, rawNdefBytes: ByteArray, previewText: String) {
        val json = JSONObject()
        json.put("name", name)
        json.put("previewText", previewText)
        json.put("base64", Base64.encodeToString(rawNdefBytes, Base64.NO_WRAP))
        prefs.edit().putString(KEY_LAST, json.toString()).apply()
    }

    fun load(): SavedPayload? {
        val raw = prefs.getString(KEY_LAST, null) ?: return null
        val json = JSONObject(raw)
        val bytes = Base64.decode(json.getString("base64"), Base64.NO_WRAP)
        return SavedPayload(
            name = json.optString("name", "Payload Tersimpan"),
            previewText = json.optString("previewText", ""),
            rawNdefBytes = bytes
        )
    }

    companion object {
        private const val KEY_LAST = "last_payload"
    }
}

data class SavedPayload(
    val name: String,
    val previewText: String,
    val rawNdefBytes: ByteArray
)
