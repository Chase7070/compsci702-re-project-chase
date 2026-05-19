package com.example.reverseengineeringbuildphase.security.credential

import com.example.reverseengineeringbuildphase.security.credential.fragments.MaterialSegmentAlpha
import com.example.reverseengineeringbuildphase.security.credential.fragments.MaterialSegmentBeta
import com.example.reverseengineeringbuildphase.security.credential.fragments.MaterialSegmentGamma
import com.example.reverseengineeringbuildphase.security.credential.fragments.MaterialSegmentDelta

/**
 * Combines build-generated ciphertext segments. [combinedPayload] is populated by Gradle codegen.
 */
internal object CredentialFragmentAssembler {

    fun combinedPayload(): ByteArray {
        val parts = listOf(
            MaterialSegmentAlpha.payload(),
            MaterialSegmentBeta.payload(),
            MaterialSegmentGamma.payload(),
            MaterialSegmentDelta.payload()
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
