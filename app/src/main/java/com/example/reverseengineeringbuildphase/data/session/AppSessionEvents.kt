package com.example.reverseengineeringbuildphase.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-cutting session signals (e.g. refresh failure → UI should prompt re-login).
 */
object AppSessionEvents {
    private val _reloginSuggested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reloginSuggested: SharedFlow<Unit> = _reloginSuggested.asSharedFlow()

    fun notifyReloginSuggested() {
        _reloginSuggested.tryEmit(Unit)
    }
}
