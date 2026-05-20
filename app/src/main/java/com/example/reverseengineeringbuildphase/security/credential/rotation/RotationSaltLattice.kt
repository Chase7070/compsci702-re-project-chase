package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationSaltLattice {

    fun forEachSalt(block: (ByteArray) -> Unit) {
        block(ringA())
        block(ringB())
        block(ringC())
        block(ringD())
    }

    private fun ringA(): ByteArray = intArrayToBytes(
        intArrayOf(0x52, 0x6f, 0x74, 0x61, 0x74, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x31)
    )

    private fun ringB(): ByteArray = intArrayToBytes(
        intArrayOf(0x52, 0x6f, 0x74, 0x61, 0x74, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x32)
    )

    private fun ringC(): ByteArray = intArrayToBytes(
        intArrayOf(0x63, 0x79, 0x63, 0x6c, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x61)
    )

    private fun ringD(): ByteArray = intArrayToBytes(
        intArrayOf(0x63, 0x79, 0x63, 0x6c, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x62)
    )

    private fun intArrayToBytes(values: IntArray): ByteArray {
        val out = ByteArray(values.size)
        for (i in values.indices) {
            out[i] = values[i].toByte()
        }
        return out
    }
}
