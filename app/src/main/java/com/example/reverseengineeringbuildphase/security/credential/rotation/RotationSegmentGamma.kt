package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationSegmentGamma {
    fun payload(): ByteArray = byteArrayOf(
        (0x35).toByte(), (0xca).toByte(), (0x62).toByte(), (0x9e).toByte(),
        (0x04).toByte(), (0x7b).toByte(), (0xf1).toByte(), (0x28).toByte(),
        (0xb6).toByte(), (0x4d).toByte(), (0x80).toByte(), (0x19).toByte(),
        (0x6f).toByte(), (0xd2).toByte(), (0x57).toByte(), (0x0c).toByte(),
        (0x93).toByte(), (0x2f).toByte(), (0xe8).toByte(), (0x41).toByte()
    )
}
