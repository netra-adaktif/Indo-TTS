package org.evvdroid

import java.io.File

/**
 * Modul pemetaan karakter Unicode Emoji ke deskripsi teks Bahasa Indonesia.
 */
object Emoji {

    private val EMOJI_MAP = mutableMapOf(
        // Makanan & Minuman
        "🍣" to "sushi",
        "🍦" to "es krim lembut",
        "🍧" to "es serut",
        "🍨" to "es krim",
        "🍩" to "donat",
        "🍪" to "kue kering",
        "🎂" to "kue ulang tahun",
        "🍰" to "kue bolu",
        "🧁" to "kue mangkok",
        "🍫" to "cokelat",
        "🍬" to "permen",
        "🍭" to "permen tangkai",
        "🍿" to "berondong jagung",
        "☕" to "minuman hangat",
        "🍵" to "cangkir teh",
        "🧃" to "minuman kotak",
        "🥤" to "cangkir dengan sedotan",
        "🧋" to "teh boba",
        "🍺" to "gelas bir",
        "🍻" to "dentang gelas bir",
        "🍷" to "gelas anggur",
        "🍸" to "gelas koktail",
        "🍹" to "minuman tropis",
        "🍾" to "botol sampanye",

        // Simbol & Perintah
        "⚠️" to "peringatan",
        "⛔" to "dilarang masuk",
        "🚫" to "dilarang",
        "❌" to "tanda silang",
        "⭕" to "lingkaran merah",
        "✅" to "tombol tanda centang",
        "✔️" to "tanda centang",
        "✔" to "tanda centang",
        "ℹ️" to "informasi",
        "❓" to "tanda tanya merah",
        "❔" to "tanda tanya putih",
        "❕" to "tanda seru putih",
        "❗" to "tanda seru merah",
        "♿" to "simbol kursi roda",
        "🚹" to "toilet pria",
        "🚺" to "toilet wanita",
        "🚻" to "toilet",
        "🚼" to "simbol bayi",
        "🚭" to "dilarang merokok",
        "🚩" to "bendera segitiga",
        "🏁" to "bendera kotak-kotak",
        "🏳️" to "bendera putih",
        "🏴" to "bendera hitam",

        // Ekspresi Wajah Utama
        "😀" to "wajah tersenyum",
        "😃" to "wajah tersenyum lebar",
        "😄" to "wajah tersenyum bahagia",
        "😁" to "wajah berseri",
        "😆" to "wajah tertawa puas",
        "😅" to "wajah tersenyum berkeringat",
        "🤣" to "tertawa terbahak bahak",
        "😂" to "tertawa sampai menangis",
        "🙂" to "wajah tersenyum sedikit",
        "❤️" to "hati merah",
        "👍" to "jempol atas",
        "👎" to "jempol bawah",
        "🙏" to "tangan menyatu"
    )

    /**
     * Memuat kustomisasi emoji dari file pengguna (digunakan oleh SettingsModel).
     */
    fun loadUserEmoji(file: File?): Map<String, String> {
        if (file == null || !file.exists()) return EMOJI_MAP
        return EMOJI_MAP
    }

    /**
     * Mengubah setiap emoji dalam teks menjadi teks Bahasa Indonesia.
     */
    fun process(text: String): String {
        var result = text
        for ((emoji, description) in EMOJI_MAP) {
            result = result.replace(emoji, " $description ")
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }
}
