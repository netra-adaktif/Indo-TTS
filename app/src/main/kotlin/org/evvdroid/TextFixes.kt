package org.evvdroid

/**
 * Modul untuk melakukan normalisasi teks dan pemrosesan singkatan
 * dalam Bahasa Indonesia sebelum dikirim ke engine TTS Eloquence.
 */
object TextFixes {

    // Regex untuk pembersihan format dasar Eloquence
    private val BEFORE_A_DIGIT = Regex("""([A-Za-z])(\d)""")
    private val OPENERS = """~#${'$'}%^*({|\[<""" + "\u2022"
    private val BEFORE_AN_OPENER = Regex("([A-Za-z]+)([" + Regex.escape(OPENERS) + "])")
    private val SPACE_BEFORE_A_MARK = Regex("""([A-Za-z]+|\d+|\W+)\s+([:.!;,?](?![A-Za-z]|\d))""")

    // Kamus pemetaan singkatan Bahasa Indonesia
    private val ABBREVIATIONS = mapOf(
        "yg" to "yang",
        "dgn" to "dengan",
        "krn" to "karena",
        "sbg" to "sebagai",
        "dll" to "dan lain-lain",
        "dsb" to "dan sebagainya",
        "dst" to "dan seterusnya",
        "ttg" to "tentang",
        "tgl" to "tanggal",
        "bln" to "bulan",
        "thn" to "tahun",
        "u/" to "untuk",
        "d/a" to "dengan alamat",
        "s/d" to "sampai dengan",
        "utk" to "untuk"
    )

    /**
     * Memproses teks input untuk merapikan format dan mengganti singkatan.
     */
    fun process(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // 1. Pisahkan huruf yang menempel langsung dengan angka (misal: mp3 -> mp 3)
        result = result.replace(BEFORE_A_DIGIT, "$1 $2")

        // 2. Berikan spasi sebelum simbol pembuka
        result = result.replace(BEFORE_AN_OPENER, "$1 $2")

        // 3. Hapus spasi abnormal sebelum tanda baca
        result = result.replace(SPACE_BEFORE_A_MARK, "$1$2")

        // 4. Ganti singkatan Bahasa Indonesia berdasarkan batas kata (\b)
        for ((abbr, full) in ABBREVIATIONS) {
            val regex = Regex("\\b${Regex.escape(abbr)}\\b", RegexOption.IGNORE_CASE)
            result = result.replace(regex, full)
        }

        // 5. Normalisasi spasi ganda menjadi satu spasi
        return result.replace(Regex("\\s+"), " ").trim()
    }
}
