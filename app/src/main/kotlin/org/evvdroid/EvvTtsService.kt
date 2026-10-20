package org.evvdroid

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * The engine Android talks to.
 *
 * onSynthesizeText runs on the framework's own synthesis thread and is the
 * only place an utterance is driven; onStop arrives from another thread and
 * does nothing but set the engine's abort flag, which is what unblocks the
 * read below.
 */
class EvvTtsService : TextToSpeechService() {

	@Volatile
	private var engine: EvvEngine? = null
	private var settings: Settings? = null

	@Volatile
	private var stopped = false

	private var appliedRevision = -1

	private data class LoadedDictState(val path: String, val lastModified: Long, val length: Long)
	private var loadedDictStates: Map<Int, LoadedDictState> = emptyMap()
	private var lastEmojiModified: Long = -1L
	private var lastEmojiLength: Long = -1L
	@Volatile
	private var activeCaseSensitiveEntries: List<Dictionaries.CachedCaseSensitiveEntry> = emptyList()

	override fun onCreate() {
		settings = Settings(this)
		super.onCreate()
	}

	override fun onDestroy() {
		super.onDestroy()
		engine?.close()
		engine = null
	}

	// ---- languages -------------------------------------------------------

	private fun isUsEnglishLanguage(language: Int): Boolean {
		val loc = Eci.localeOf(language) ?: return false
		return loc.first.equals("eng", ignoreCase = true) && loc.second.equals("USA", ignoreCase = true)
	}

