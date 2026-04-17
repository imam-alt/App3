package com.imam.nfcsafetools

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.imam.nfcsafetools.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private var lastSeenTag: Tag? = null
    private var lastReadMessage: NdefMessage? = null
    private lateinit var store: SavedPayloadStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = SavedPayloadStore(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setupButtons()
        handleIntent(intent)
        updateStatusForDevice()
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
        updateStatusForDevice()
    }

    override fun onPause() {
        disableForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupButtons() {
        binding.btnSavePayload.setOnClickListener {
            val msg = lastReadMessage
            if (msg == null) {
                toast("Belum ada payload NDEF yang terbaca")
                return@setOnClickListener
            }
            val name = binding.etName.text?.toString()?.trim().orEmpty().ifBlank { "Payload Tersimpan" }
            val preview = NdefUtils.parsePlainText(msg)
            store.save(name, msg.toByteArray(), preview)
            toast("Payload tersimpan")
        }

        binding.btnLoadPayload.setOnClickListener {
            val saved = store.load()
            if (saved == null) {
                toast("Belum ada payload tersimpan")
                return@setOnClickListener
            }
            binding.etName.setText(saved.name)
            binding.etPayload.setText(saved.previewText)
            binding.tvStatus.text = "Status: payload tersimpan dimuat."
        }

        binding.btnWriteText.setOnClickListener {
            val tag = lastSeenTag
            if (tag == null) {
                toast("Tempelkan dulu tag tujuan ke ponsel")
                return@setOnClickListener
            }
            val text = binding.etPayload.text?.toString().orEmpty()
            if (text.isBlank()) {
                toast("Isi teks dulu")
                return@setOnClickListener
            }
            runCatching {
                val message = NdefUtils.createTextMessage(text)
                NdefUtils.writeMessage(tag, message)
            }.onSuccess {
                binding.tvStatus.text = "Status: teks berhasil ditulis ke tag."
                toast("Berhasil menulis teks NDEF")
            }.onFailure {
                binding.tvStatus.text = "Status: gagal menulis tag: ${it.message}"
                toast("Gagal: ${it.message}")
            }
        }

        binding.btnWriteSaved.setOnClickListener {
            val tag = lastSeenTag
            if (tag == null) {
                toast("Tempelkan dulu tag tujuan ke ponsel")
                return@setOnClickListener
            }
            val saved = store.load()
            if (saved == null) {
                toast("Belum ada payload tersimpan")
                return@setOnClickListener
            }
            runCatching {
                val message = NdefMessage(saved.rawNdefBytes)
                NdefUtils.writeMessage(tag, message)
            }.onSuccess {
                binding.tvStatus.text = "Status: payload tersimpan berhasil ditulis ke tag milik sendiri."
                toast("Payload tersimpan berhasil ditulis")
            }.onFailure {
                binding.tvStatus.text = "Status: gagal menulis payload tersimpan: ${it.message}"
                toast("Gagal: ${it.message}")
            }
        }

        binding.btnPrepareHce.setOnClickListener {
            val payload = binding.etPayload.text?.toString().orEmpty().ifBlank { "Demo NFC payload milik sendiri" }
            HcePayloadHolder.payloadText = payload
            binding.tvStatus.text = "Status: payload demo HCE disiapkan. Gunakan reader custom yang memilih AID F0394148148100 lalu APDU 80CA000000."
            toast("Payload demo HCE disiapkan")
        }

        binding.tvHceInfo.setOnLongClickListener {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            true
        }
    }

    private fun handleIntent(intent: Intent?) {
        val tag = extractTag(intent) ?: return
        lastSeenTag = tag
        lastReadMessage = NdefUtils.readNdefMessage(tag)

        val summary = buildString {
            appendLine(NdefUtils.readTagSummary(tag))
            appendLine()
            appendLine("NDEF:")
            append(NdefUtils.parsePlainText(lastReadMessage))
        }

        binding.tvTagInfo.text = summary
        if (lastReadMessage != null) {
            binding.etPayload.setText(NdefUtils.parsePlainText(lastReadMessage))
        }
        binding.tvStatus.text = "Status: tag terbaca. Anda bisa simpan payload atau tulis ke tag milik sendiri."
    }

    private fun extractTag(intent: Intent?): Tag? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    private fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        adapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    private fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun updateStatusForDevice() {
        val adapter = nfcAdapter
        binding.tvStatus.text = when {
            adapter == null -> "Status: perangkat ini tidak memiliki NFC."
            !adapter.isEnabled -> "Status: NFC mati. Tekan lama info HCE untuk buka pengaturan NFC."
            else -> "Status: siap. Tempelkan tag NFC ke ponsel."
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
