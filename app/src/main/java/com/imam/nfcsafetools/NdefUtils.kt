package com.imam.nfcsafetools

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

object NdefUtils {

    fun readTagSummary(tag: Tag): String {
        val uid = tag.id.joinToString(":") { each -> "%02X".format(each) }
        val techs = tag.techList.joinToString(", ")
        return "UID: $uid\nTech: $techs"
    }

    fun readNdefMessage(tag: Tag): NdefMessage? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            ndef.cachedNdefMessage ?: ndef.ndefMessage
        } finally {
            safeClose(ndef)
        }
    }

    fun parsePlainText(message: NdefMessage?): String {
        if (message == null) return "(Tag tidak mengandung NDEF)"
        val lines = mutableListOf<String>()
        for ((index, record) in message.records.withIndex()) {
            val parsed = parseRecord(record)
            lines.add("Record ${index + 1}: $parsed")
        }
        return lines.joinToString("\n")
    }

    fun createTextMessage(text: String, locale: Locale = Locale.ENGLISH): NdefMessage {
        val langBytes = locale.language.toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        val record = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
        return NdefMessage(arrayOf(record))
    }

    @Throws(IOException::class, FormatException::class)
    fun writeMessage(tag: Tag, message: NdefMessage) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (!ndef.isWritable) {
                throw IOException("Tag tidak writable")
            }
            if (ndef.maxSize < message.toByteArray().size) {
                throw IOException("Kapasitas tag tidak cukup")
            }
            ndef.writeNdefMessage(message)
            safeClose(ndef)
            return
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            formatable.connect()
            formatable.format(message)
            safeClose(formatable)
            return
        }

        throw IOException("Tag tidak mendukung NDEF")
    }

    private fun parseRecord(record: NdefRecord): String {
        return if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            parseTextRecord(record)
        } else {
            "TNF=${record.tnf}, type=${record.type.toHexString()}, payload=${record.payload.toHexString()}"
        }
    }

    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return "(text record kosong)"
        val langLength = payload[0].toInt() and 0x3F
        if (payload.size <= langLength) return "(text record tidak valid)"
        return payload.copyOfRange(1 + langLength, payload.size).toString(Charsets.UTF_8)
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    private fun safeClose(closeable: Any?) {
        try {
            when (closeable) {
                is Ndef -> closeable.close()
                is NdefFormatable -> closeable.close()
            }
        } catch (_: Exception) {
        }
    }
}
