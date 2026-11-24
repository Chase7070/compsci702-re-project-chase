package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationSegmentAlpha {
    fun payload(): ByteArray = byteArrayOf(
        (0x24).toByte(), (0x9f).toByte(), (0x56).toByte(), (0xc1).toByte(),
        (0x0d).toByte(), (0x78).toByte(), (0xe4).toByte(), (0x31).toByte(),
        (0xac).toByte(), (0x17).toByte(), (0x6b).toByte(), (0xd8).toByte(),
        (0x43).toByte(), (0x90).toByte(), (0x2e).toByte(), (0xf7).toByte(),
        (0x5a).toByte(), (0x08).toByte(), (0xbd).toByte(), (0x64).toByte()
    )
}
