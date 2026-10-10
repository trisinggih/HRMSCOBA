package com.jangeby.system

import androidx.compose.ui.window.ComposeUIViewController
import com.jangeby.system.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    App()
}

fun initIOSKoin() {
    initKoin()
}
