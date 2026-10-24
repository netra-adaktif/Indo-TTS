package id.eloquenceindo

/**
 * Long text, cut into pieces the engine can be stopped between.
 *
 * The engine cannot abandon an utterance. That is settled rather than open --
 * openevv's own documents say six ways of adding an abandonment point were
 * tried and all fault -- so asking for silence waits out whatever is in flight.
 * For one line of a list that is a thirtieth of a second and nobody hears it.
 * For a paragraph it is not, and a reader swiping every couple of hundred
 * milliseconds has everything that lands inside the wait dropped: speech goes
 * quiet for several items and then catches up on the last one.
 *
 * Handing the text over in pieces makes the wait one piece rather than one
 * message. Where the piece ends is the whole of the problem, because ending
 * one is not free: the engine pauses at the end of an utterance as it would at
 * a full stop, so a boundary it would not have made itself is an audible gap.
 *
 * The rule here is openevv's, ported from its NVDA driver, and the numbers in
 * it are measured rather than chosen. A sentence end costs nothing -- one
 * message came to 252,010 samples whole and the same 252,010 in six pieces --
 * and anywhere else costs about four tenths of a second. So a full stop has to
 * argue that it is one: a word carrying a dot inside it is an abbreviation
 * ("e.g.", "U.S.") and a short word starting with a capital is an initial or a
 * title ("J.", "Mr."), and cutting at those measured 1.48 s and 0.70 s longer
 * than the same sentences whole. Only past [PIECE_CAP] characters does a piece
 * end at plain whitespace, which bounds the case that offers no sentence end
 * at all.
 */
object TextPieces {

	/** How far a piece runs before it ends at whitespace for want of a
	 *  sentence end. */
	const val PIECE_CAP = 500

	/** Closing marks that may sit after the punctuation that ends a sentence. */
	private const val CLOSERS = "\"')]}»”’"

	private const val ENDERS = "?!…。！？"

	/** Opening marks that may sit before the first letter of a word. */
	private const val OPENERS = "\"'([{«“‘"

	/** Abbreviations that go before a name or a number, so a dot after one of
	 *  them never ends a sentence. */
	private val TITLES = setOf(
		"dr", "drs", "dra", "ir", "prof", "hj", "h", "kh", "bpk", "bp", "sdr", "sdri",
		"jl", "jln", "gg", "rt", "rw", "kab", "kec", "kel", "no", "hlm", "tgl", "pt",
		"cv", "ud", "rp", "yg", "dg", "dgn", "spt", "sbb", "mr", "mrs", "ms", "st",
		"vs", "sr", "jr", "mt", "gn"
	)

	/** Abbreviations that close a list and so may end a sentence, which they do
	 *  when a capital letter follows. */
	private val TRAILING = setOf("dll", "dsb", "dst", "dkk", "dsj", "tsb", "etc")

	/** Whether a word ends a sentence rather than being a dotted abbreviation. */
	fun endsSentence(word: String): Boolean {
		val tail = word.trimEnd { it in CLOSERS }
		if (tail.isEmpty()) return false
		if (tail.last() in ENDERS) return true
		if (!tail.endsWith('.') || tail.length < 2 || tail[tail.length - 2].isDigit()) return false
		val without = tail.dropLast(1)
		if (without.contains('.')) return false
		return !(without.length <= 3 && without.first().isUpperCase())
	}

	/**
	 * As [endsSentence], but told the word that follows, which is what settles
	 * it. Indonesian ends sentences in ways the rule above takes for an
	 * abbreviation: with a year ("...tahun 2026."), with an acronym ("...oleh
	 * DPR."), with a short word ("...terima kasih, Pak."). And it writes
abbreviations in lower case ("dll.", "yg.") that the rule above takes
	 * for the end of a sentence. A dot ends a sentence when what follows starts
	 * like one, unless the word is an abbreviation that is always followed by
	 * something, such as a title.
	 */
	fun endsSentenceBefore(word: String, next: String?): Boolean {
		val tail = word.trimEnd { it in CLOSERS }
		if (tail.isEmpty()) return false
		if (tail.last() in ENDERS) return true
		if (tail.length < 2 || !tail.endsWith('.')) return false
		val core = tail.dropLast(1)
		val lower = core.lowercase()
		if (lower in TITLES) return false
		if (lower in TRAILING) return startsLikeASentence(next)
		if (core.length == 1 && core[0].isLetter()) return false
		if (core.contains('.')) {
			if (core.all { it.isDigit() || it == '.' || it == ',' }) return startsLikeASentence(next)
			return next == null
		}
		return startsLikeASentence(next)
	}

	private fun startsLikeASentence(next: String?): Boolean {
		if (next == null) return true
		val first = next.trimStart { it in OPENERS }.firstOrNull() ?: return true
		return first.isUpperCase() || first.isDigit()
	}

	/**
	 * [text] as the pieces to hand over one at a time. Every character of the
	 * original is in exactly one piece and in the same order, so the speech is
	 * the same speech.
	 */
	fun split(text: String): List<String> {
		if (text.length <= PIECE_CAP && !text.any { it.isWhitespace() }) return listOf(text)
		val pieces = ArrayList<String>()
		val piece = StringBuilder()
		var gathered = 0
		var sentence = false
		val parts = runs(text)
		for ((index, part) in parts.withIndex()) {
			piece.append(part)
			gathered += part.length
			if (part.isNotBlank()) {
				sentence = endsSentenceBefore(part, parts.getOrNull(index + 2))
				continue
			}
			// A blank line ends a paragraph, and a pause there is wanted.
			val paragraph = part.count { it == '\n' } >= 2 && piece.isNotBlank()
			// A piece ends after the space that follows the word, so no word is
			// split and nothing is left leaning on the next piece.
			if (sentence || paragraph || gathered >= PIECE_CAP) {
				pieces.add(piece.toString())
				piece.setLength(0)
				gathered = 0
			}
			sentence = false
		}
		if (piece.isNotBlank()) {
			pieces.add(piece.toString())
		} else if (piece.isNotEmpty() && pieces.isNotEmpty()) {
			// Trailing space belongs to the piece in front of it rather than to
			// a piece of its own, which would be an utterance saying nothing.
			pieces[pieces.lastIndex] = pieces.last() + piece
		}
		return if (pieces.isEmpty()) listOf(text) else pieces
	}

	/** [text] as alternating runs of whitespace and everything else. */
	private fun runs(text: String): List<String> {
		val out = ArrayList<String>()
		var at = 0
		while (at < text.length) {
			val from = at
			val space = text[at].isWhitespace()
			while (at < text.length && text[at].isWhitespace() == space) at++
			out.add(text.substring(from, at))
		}
		return out
	}
}
