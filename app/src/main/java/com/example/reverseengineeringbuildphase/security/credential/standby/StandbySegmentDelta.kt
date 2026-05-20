package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbySegmentDelta {
    fun payload(): ByteArray = byteArrayOf(
        (0x72).toByte(), (0x09).toByte(), (0xbe).toByte(), (0x45).toByte(),
        (0xda).toByte(), (0x27).toByte(), (0x91).toByte(), (0x58).toByte(),
        (0xec).toByte(), (0x34).toByte(), (0x80).toByte(), (0x1a).toByte(),
        (0x67).toByte(), (0xf3).toByte(), (0x0e).toByte(), (0x9b).toByte(),
        (0x28).toByte(), (0xd5).toByte(), (0x61).toByte(), (0xb4).toByte()
    )
}
