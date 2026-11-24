package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveSegmentGamma {
    fun payload(): ByteArray = byteArrayOf(
        (0x58).toByte(), (0xcd).toByte(), (0x21).toByte(), (0x9a).toByte(),
        (0x04).toByte(), (0x7f).toByte(), (0xe6).toByte(), (0x3d).toByte(),
        (0xb2).toByte(), (0x16).toByte(), (0x8c).toByte(), (0x40).toByte(),
        (0xf5).toByte(), (0x69).toByte(), (0x0b).toByte(), (0x97).toByte(),
        (0x2a).toByte(), (0xd4).toByte(), (0x51).toByte(), (0x83).toByte()
    )
}
