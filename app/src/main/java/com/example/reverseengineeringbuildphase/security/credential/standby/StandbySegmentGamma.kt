package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbySegmentGamma {
    fun payload(): ByteArray = byteArrayOf(
        (0x44).toByte(), (0xb9).toByte(), (0x1f).toByte(), (0x8e).toByte(),
        (0x03).toByte(), (0x6a).toByte(), (0xd7).toByte(), (0x25).toByte(),
        (0x90).toByte(), (0x4c).toByte(), (0xf8).toByte(), (0x16).toByte(),
        (0x83).toByte(), (0x2e).toByte(), (0xad).toByte(), (0x50).toByte(),
        (0x0b).toByte(), (0x97).toByte(), (0x3c).toByte(), (0xe6).toByte()
    )
}
