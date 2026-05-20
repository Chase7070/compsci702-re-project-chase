package com.example.reverseengineeringbuildphase.security.credential.standby

internal object StandbyFragmentAssembler {

    fun combinedPayload(): ByteArray {
        val parts = listOf(
            StandbySegmentAlpha.payload(),
            StandbySegmentBeta.payload(),
            StandbySegmentGamma.payload(),
            StandbySegmentDelta.payload()
        )
        val total = parts.sumOf { it.size }
        val out = ByteArray(total)
        var offset = 0
        for (part in parts) {
            part.copyInto(out, destinationOffset = offset)
            offset += part.size
        }
        return out
    }
}
