package com.example.reverseengineeringbuildphase.security.credential.archive

internal object ArchiveFragmentAssembler {

    fun combinedPayload(): ByteArray {
        val parts = listOf(
            ArchiveSegmentAlpha.payload(),
            ArchiveSegmentBeta.payload(),
            ArchiveSegmentGamma.payload(),
            ArchiveSegmentDelta.payload()
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
