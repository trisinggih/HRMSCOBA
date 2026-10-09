package com.jangeby.system.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpEngine(): HttpClientEngine {
    return Darwin.create {
        configureRequest {
            setTimeoutInterval(30.0)
        }
    }
}
