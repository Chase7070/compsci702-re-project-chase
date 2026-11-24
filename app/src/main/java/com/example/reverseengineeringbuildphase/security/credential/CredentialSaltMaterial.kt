package com.example.reverseengineeringbuildphase.security.credential

/**
 * Salts split across small methods so key material is not assembled in one obvious constant pool entry.
 */
internal object CredentialSaltMaterial {

    fun forEachSalt(block: (ByteArray) -> Unit) {
        block(segmentNorth())
        block(segmentEast())
        block(segmentSouth())
        block(segmentWest())
    }

    private fun segmentNorth(): ByteArray = intArrayToBytes(
        intArrayOf(0x4e, 0x6f, 0x72, 0x74, 0x68, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x61)
    )

    private fun segmentEast(): ByteArray = intArrayToBytes(
        intArrayOf(0x45, 0x61, 0x73, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x62, 0x31)
    )

    private fun segmentSouth(): ByteArray = intArrayToBytes(
        intArrayOf(0x53, 0x6f, 0x75, 0x74, 0x68, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x63)
    )

    private fun segmentWest(): ByteArray = intArrayToBytes(
        intArrayOf(0x57, 0x65, 0x73, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x64, 0x32)
    )

    private fun intArrayToBytes(values: IntArray): ByteArray {
        val out = ByteArray(values.size)
        for (i in values.indices) {
            out[i] = values[i].toByte()
        }
        return out
    }
}
