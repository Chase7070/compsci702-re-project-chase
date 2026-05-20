package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbySegmentAlpha {
    fun payload(): ByteArray = byteArrayOf(
        (0x5f).toByte(), (0xc3).toByte(), (0x18).toByte(), (0x94).toByte(),
        (0x2b).toByte(), (0x70).toByte(), (0xea).toByte(), (0x06).toByte(),
        (0x9d).toByte(), (0x41).toByte(), (0xb7).toByte(), (0x23).toByte(),
        (0x88).toByte(), (0x0e).toByte(), (0x76).toByte(), (0xfc).toByte(),
        (0x39).toByte(), (0xa5).toByte(), (0x52).toByte(), (0x1d).toByte()
    )
}
