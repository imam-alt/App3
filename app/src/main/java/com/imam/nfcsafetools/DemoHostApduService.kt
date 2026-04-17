package com.imam.nfcsafetools

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class DemoHostApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        return when {
            isSelectAidApdu(commandApdu) -> STATUS_OK
            isReadDemoPayloadApdu(commandApdu) -> {
                val payload = HcePayloadHolder.payloadText.toByteArray(Charsets.UTF_8)
                payload + STATUS_OK
            }
            else -> STATUS_FAILED
        }
    }

    override fun onDeactivated(reason: Int) = Unit

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        val selectAid = hexToBytes("00A4040007F0394148148100")
        return apdu.contentEquals(selectAid)
    }

    private fun isReadDemoPayloadApdu(apdu: ByteArray): Boolean {
        val readCmd = hexToBytes("80CA000000")
        return apdu.contentEquals(readCmd)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        private val STATUS_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }
}

object HcePayloadHolder {
    @Volatile
    var payloadText: String = "Demo NFC payload milik sendiri"
}