	override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
		val found = Languages.match(lang, country) ?: return TextToSpeech.LANG_NOT_SUPPORTED
		return if (isUsEnglishLanguage(found.first)) found.second else TextToSpeech.LANG_NOT_SUPPORTED
	}

	override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
		val found = Languages.match(lang, country) ?: return TextToSpeech.LANG_NOT_SUPPORTED
		if (!isUsEnglishLanguage(found.first)) return TextToSpeech.LANG_NOT_SUPPORTED
		ensureEngine(found.first) ?: return TextToSpeech.LANG_NOT_SUPPORTED
		return found.second
	}

	override fun onGetLanguage(): Array<String> {
		val language = engine?.language ?: EvvEngine.available.firstOrNull() ?: return arrayOf("eng", "USA", "")
		val loc = Eci.localeOf(language) ?: return arrayOf("eng", "USA", "")
		return arrayOf(loc.first, loc.second, loc.third)
	}

	// ---- voices ----------------------------------------------------------

	private fun voiceName(language: Int, preset: Int): String {
		val loc = Eci.localeOf(language) ?: return "evv-$preset"
		return "${loc.first}-${loc.second}-${Eci.PRESET_NAMES[preset]}"
	}

	override fun onGetVoices(): MutableList<Voice> {
		val out = ArrayList<Voice>()
		for (language in EvvEngine.available) {
			if (!isUsEnglishLanguage(language)) continue
			val locale = Locale("en", "US")
			out.add(
				Voice(
					voiceName(language, 0),
					locale,
					Voice.QUALITY_NORMAL,
					Voice.LATENCY_VERY_LOW,
					false,
					emptySet()
				)
			)
		}
		return out
	}

	private fun findVoice(name: String?): Pair<Int, Int>? {
		if (name.isNullOrEmpty()) return null
		for (language in EvvEngine.available) {
			if (isUsEnglishLanguage(language) && voiceName(language, 0) == name) return language to 0
		}
		return null
	}

	override fun onIsValidVoiceName(name: String?): Int =
		if (findVoice(name) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR

	override fun onLoadVoice(name: String?): Int {
		val found = findVoice(name) ?: return TextToSpeech.ERROR
		ensureEngine(found.first) ?: return TextToSpeech.ERROR
		return TextToSpeech.SUCCESS
	}

	override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
		val found = Languages.match(lang, country) ?: return null
		return if (isUsEnglishLanguage(found.first)) voiceName(found.first, 0) else null
	}

	// ---- speaking --------------------------------------------------------

	private fun ensureEngine(language: Int): EvvEngine? {
		val held = engine
		if (held != null && held.language == language) return held
		held?.close()
		loadedDictStates = emptyMap()
		lastEmojiModified = -1L
		lastEmojiLength = -1L
		val made = EvvEngine.open(language)
		engine = made
		if (made != null) applySettings(made)
		return made
	}

	private fun applySettings(target: EvvEngine) {
		val s = settings ?: return
		target.setSampleRate(s.sampleRateHz)
		target.setAbbreviations(s.abbreviations)
		loadDictionaries(target, s)
		if (target.applyVoice(0, s.shape(0))) appliedRevision = s.revision
	}

	/** Teaching words happens when the files change rather than whenever any setting does. */
	private fun loadDictionaries(target: EvvEngine, s: Settings) {
		val wantPaths = s.dictionaryPaths().filterValues { java.io.File(it).exists() }
		val currentStates = wantPaths.mapValues { (_, path) ->
			val f = java.io.File(path)
			LoadedDictState(path, f.lastModified(), f.length())
		}
		val emojiPath = s.dictionaryPath(Eci.DICT_EMOJI)
			?: java.io.File(DirectBoot.dictionaries(this), "volume-3-emoji.dic").takeIf { it.exists() }?.absolutePath
		val emojiFile = emojiPath?.let { java.io.File(it) }
		val emojiModified = emojiFile?.lastModified() ?: 0L
		val emojiLength = emojiFile?.length() ?: 0L

		if (currentStates == loadedDictStates && emojiModified == lastEmojiModified && emojiLength == lastEmojiLength) {
			return
		}

		Log.i(TAG, "Reloading dictionaries because files or settings changed...")
		target.forgetDictionaries()
		val csList = mutableListOf<Dictionaries.CachedCaseSensitiveEntry>()
		for ((volume, path) in wantPaths) {
			val file = java.io.File(path)
			if (volume == Eci.DICT_EMOJI) {
				Emoji.loadUserEmoji(file)
				continue
			}
			val (taught, entries) = Dictionaries.loadWithCaseSensitive(target, volume, file)
			csList.addAll(entries)
			Log.i(TAG, "volume $volume: $taught active entries from ${file.name}")
		}
		if (emojiFile != null && emojiFile.exists()) {
			Emoji.loadUserEmoji(emojiFile)
		}
		activeCaseSensitiveEntries = csList
		loadedDictStates = currentStates
		lastEmojiModified = emojiModified
		lastEmojiLength = emojiLength
	}

	override fun onStop() {
		stopped = true
		engine?.stop()
	}

	override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
		if (request == null || callback == null) return
		stopped = false
		val found = Languages.match(request.language, request.country)
		if (found == null || !isUsEnglishLanguage(found.first)) {
			callback.error(TextToSpeech.ERROR_NOT_INSTALLED_YET)
			return
		}
		val wanted = findVoice(request.voiceName)
		val language = wanted?.first ?: found.first
		val target = ensureEngine(language)
		if (target == null) {
			callback.error(TextToSpeech.ERROR_SERVICE)
			return
		}
		val s = settings
		if (s != null) {
			if (s.revision != appliedRevision) {
				applySettings(target)
			} else {
				loadDictionaries(target, s)
			}
		}
		s?.let { target.setAbbreviations(it.abbreviations) }
		target.setRatePercent(request.speechRate)
		target.setPitchPercent(request.pitch)
		val charSeq = request.charSequenceText
		val rawText = Punctuation.expandTtsSpans(charSeq) ?: charSeq?.toString().orEmpty()
		if (rawText.isEmpty()) {
			callback.start(target.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1)
			callback.done()
			return
		}

		var processedText = rawText
		val cs = activeCaseSensitiveEntries
		if (cs.isNotEmpty()) {
			processedText = Dictionaries.applyCaseSensitiveDict(processedText, cs)
		}
		if (s?.readEmoji != false) {
			processedText = Emoji.apply(processedText)
		}
		if (s?.processNumbers == true) {
			processedText = Numbers.apply(
				processedText,
				mode = s.numberMode,
				readTimeNaturally = s.readTimeNaturally,
				readRomanNumerals = s.readRomanNumerals
			)
		} else if (s?.readTimeNaturally == true || s?.readRomanNumerals == true) {
			processedText = Numbers.apply(
				processedText,
				mode = Numbers.MODE_DEFAULT,
				readTimeNaturally = s.readTimeNaturally,
				readRomanNumerals = s.readRomanNumerals
			)
		}
		val punctLevel = if (s?.readPunctuation != false) {
			s?.punctuationLevel ?: Punctuation.LEVEL_SOME
		} else {
			Punctuation.LEVEL_NONE
		}
		processedText = Punctuation.apply(processedText, punctLevel, target.language)
		val text = TextFixes.apply(processedText)
		if (text.isEmpty()) {
			callback.start(target.sampleRateHz, AudioFormat.ENCODING_PCM_16BIT, 1)
			callback.done()
			return
		}
		val opening = Opening(callback, target.sampleRateHz)
		val pace = Pace(target.sampleRateHz * BYTES_PER_SAMPLE)
		val pauses = settings?.pauses ?: Pauses.ALL
		val prosody = Prosody.prefix(settings?.phrasePrediction ?: false)
		val pieces = TextPieces.split(text)
		for ((at, raw) in pieces.withIndex()) {
			if (stopped) break
			val piece = prosody + Pauses.apply(raw, pauses, at == pieces.lastIndex)
			if (!target.speak(piece)) {
				Log.e(TAG, "the engine refused ${piece.length} characters")
				callback.error(TextToSpeech.ERROR_SYNTHESIS)
				return
			}
			if (stopped) {
				target.stop()
				break
			}
			if (!pump(target, callback, pace, opening)) {
				remember(pace)
				return
			}
		}
		remember(pace)
		if (opening.began) callback.done()
	}

	private fun pump(target: EvvEngine, callback: SynthesisCallback, pace: Pace, opening: Opening): Boolean {
		val size = callback.maxBufferSize.coerceIn(MIN_CHUNK, MAX_CHUNK)
		val buffer = ByteArray(size)
		while (!stopped) {
			val n = target.read(buffer)
			if (n <= 0) break
			if (!opening.open()) {
				target.stop()
				return false
			}
			if (callback.audioAvailable(buffer, 0, n) != TextToSpeech.SUCCESS) {
				target.stop()
				return false
			}
			pace.handed(n)
			hold(pace)
		}
		return true
	}

	private fun remember(pace: Pace) {
		if (pace.handedMs() > mostAudioHandedMs) mostAudioHandedMs = pace.handedMs()
	}

	private fun hold(pace: Pace) {
		while (!stopped) {
			val ahead = pace.ahead() - LEAD_MS
			if (ahead <= 0) return
			try {
				Thread.sleep(minOf(ahead, SLICE_MS))
			} catch (stopping: InterruptedException) {
				Thread.currentThread().interrupt()
				return
			}
		}
	}

	private class Opening(private val callback: SynthesisCallback, private val rateHz: Int) {
		var began = false
			private set

		fun open(): Boolean {
			if (began) return true
			began = callback.start(rateHz, AudioFormat.ENCODING_PCM_16BIT, 1) == TextToSpeech.SUCCESS
			return began
		}
	}

	private class Pace(private val bytesPerSecond: Int) {
		private val from = System.nanoTime()
		private var bytes = 0L

		fun handed(more: Int) {
			bytes += more
		}

		fun handedMs(): Long = if (bytesPerSecond <= 0) 0 else bytes * 1000L / bytesPerSecond

		fun ahead(): Long = handedMs() - (System.nanoTime() - from) / 1_000_000L
	}

	internal companion object {
		@Volatile
		var mostAudioHandedMs: Long = 0

		const val TAG = "evvdroid"
		const val MIN_CHUNK = 1024
		const val MAX_CHUNK = 8192
		const val BYTES_PER_SAMPLE = 2
		const val LEAD_MS = 300L
		const val SLICE_MS = 50L
	}
}
