package org.offlinemesh.audio

object AudioCodec {

    /**
     * G.711 Mu-law encoder: Compresses 16-bit Linear PCM to 8-bit Mu-law.
     * Cuts voice bandwidth in half while preserving speech intelligibility.
     */
    fun encodeMuLaw(pcm16: ByteArray): ByteArray {
        val out = ByteArray(pcm16.size / 2)
        var outIdx = 0
        var i = 0
        while (i < pcm16.size - 1) {
            val sample = ((pcm16[i + 1].toInt() shl 8) or (pcm16[i].toInt() and 0xFF)).toShort()
            out[outIdx++] = linearToMuLaw(sample.toInt())
            i += 2
        }
        return out
    }

    /**
     * G.711 Mu-law decoder: Expands 8-bit Mu-law back to 16-bit Linear PCM.
     */
    fun decodeMuLaw(muLaw: ByteArray): ByteArray {
        val out = ByteArray(muLaw.size * 2)
        var outIdx = 0
        for (b in muLaw) {
            val sample = muLawToLinear(b).toInt()
            out[outIdx++] = (sample and 0xFF).toByte()
            out[outIdx++] = ((sample ushr 8) and 0xFF).toByte()
        }
        return out
    }

    private const val BIAS = 0x84
    private const val CLIP = 32635

    private val segEnd = intArrayOf(0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF)

    private fun linearToMuLaw(pcmVal: Int): Byte {
        var pcm = pcmVal
        val mask = if (pcm < 0) {
            pcm = -pcm
            0x7F
        } else {
            0xFF
        }

        if (pcm > CLIP) pcm = CLIP
        pcm += BIAS

        var seg = 8
        for (i in 0 until 8) {
            if (pcm <= segEnd[i]) {
                seg = i
                break
            }
        }

        if (seg >= 8) return (0x7F xor mask).toByte()

        val uval = (seg shl 4) or ((pcm shr (seg + 3)) and 0xF)
        return (uval xor mask).toByte()
    }

    private val muLawDecompressTable = ShortArray(256) { i ->
        var uval = i.inv()
        val sign = uval and 0x80
        val exponent = (uval shr 4) and 0x07
        val mantissa = uval and 0x0F
        var sample = ((mantissa shl 3) + BIAS) shl exponent
        sample -= BIAS
        (if (sign != 0) sample else -sample).toShort()
    }

    private fun muLawToLinear(muLawByte: Byte): Short {
        return muLawDecompressTable[muLawByte.toInt() and 0xFF]
    }
}
