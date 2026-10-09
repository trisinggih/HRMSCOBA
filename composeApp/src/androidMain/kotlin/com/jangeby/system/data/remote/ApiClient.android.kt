package com.jangeby.system.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun createHttpEngine(): HttpClientEngine {
    return Android.create {
        connectTimeout = 30_000
        socketTimeout = 30_000
    }
}
