package com.pusher

/**
 * Information to be sent with every request sent through [com.pusher.platform.BaseClient].
 */
data class SdkInfo(val product: String, val sdkVersion: String, val platform: String, val language: String)
