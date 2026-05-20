package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveSegmentAlpha {
    fun payload(): ByteArray = byteArrayOf(
        (0x91).toByte(), (0x2e).toByte(), (0x44).toByte(), (0xab).toByte(),
        (0x07).toByte(), (0xf3).toByte(), (0x5c).toByte(), (0x18).toByte(),
        (0x6d).toByte(), (0xb0).toByte(), (0xe2).toByte(), (0x39).toByte(),
        (0x4f).toByte(), (0x82).toByte(), (0x1a).toByte(), (0xc7).toByte(),
        (0x55).toByte(), (0x0e).toByte(), (0x73).toByte(), (0x9d).toByte()
    )
}
