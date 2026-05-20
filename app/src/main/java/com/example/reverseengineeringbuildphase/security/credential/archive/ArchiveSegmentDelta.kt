package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveSegmentDelta {
    fun payload(): ByteArray = byteArrayOf(
        (0x6e).toByte(), (0x13).toByte(), (0x88).toByte(), (0xdf).toByte(),
        (0x42).toByte(), (0xba).toByte(), (0x05).toByte(), (0x79).toByte(),
        (0xec).toByte(), (0x30).toByte(), (0xa7).toByte(), (0x1c).toByte(),
        (0x64).toByte(), (0x9b).toByte(), (0xf0).toByte(), (0x27).toByte(),
        (0x5e).toByte(), (0x02).toByte(), (0xad).toByte(), (0x71).toByte()
    )
}
