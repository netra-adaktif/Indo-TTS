#!/usr/bin/env python3
"""Mengubah daftar emoji bahasa Indonesia berbentuk Kotlin menjadi
app/src/main/assets/emoji_id.tsv, berkas yang dibaca aplikasi.

    python3 tools/buat_emoji_id.py daftar_emoji_kotlin.txt

Masukannya baris-baris berbentuk  "emoji" to "nama".  Keluarannya satu baris per
emoji, "emoji<TAB>nama". Saat diubah:
- yang bukan emoji dibuang (koma, titik dua, garis bawah, tanda hubung, tanda
  kurung, mata uang, simbol matematika). Tanda baca sudah ditangani TalkBack, dan
  kalau ikut, setiap koma di tulisan akan dibacakan "koma";
- penanda variasi tak terlihat (U+FE0F) dibuang dari emoji, supaya cocok dengan
  tulisan yang memakainya maupun yang tidak;
- nama dibuat enak dibacakan: angka ditulis dengan huruf, titik dua menjadi koma,
  tanda hubung menjadi spasi.
"""
import os
import re
import sys

KELUARAN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                        "app", "src", "main", "assets", "emoji_id.tsv")

# Emoji di luar blok astral (Unicode emoji-data, Emoji=Yes).
BMP_EMOJI = set()
for a, b in [
    (0xA9, 0xA9), (0xAE, 0xAE), (0x203C, 0x203C), (0x2049, 0x2049), (0x2122, 0x2122),
    (0x2139, 0x2139), (0x2194, 0x2199), (0x21A9, 0x21AA), (0x231A, 0x231B), (0x2328, 0x2328),
    (0x23CF, 0x23CF), (0x23E9, 0x23F3), (0x23F8, 0x23FA), (0x24C2, 0x24C2), (0x25AA, 0x25AB),
    (0x25B6, 0x25B6), (0x25C0, 0x25C0), (0x25FB, 0x25FE), (0x2600, 0x2604), (0x260E, 0x260E),
    (0x2611, 0x2611), (0x2614, 0x2615), (0x2618, 0x2618), (0x261D, 0x261D), (0x2620, 0x2620),
    (0x2622, 0x2623), (0x2626, 0x2626), (0x262A, 0x262A), (0x262E, 0x262F), (0x2638, 0x263A),
    (0x2640, 0x2640), (0x2642, 0x2642), (0x2648, 0x2653), (0x265F, 0x2660), (0x2663, 0x2663),
    (0x2665, 0x2666), (0x2668, 0x2668), (0x267B, 0x267B), (0x267E, 0x267F), (0x2692, 0x2697),
    (0x2699, 0x2699), (0x269B, 0x269C), (0x26A0, 0x26A1), (0x26A7, 0x26A7), (0x26AA, 0x26AB),
    (0x26B0, 0x26B1), (0x26BD, 0x26BE), (0x26C4, 0x26C5), (0x26C8, 0x26C8), (0x26CE, 0x26CF),
    (0x26D1, 0x26D1), (0x26D3, 0x26D4), (0x26E9, 0x26EA), (0x26F0, 0x26F5), (0x26F7, 0x26FA),
    (0x26FD, 0x26FD), (0x2702, 0x2702), (0x2705, 0x2705), (0x2708, 0x270D), (0x270F, 0x270F),
    (0x2712, 0x2712), (0x2714, 0x2714), (0x2716, 0x2716), (0x271D, 0x271D), (0x2721, 0x2721),
    (0x2728, 0x2728), (0x2733, 0x2734), (0x2744, 0x2744), (0x2747, 0x2747), (0x274C, 0x274C),
    (0x274E, 0x274E), (0x2753, 0x2755), (0x2757, 0x2757), (0x2763, 0x2764), (0x2795, 0x2797),
    (0x27A1, 0x27A1), (0x27B0, 0x27B0), (0x27BF, 0x27BF), (0x2934, 0x2935), (0x2B05, 0x2B07),
    (0x2B1B, 0x2B1C), (0x2B50, 0x2B50), (0x2B55, 0x2B55), (0x3030, 0x3030), (0x303D, 0x303D),
    (0x3297, 0x3297), (0x3299, 0x3299),
]:
    BMP_EMOJI.update(range(a, b + 1))

SATUAN = ["nol", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan",
          "sembilan", "sepuluh", "sebelas"]


def terbilang(n):
    if n < 12:
        return SATUAN[n]
    if n < 20:
        return terbilang(n - 10) + " belas"
    if n < 100:
        return SATUAN[n // 10] + " puluh" + ("" if n % 10 == 0 else " " + terbilang(n % 10))
    if n < 200:
        return "seratus" + ("" if n == 100 else " " + terbilang(n - 100))
    if n < 1000:
        return SATUAN[n // 100] + " ratus" + ("" if n % 100 == 0 else " " + terbilang(n % 100))
    if n < 2000:
        return "seribu" + ("" if n == 1000 else " " + terbilang(n - 1000))
    if n < 1000000:
        return terbilang(n // 1000) + " ribu" + ("" if n % 1000 == 0 else " " + terbilang(n % 1000))
    return str(n)


def kunci_emoji(kunci):
    isi = kunci.replace("\ufe0f", "")
    if not isi:
        return False
    if "\u20e3" in isi:  # tombol (keycap)
        return True
    return any(ord(c) >= 0x1F000 or ord(c) in BMP_EMOJI for c in isi)


def bersihkan(nama):
    nama = nama.strip().replace("&", " dan ")
    nama = re.sub(r"(?<=\d)[.:](?=\d)", " ", nama)
    nama = nama.replace(":", ",").replace(";", ",")
    nama = re.sub(r"(?<=[^\W\d_])-(?=[^\W\d_])", " ", nama)
    nama = re.sub(r"\d+", lambda m: terbilang(int(m.group())), nama)
    nama = re.sub(r"[\"“”()\[\]{}<>*_#@%^~|\\/]", " ", nama)
    nama = re.sub(r"\bkeycap\b", "tombol", nama, flags=re.I)
    nama = nama.replace("\u2019", "'").replace("!", "").replace(" - ", " ")
    nama = re.sub(r"\bKep\.", "Kepulauan", nama)
    nama = re.sub(r"\s+,", ",", nama)
    return re.sub(r"\s+", " ", nama).strip(" ,")


def lepas(s):
    """Membuka tulisan lepas ala Kotlin: \\" \\\\ \\$ \\uXXXX."""
    s = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), s)
    return re.sub(r"\\(.)", r"\1", s)


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        return 1
    teks = open(sys.argv[1], encoding="utf-8").read()
    pola = re.compile(r'^\s*"((?:\\.|[^"\\])*)"\s+to\s+"((?:\\.|[^"\\])*)"\s*,?\s*$', re.M)
    semua, dibuang = {}, 0
    for k, v in pola.findall(teks):
        k, v = lepas(k), lepas(v)
        if not kunci_emoji(k):
            dibuang += 1
            continue
        k = k.replace("\ufe0f", "")
        nama = bersihkan(v)
        if nama and k not in semua:
            semua[k] = nama
    os.makedirs(os.path.dirname(KELUARAN), exist_ok=True)
    with open(KELUARAN, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Nama emoji bahasa Indonesia. Dibuat oleh tools/buat_emoji_id.py.\n")
        f.write("\n".join(f"{k}\t{v}" for k, v in sorted(semua.items())) + "\n")
    print(f"{len(semua)} emoji ditulis, {dibuang} entri bukan emoji dibuang")
    return 0


if __name__ == "__main__":
    sys.exit(main())
