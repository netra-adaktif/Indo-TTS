package org.evvdroid

/**
 * Modul untuk mengonversi angka digit menjadi kata-kata terbilang
 * dalam Bahasa Indonesia.
 */
object Numbers {

    // Konstanta Mode Pembacaan Angka untuk Settings.kt
    const val MODE_DEFAULT = 0
    const val MODE_DIGITS = 1
    const val MODE_WORDS = 2

    private val ONES = arrayOf(
        "nol", "satu", "dua", "tiga", "empat", "lima",
        "enam", "tujuh", "delapan", "sembilan", "sepuluh", "sebelas"
    )

    /**
     * Mengonversi angka bertipe Long menjadi kata terbilang Bahasa Indonesia.
     */
    fun toWords(n: Long): String {
        return when {
            n < 0 -> "minus " + toWords(-n)
            n < 12 -> ONES[n.toInt()]
            n < 20 -> toWords(n % 10) + " belas"
            n < 100 -> {
                val ten = toWords(n / 10) + " puluh"
                val rem = n % 10
                if (rem > 0) "$ten ${ONES[rem.toInt()]}" else ten
            }
            n < 200 -> {
                val rem = n - 100
                if (rem > 0) "seratus " + toWords(rem) else "seratus"
            }
            n < 1000 -> {
                val hundred = toWords(n / 100) + " ratus"
                val rem = n % 100
                if (rem > 0) "$hundred " + toWords(rem) else hundred
            }
            n < 2000 -> {
                val rem = n - 1000
                if (rem > 0) "seribu " + toWords(rem) else "seribu"
            }
            n < 1000000 -> {
                val thousand = toWords(n / 1000) + " ribu"
                val rem = n % 1000
                if (rem > 0) "$thousand " + toWords(rem) else thousand
            }
            n < 1000000000 -> {
                val million = toWords(n / 1000000) + " juta"
                val rem = n % 1000000
                if (rem > 0) "$million " + toWords(rem) else million
            }
            n < 1000000000000L -> {
                val billion = toWords(n / 1000000000L) + " miliar"
                val rem = n % 1000000000L
                if (rem > 0) "$billion " + toWords(rem) else billion
            }
            else -> numToStrDigits(n.toString())
        }.trim().replace(Regex("\\s+"), " ")
    }

    private fun numToStrDigits(str: String): String =
        str.map { ONES.getOrElse(it - '0') { it.toString() } }.joinToString(" ")

    /**
     * Mencari seluruh digit angka di dalam teks dan mengonversinya ke kata-kata.
     */
    fun processNumbersInText(text: String): String {
        val regex = Regex("\\d+")
        return regex.replace(text) { matchResult ->
            val num = matchResult.value.toLongOrNull()
            if (num != null) toWords(num) else matchResult.value
        }
    }
}
