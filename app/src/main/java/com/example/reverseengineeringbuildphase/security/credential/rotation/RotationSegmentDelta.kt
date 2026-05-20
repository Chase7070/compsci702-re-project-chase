package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationSegmentDelta {
    fun payload(): ByteArray = byteArrayOf(
        (0x8a).toByte(), (0x36).toByte(), (0xcf).toByte(), (0x12).toByte(),
        (0x59).toByte(), (0xe3).toByte(), (0x07).toByte(), (0x7d).toByte(),
        (0xb4).toByte(), (0x20).toByte(), (0x9c).toByte(), (0x48).toByte(),
        (0xf6).toByte(), (0x6a).toByte(), (0x15).toByte(), (0x83).toByte(),
        (0x3e).toByte(), (0xd0).toByte(), (0x5b).toByte(), (0xa9).toByte()
    )
}
