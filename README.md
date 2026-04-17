# NfcSafeTools for Android

Aplikasi Android native untuk penggunaan **NFC yang legal dan aman** pada tag milik sendiri.

## Yang didukung
- Baca UID, tech list, dan informasi umum tag NFC
- Baca **NDEF** dari tag yang kompatibel
- Simpan payload NDEF terakhir ke memori aplikasi
- Tulis **teks NDEF** ke tag kosong / writable milik sendiri
- Salin ulang **payload NDEF yang tersimpan** ke tag lain milik sendiri
- Mode **Host Card Emulation (HCE) demo** untuk payload custom milik sendiri

## Yang sengaja tidak didukung
- Dump sektor/raw memory untuk cloning kartu akses
- Membaca atau menulis kunci rahasia / sektor terproteksi
- Menyalin kartu pembayaran, akses, absensi, hotel, transport, atau kartu pihak ketiga
- Menulis ulang UID / serial kartu

## Struktur utama
- `MainActivity.kt` -> UI, pembacaan tag, penulisan tag, simpan/muat payload
- `NdefUtils.kt` -> util baca/tulis NDEF
- `SavedPayloadStore.kt` -> penyimpanan lokal payload NDEF terakhir
- `DemoHostApduService.kt` -> HCE demo **custom** untuk pengujian milik sendiri
- `res/xml/apduservice.xml` -> deklarasi layanan HCE

## Catatan repo
Folder ini adalah proyek Android siap dibuka di Android Studio. Gradle Wrapper belum disertakan dari lingkungan kerja ini, jadi setelah repo dibuat di GitHub, buka di Android Studio lalu izinkan IDE membuat/menyinkronkan wrapper jika diperlukan.

## Cara pakai singkat
1. Instal dan buka aplikasi pada ponsel Android yang mendukung NFC.
2. Tap tag NFC milik sendiri ke ponsel.
3. Lihat info tag dan payload NDEF yang terbaca.
4. Tekan **Simpan Payload** untuk menyimpan payload terakhir.
5. Tekan **Muat Payload** untuk memuat payload tersimpan.
6. Tekan **Tulis Payload Tersimpan ke Tag** untuk menyalin payload NDEF ke tag milik sendiri.
7. Atau isi teks baru lalu tekan **Tulis Teks ke Tag**.

## Catatan HCE
Mode HCE pada proyek ini hanya meniru **aplikasi APDU custom** dengan AID milik sendiri untuk pengujian integrasi. Ini **bukan** alat untuk meniru kartu akses atau kartu pihak ketiga.

## Build di Android Studio
1. Buat repo GitHub baru, misalnya `NfcSafeTools`.
2. Upload seluruh isi folder ini ke repo tersebut.
3. Buka proyek di Android Studio.
4. Biarkan Gradle sync.
5. Jalankan ke device Android.

## Catatan teknis
- Banyak tag tidak mendukung penulisan.
- Beberapa tag tidak berisi NDEF; aplikasi akan tetap menampilkan UID dan tech list.
- HCE tidak tersedia di semua perangkat.
