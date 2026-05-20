package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveSaltLattice {

    fun forEachSalt(block: (ByteArray) -> Unit) {
        block(latticeA())
        block(latticeB())
        block(latticeC())
        block(latticeD())
    }

    private fun latticeA(): ByteArray = intArrayToBytes(
        intArrayOf(0x41, 0x72, 0x63, 0x68, 0x5f, 0x6c, 0x61, 0x74, 0x74, 0x69, 0x63, 0x65)
    )

    private fun latticeB(): ByteArray = intArrayToBytes(
        intArrayOf(0x41, 0x72, 0x63, 0x68, 0x5f, 0x6c, 0x61, 0x74, 0x74, 0x69, 0x63, 0x65)
    )

    private fun latticeC(): ByteArray = intArrayToBytes(
        intArrayOf(0x76, 0x61, 0x75, 0x6c, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x61)
    )

    private fun latticeD(): ByteArray = intArrayToBytes(
        intArrayOf(0x76, 0x61, 0x75, 0x6c, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x62)
    )

    private fun intArrayToBytes(values: IntArray): ByteArray {
        val out = ByteArray(values.size)
        for (i in values.indices) {
            out[i] = values[i].toByte()
        }
        return out
    }
}
