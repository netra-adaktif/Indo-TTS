package id.eloquenceindo

import android.content.res.AssetManager
import android.util.Log
import java.io.File

/**
 * Intelligent Emoji reading and dictionary for Eloquence TTS.
 *
 * This project keeps English-only engine configuration, but the emoji expansion is
 * translated to Indonesian so the spoken output feels natural on Android devices.
 */
object Emoji {

	private const val TAG = "evvdroid"

	@Volatile
	private var userEmojiMap: Map<String, String> = emptyMap()

	fun loadUserEmoji(file: File) {
		if (!file.exists() || file.length() == 0L) {
			userEmojiMap = emptyMap()
			return
		}
		val map = mutableMapOf<String, String>()
		try {
			file.forEachLine(Charsets.UTF_8) { rawLine ->
				val line = rawLine.trim()
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEachLine
				val split = line.indexOf('\t')
				if (split > 0) {
					val key = line.substring(0, split).trim()
					val rest = line.substring(split + 1).trim()
					val tab2 = rest.indexOf('\t')
					val say = (if (tab2 >= 0) rest.substring(0, tab2) else rest).trim()
					if (key.isNotEmpty() && say.isNotEmpty()) {
						map[key] = say
						val cleanKey = key.replace("\uFE0F", "").replace("\uFE0E", "")
						if (cleanKey != key && cleanKey.isNotEmpty()) {
							map[cleanKey] = say
						}
					}
				}
			}
			userEmojiMap = map
			Log.i(TAG, "Loaded ${map.size} custom emoji mappings from ${file.name}")
		} catch (e: Exception) {
			Log.e(TAG, "Failed to load user emoji dictionary", e)
		}
	}

	/** Where the Indonesian names of the rest of the emoji are kept: one line
	 *  each, the emoji and its name with a tab between. See tools/buat_emoji_id.py. */
	private const val CLDR_ASSET = "emoji_id.tsv"

	@Volatile
	private var assetManager: AssetManager? = null

	@Volatile
	private var cldrNames: Map<String, String>? = null

	@Volatile
	private var cldrLongest = 1

	/** Lets the names be read from the app's assets. Called when the speech
	 *  service starts. */
	fun install(assets: AssetManager) {
		assetManager = assets
	}

	private fun cldr(): Map<String, String> {
		cldrNames?.let { return it }
		val am = assetManager ?: return emptyMap()
		val loaded = HashMap<String, String>(8192)
		var longest = 1
		try {
			am.open(CLDR_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
				for (raw in lines) {
					if (raw.isEmpty() || raw[0] == '#') continue
					val tab = raw.indexOf('\t')
					if (tab <= 0) continue
					val key = raw.substring(0, tab)
					val say = raw.substring(tab + 1).trim()
					if (say.isEmpty()) continue
					loaded[key] = say
					if (key.length > longest) longest = key.length
				}
			}
		} catch (e: Exception) {
			Log.w(TAG, "The Indonesian emoji names could not be read: $e")
		}
		cldrLongest = longest
		cldrNames = loaded
		return loaded
	}

	fun apply(text: String): String {
		if (text.isEmpty()) return text
		val userMap = userEmojiMap
		var out = text

		if (userMap.isNotEmpty()) {
			val sortedUserKeys = userMap.keys.sortedByDescending { it.length }
			for (key in sortedUserKeys) {
				if (out.contains(key)) {
					val say = userMap[key] ?: continue
					out = out.replace(key, " $say ")
				}
			}
		}

		if (!hasEmojiCandidate(out)) {
			return collapseSpaces(out)
		}

		for (entry in BUILT_IN_EMOJIS_SORTED) {
			if (out.contains(entry.key)) {
				out = out.replace(entry.key, " ${entry.value} ")
			}
		}

		// What the built in list did not name is named from the full Indonesian
		// list: skin tones, flags, families and the rest.
		out = nameTheRest(out)

		return collapseSpaces(out)
	}

	/** Reads [text] once, longest match first, so that a flag or a family is
	 *  named as the one thing it is rather than as the pieces it is made of. */
	private fun nameTheRest(text: String): String {
		val names = cldr()
		if (names.isEmpty()) return text
		val clean = text.replace("\uFE0F", "").replace("\uFE0E", "")
		val out = StringBuilder(clean.length + 32)
		var i = 0
		while (i < clean.length) {
			if (!startsEmoji(clean, i)) {
				out.append(clean[i])
				i++
				continue
			}
			var len = minOf(cldrLongest, clean.length - i)
			var say: String? = null
			while (len > 0) {
				say = names[clean.substring(i, i + len)]
				if (say != null) break
				len--
			}
			if (say != null) {
				out.append(' ').append(say).append(' ')
				i += len
			} else {
				out.append(clean[i])
				i++
			}
		}
		// A joiner left between pieces that were named one by one says nothing.
		return out.toString().replace("\u200D", "")
	}

