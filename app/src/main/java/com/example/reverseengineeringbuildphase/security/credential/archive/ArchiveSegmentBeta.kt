package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveSegmentBeta {
    fun payload(): ByteArray = byteArrayOf(
        (0x3b).toByte(), (0x68).toByte(), (0xaf).toByte(), (0x12).toByte(),
        (0xde).toByte(), (0x47).toByte(), (0x90).toByte(), (0x2c).toByte(),
        (0x61).toByte(), (0xfb).toByte(), (0x08).toByte(), (0x85).toByte(),
        (0x1e).toByte(), (0x54).toByte(), (0xc9).toByte(), (0x76).toByte(),
        (0x0a).toByte(), (0xbc).toByte(), (0x33).toByte(), (0xee).toByte()
    )
}
