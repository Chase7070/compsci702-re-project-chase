package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbySegmentBeta {
    fun payload(): ByteArray = byteArrayOf(
        (0x0a).toByte(), (0x87).toByte(), (0x3e).toByte(), (0xd1).toByte(),
        (0x66).toByte(), (0x1c).toByte(), (0xab).toByte(), (0x49).toByte(),
        (0xf4).toByte(), (0x75).toByte(), (0x02).toByte(), (0x98).toByte(),
        (0x2f).toByte(), (0xe0).toByte(), (0x54).toByte(), (0xbb).toByte(),
        (0x07).toByte(), (0x6d).toByte(), (0xc8).toByte(), (0x31).toByte()
    )
}
