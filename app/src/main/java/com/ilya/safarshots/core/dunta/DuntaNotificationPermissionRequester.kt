package com.ilya.safarshots.core.dunta

interface DuntaNotificationPermissionRequester {
    fun requestDuntaNotificationPermission(onResult: (Boolean) -> Unit)
}