	private fun startsEmoji(text: String, i: Int): Boolean {
		val c = text[i]
		if (c.isHighSurrogate() || isBmpEmoji(c.code)) return true
		val keycapBase = c in '0'..'9' || c == '#' || c == '*'
		return keycapBase && i + 1 < text.length && text[i + 1] == '\u20E3'
	}

	private fun isBmpEmoji(code: Int): Boolean =
		code == 0x00A9 || code == 0x00AE || code == 0x203C || code == 0x2049 ||
			code == 0x2122 || code == 0x2139 || code == 0x24C2 ||
			code == 0x3030 || code == 0x303D || code == 0x3297 || code == 0x3299 ||
			code in 0x2190..0x21FF || code in 0x2300..0x23FF || code in 0x25A0..0x25FF ||
			code in 0x2600..0x27BF || code in 0x2900..0x297F || code in 0x2B00..0x2BFF

	private fun hasEmojiCandidate(text: String): Boolean {
		for (i in 0 until text.length) {
			val c = text[i]
			if (c.isHighSurrogate() || isBmpEmoji(c.code) || c == '\u20E3') {
				return true
			}
		}
		return false
	}

	private fun collapseSpaces(text: String): String =
		text.replace(Regex("""[ \t]+"""), " ")

	private val BUILT_IN_EMOJIS_SORTED: List<Map.Entry<String, String>> by lazy {
		BUILT_IN_EMOJIS.entries.sortedByDescending { it.key.length }
	}

