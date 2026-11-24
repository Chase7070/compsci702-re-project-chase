package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbySaltLattice {

    fun forEachSalt(block: (ByteArray) -> Unit) {
        block(tierA())
        block(tierB())
        block(tierC())
        block(tierD())
    }

    private fun tierA(): ByteArray = intArrayToBytes(
        intArrayOf(0x53, 0x74, 0x61, 0x6e, 0x64, 0x62, 0x79, 0x5f, 0x73, 0x61, 0x6c, 0x74)
    )

    private fun tierB(): ByteArray = intArrayToBytes(
        intArrayOf(0x53, 0x74, 0x61, 0x6e, 0x64, 0x62, 0x79, 0x5f, 0x73, 0x61, 0x6c, 0x74)
    )

    private fun tierC(): ByteArray = intArrayToBytes(
        intArrayOf(0x72, 0x65, 0x73, 0x65, 0x72, 0x76, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74)
    )

    private fun tierD(): ByteArray = intArrayToBytes(
        intArrayOf(0x72, 0x65, 0x73, 0x65, 0x72, 0x76, 0x65, 0x5f, 0x73, 0x61, 0x6c, 0x74)
    )

    private fun intArrayToBytes(values: IntArray): ByteArray {
        val out = ByteArray(values.size)
        for (i in values.indices) {
            out[i] = values[i].toByte()
        }
        return out
    }
}
