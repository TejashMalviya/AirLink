package org.offlinemesh.audio

import java.util.concurrent.ConcurrentSkipListMap

/**
 * Adaptive jitter buffer for voice playout.
 * targetDelayFrames=1 means ~20ms playout latency (one frame buffered before play).
 * Packets arriving late (seq <= lastPlayedSeq) are dropped.
 */
class JitterBuffer(
    private val targetDelayFrames: Int = 1,   // Reduced from 3 → 1 for lower latency
    private val maxCapacityFrames: Int = 12
) {
    private val buffer = ConcurrentSkipListMap<Long, ByteArray>()
    private var lastPlayedSeq = -1L
    private var bufferedFrames = 0

    fun push(seq: Long, data: ByteArray) {
        if (seq <= lastPlayedSeq) return   // Late packet — discard

        buffer[seq] = data
        bufferedFrames = buffer.size

        // Hard cap: evict oldest if overflowing
        while (buffer.size > maxCapacityFrames) {
            buffer.pollFirstEntry()
        }
    }

    fun pop(): ByteArray? {
        // Play as soon as we have at least targetDelayFrames ready
        if (buffer.size < targetDelayFrames) return null

        val entry = buffer.pollFirstEntry() ?: return null
        lastPlayedSeq = entry.key
        bufferedFrames = buffer.size
        return entry.value
    }

    fun clear() {
        buffer.clear()
        lastPlayedSeq = -1L
        bufferedFrames = 0
    }

    /** Current depth in frames (for diagnostics). */
    val depth: Int get() = bufferedFrames
}