	private val BUILT_IN_EMOJIS: Map<String, String> = mapOf(
		"😀" to "wajah tersenyum lebar",
		"😃" to "wajah tersenyum dengan mata lebar",
		"😄" to "wajah tersenyum dengan mata bersinar",
		"😁" to "wajah penuh semangat",
		"😆" to "wajah tertawa lebar",
		"😅" to "wajah tersenyum sambil berkeringat",
		"🤣" to "tertawa terbahak bahak",
		"😂" to "wajah menangis tertawa",
		"🙂" to "wajah tersenyum sedikit",
		"🙃" to "wajah terbalik",
		"😉" to "wajah mengedip",
		"😊" to "wajah tersenyum lembut",
		"😇" to "wajah berseri dengan halo",
		"🥰" to "wajah tersenyum dengan hati",
		"😍" to "mata berbentuk hati",
		"🤩" to "terpesona",
		"😘" to "wajah mengecup",
		"😗" to "wajah mencium",
		"😚" to "wajah mencium dengan mata tertutup",
		"😙" to "wajah mencium dengan mata bersinar",
		"😋" to "wajah menikmati makanan",
		"😛" to "wajah dengan lidah",
		"😜" to "wajah mengedip dengan lidah",
		"🤪" to "wajah konyol",
		"😝" to "wajah mencibir dengan lidah",
		"🤗" to "wajah tersenyum sambil merentangkan tangan",
		"🤭" to "wajah menutup mulut",
		"🤔" to "wajah berpikir",
		"🤐" to "wajah dengan mulut zip",
		"😐" to "wajah netral",
		"😑" to "wajah tanpa ekspresi",
		"😶" to "wajah tanpa mulut",
		"😏" to "wajah menyeringai",
		"😒" to "wajah tidak senang",
		"🙄" to "wajah dengan mata berputar",
		"😬" to "wajah canggung",
		"🤥" to "wajah berbohong",
		"😌" to "wajah lega",
		"😔" to "wajah sedih",
		"😴" to "wajah tidur",
		"😷" to "wajah dengan masker medis",
		"🤒" to "wajah dengan termometer",
		"🤕" to "wajah dengan perban",
		"🤢" to "wajah mual",
		"🤮" to "wajah muntah",
		"🤧" to "wajah bersin",
		"🥵" to "wajah panas",
		"🥶" to "wajah dingin",
		"🥴" to "wajah pusing",
		"😵" to "wajah pusing",
		"🤯" to "kepala meledak",
		"😎" to "wajah tersenyum dengan kacamata hitam",
		"🤓" to "wajah nerd",
		"😕" to "wajah bingung",
		"😟" to "wajah khawatir",
		"🙁" to "wajah cemberut sedikit",
		"☹️" to "wajah cemberut",
		"😮" to "wajah dengan mulut terbuka",
		"😯" to "wajah terdiam",
		"😲" to "wajah terkejut",
		"😳" to "wajah memerah",
		"🥺" to "wajah memelas",
		"😦" to "wajah cemberut dengan mulut terbuka",
		"😧" to "wajah tertekan",
		"😨" to "wajah takut",
		"😰" to "wajah cemas dengan keringat",
		"😥" to "wajah sedih tapi lega",
		"😢" to "wajah menangis",
		"😭" to "wajah menangis keras",
		"😱" to "wajah berteriak ketakutan",
		"😖" to "wajah bingung dan frustrasi",
		"😞" to "wajah kecewa",
		"😓" to "wajah sedih berkeringat",
		"😩" to "wajah lelah",
		"😫" to "wajah capek",
		"😡" to "wajah marah",
		"😠" to "wajah kesal",
		"🤬" to "wajah dengan simbol di mulut",
		"💀" to "tengkorak",
		"☠️" to "tengkorak dan tulang bersilang",
		"💩" to "tinja",
		"🤡" to "wajah badut",
		"👻" to "hantu",
		"👽" to "alien",
		"🤖" to "robot",
		"👋" to "tangan melambai",
		"✋" to "tangan terangkat",
		"👌" to "tangan oke",
		"👍" to "jempol ke atas",
		"👎" to "jempol ke bawah",
		"✊" to "tinju terangkat",
		"👊" to "tinju datang",
		"👏" to "tepuk tangan",
		"🙌" to "mengangkat tangan",
		"🙏" to "tangan bersimpuh",
		"💪" to "otot lengan",
		"❤️" to "hati merah",
		"🩷" to "hati pink",
		"💔" to "hati yang patah",
		"💕" to "dua hati",
		"💖" to "hati yang berkilau",
		"💗" to "hati yang tumbuh",
		"💘" to "hati dengan panah",
		"💝" to "hati dengan pita",
		"💋" to "bekas ciuman",
		"💯" to "seratus poin",
		"🔥" to "api",
		"✨" to "bintang berkilau",
		"⭐" to "bintang",
		"🎉" to "poppers pesta",
		"🎊" to "bola konfeti",
		"🎁" to "hadiah dibungkus",
		"🏆" to "piala",
		"⚽" to "bola sepak",
		"🏀" to "bola basket",
		"⚾" to "bola bisbol",
		"🎾" to "tenis",
		"📱" to "telepon genggam",
		"💻" to "laptop",
		"📷" to "kamera",
		"📺" to "televisi",
		"🔊" to "speaker volume tinggi",
		"🔉" to "speaker volume sedang",
		"🔈" to "speaker volume rendah",
		"🔇" to "speaker senyap",
		"📢" to "speaker keras",
		"🎵" to "nada musik",
		"🎶" to "dua nada musik",
		"🎤" to "mikrofon",
		"🚗" to "mobil",
		"🚕" to "taksi",
		"🚌" to "bus",
		"✈️" to "pesawat",
		"🚀" to "roket",
		"☀️" to "matahari",
		"☁️" to "awan",
		"🌧️" to "awan dengan hujan",
		"❄️" to "salju",
		"⚡" to "tegangan tinggi",
		"🌈" to "pelangi",
		"🌊" to "ombak",
		"✅" to "tanda centang",
		"❌" to "tanda silang",
		"⚠️" to "perhatian",
		"❗" to "tanda seru merah",
		"❓" to "tanda tanya merah",
		"🐶" to "wajah anjing",
		"🐱" to "wajah kucing",
		"🐭" to "wajah tikus",
		"🐹" to "hamster",
		"🐰" to "wajah kelinci",
		"🦊" to "rubah",
		"🐻" to "beruang",
		"🐼" to "panda",
		"🐨" to "koala",
		"🐯" to "wajah harimau",
		"🦁" to "singa",
		"🐮" to "wajah sapi",
		"🐷" to "wajah babi",
		"🐸" to "katak",
		"🐵" to "wajah monyet",
		"🐔" to "ayam",
		"🐧" to "penguin",
		"🐦" to "burung",
		"🦆" to "bebek",
		"🦅" to "elang",
		"🦉" to "burung hantu",
		"🐺" to "serigala",
		"🐴" to "wajah kuda",
		"🦄" to "unikorn",
		"🐝" to "lebah",
		"🐛" to "ulat",
		"🦋" to "kupu kupu",
		"🌼" to "bunga",
		"🌻" to "bunga matahari",
		"🌷" to "bunga tulip",
		"🌱" to "bibit",
		"🌳" to "pohon",
		"🌴" to "pohon palem",
		"🍉" to "semangka",
		"🍇" to "anggur",
		"🍓" to "stroberi",
		"🍒" to "ceri",
		"🍑" to "persik",
		"🍍" to "nanas",
		"🥥" to "kelapa",
		"🍅" to "tomat",
		"🥑" to "alpukat",
		"🌽" to "jagung",
		"🥕" to "wortel",
		"🍞" to "roti",
		"🥐" to "croissant",
		"🍔" to "hamburger",
		"🍟" to "kentang goreng",
		"🍕" to "pizza",
		"🌮" to "taco",
		"🍜" to "mangkuk panas",
		"🍣" to "sushi",
		"🍦" to "es krim",
		"🍩" to "donat",
		"🍰" to "kue",
		"☕" to "minuman panas",
		"🍵" to "cangkir teh",
		"🍺" to "gelas bir",
		"🍷" to "gelas anggur",
		"🍹" to "minuman tropis"
	)
}
