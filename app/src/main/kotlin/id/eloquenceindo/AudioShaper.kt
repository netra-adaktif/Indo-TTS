package id.eloquenceindo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Shapes the speech the engine made before Android gets it, so that it is
 * loud and clear on a phone's small speaker without breaking up.
 *
 * Three profiles, the way the settings screen offers them:
 *
 * - Balanced: only the lowest rumble (under about 70 Hz) is cut, the range
 *   where consonants live is lifted a little, and the whole is made somewhat
 *   louder. A man's voice such as Reed's sits at about 100 to 130 Hz, so the
 *   cut stays below it.
 * - Quality: nothing is touched. What the engine made is what is heard.
 * - Volume: a phone speaker cannot play the bass anyway, and the energy spent
 *   on it is what makes loud speech break up, so more of it is cut (under
 *   about 130 Hz), the consonants are lifted more, and the gain is greater.
 *
 * Whatever the gain, a limiter holds the peaks down and a soft clip after it
 * keeps anything that got past from ever reaching full scale, so louder does
 * not mean broken.
 *
 * One of these is made for each utterance. The filters and the limiter carry
 * their state from one buffer to the next, so a buffer is never processed on
 * its own.
 */
class AudioShaper(profile: Int, rateHz: Int) {

	private val stages: List<Biquad>
	private val gain: Double
	private val active: Boolean

	private var reduction = 1.0
	private val attack = 1.0 - exp(-1.0 / (ATTACK_SECONDS * rateHz))
	private val release = 1.0 - exp(-1.0 / (RELEASE_SECONDS * rateHz))

	init {
		when (profile) {
			PROFILE_QUALITY -> {
				stages = emptyList()
				gain = 1.0
				active = false
			}
			PROFILE_VOLUME -> {
				stages = listOf(
					Biquad.highPass(130.0, 0.7071, rateHz),
					Biquad.peaking(3000.0, 5.0, 0.8, rateHz)
				)
				gain = 3.2
				active = true
			}
			else -> {
				stages = listOf(
					Biquad.highPass(70.0, 0.7071, rateHz),
					Biquad.peaking(2800.0, 2.5, 0.9, rateHz)
				)
				gain = 1.6
				active = true
			}
		}
	}

	/** Shapes the first [length] bytes of [buffer] in place. They are 16 bit
	 *  little endian samples, which is what the engine hands over. */
	fun process(buffer: ByteArray, length: Int) {
		if (!active) return
		val end = (minOf(length, buffer.size) / 2) * 2
		var i = 0
		while (i < end) {
			val lo = buffer[i].toInt() and 0xFF
			val hi = buffer[i + 1].toInt()
			var x = ((hi shl 8) or lo).toShort().toDouble() / 32768.0
			for (stage in stages) x = stage.run(x)
			x *= gain
			val level = abs(x)
			val wanted = if (level > LIMIT) LIMIT / level else 1.0
			reduction += (wanted - reduction) * (if (wanted < reduction) attack else release)
			x = soften(x * reduction)
			val out = (x * 32767.0).roundToInt().coerceIn(-32768, 32767)
			buffer[i] = (out and 0xFF).toByte()
			buffer[i + 1] = ((out shr 8) and 0xFF).toByte()
			i += 2
		}
	}

	private fun soften(x: Double): Double {
		val a = abs(x)
		if (a <= KNEE) return x
		val s = KNEE + (CEILING - KNEE) * tanh((a - KNEE) / (CEILING - KNEE))
		return if (x < 0.0) -s else s
	}

	private class Biquad(
		private val b0: Double,
		private val b1: Double,
		private val b2: Double,
		private val a1: Double,
		private val a2: Double
	) {
		private var z1 = 0.0
		private var z2 = 0.0

		fun run(x: Double): Double {
			val y = b0 * x + z1
			z1 = b1 * x - a1 * y + z2
			z2 = b2 * x - a2 * y
			return y
		}

		companion object {
			fun highPass(hz: Double, q: Double, rate: Int): Biquad {
				val w = 2.0 * PI * hz / rate
				val alpha = sin(w) / (2.0 * q)
				val c = cos(w)
				val a0 = 1.0 + alpha
				return Biquad(
					(1.0 + c) / 2.0 / a0,
					-(1.0 + c) / a0,
					(1.0 + c) / 2.0 / a0,
					-2.0 * c / a0,
					(1.0 - alpha) / a0
				)
			}

			fun peaking(hz: Double, db: Double, q: Double, rate: Int): Biquad {
				val a = 10.0.pow(db / 40.0)
				val w = 2.0 * PI * hz / rate
				val alpha = sin(w) / (2.0 * q)
				val c = cos(w)
				val a0 = 1.0 + alpha / a
				return Biquad(
					(1.0 + alpha * a) / a0,
					-2.0 * c / a0,
					(1.0 - alpha * a) / a0,
					-2.0 * c / a0,
					(1.0 - alpha / a) / a0
				)
			}
		}
	}

	companion object {
		const val PROFILE_BALANCED = 0
		const val PROFILE_QUALITY = 1
		const val PROFILE_VOLUME = 2

		private const val LIMIT = 0.80
		private const val KNEE = 0.88
		private const val CEILING = 0.98
		private const val ATTACK_SECONDS = 0.0005
		private const val RELEASE_SECONDS = 0.08
	}
}
