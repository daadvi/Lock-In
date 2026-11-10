package com.example.service

/** Strict bounds checks before reading untrusted IPv4/UDP DNS packets. */
internal object DnsPacket {
    data class Question(val domain: String, val end: Int)

    private fun u16(bytes: ByteArray, at: Int) =
        ((bytes[at].toInt() and 255) shl 8) or (bytes[at + 1].toInt() and 255)

    fun isDnsQuery(bytes: ByteArray, length: Int): Boolean {
        if (length < 28 || length > bytes.size) return false
        val ihl = (bytes[0].toInt() and 15) * 4
        if ((bytes[0].toInt() and 255) ushr 4 != 4 || ihl < 20 || ihl + 8 > length) return false
        if (bytes[9].toInt() != 17 || u16(bytes, 6) and 0x3fff != 0) return false
        val total = u16(bytes, 2)
        val udp = u16(bytes, ihl + 4)
        return total in (ihl + 8)..length && udp >= 20 && ihl + udp <= total &&
            u16(bytes, ihl + 2) == 53 && question(bytes, ihl + 8, udp - 8) != null
    }

    fun question(bytes: ByteArray, offset: Int, length: Int): Question? {
        if (offset < 0 || length < 17 || offset > bytes.size - length) return null
        val end = offset + length
        if (bytes[offset + 2].toInt() and 0xf8 != 0 || u16(bytes, offset + 4) != 1) return null
        var pos = offset + 12
        val labels = mutableListOf<String>()
        while (pos < end) {
            val size = bytes[pos++].toInt() and 255
            if (size == 0) {
                if (labels.isEmpty() || pos + 4 > end) return null
                return Question(labels.joinToString(".").lowercase(java.util.Locale.ROOT), pos + 4)
            }
            // Reject compression pointers in questions instead of following attacker-controlled cycles.
            if (size > 63 || pos + size > end || pos + size - offset - 12 > 253) return null
            if ((pos until pos + size).any { (bytes[it].toInt() and 255) !in 33..126 }) return null
            labels.add(String(bytes, pos, size, Charsets.US_ASCII))
            pos += size
        }
        return null
    }
}
