package com.example.reverseengineeringbuildphase.security.credential.rotation

internal object RotationFragmentAssembler {

    fun combinedPayload(): ByteArray {
        val parts = listOf(
            RotationSegmentAlpha.payload(),
            RotationSegmentBeta.payload(),
            RotationSegmentGamma.payload(),
            RotationSegmentDelta.payload()
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
