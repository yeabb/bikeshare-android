package com.bikeshare.app.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide channel for auth events that originate outside the UI layer.
 *
 * The TokenAuthenticator runs on an OkHttp background thread and has no
 * access to the NavController. When a refresh fails and the user must be
 * logged out, it calls [emitLogout] here. MainActivity observes [logoutEvent]
 * and handles the navigation.
 */
object AuthEventBus {
    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent

    fun emitLogout() {
        _logoutEvent.tryEmit(Unit)
    }
}
