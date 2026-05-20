package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationSegmentBeta {
    fun payload(): ByteArray = byteArrayOf(
        (0x7c).toByte(), (0x11).toByte(), (0x89).toByte(), (0xde).toByte(),
        (0x46).toByte(), (0xba).toByte(), (0x03).toByte(), (0x70).toByte(),
        (0xed).toByte(), (0x29).toByte(), (0x95).toByte(), (0x52).toByte(),
        (0x0f).toByte(), (0x86).toByte(), (0x3a).toByte(), (0xc4).toByte(),
        (0x67).toByte(), (0x1b).toByte(), (0xaf).toByte(), (0x74).toByte()
    )
}
