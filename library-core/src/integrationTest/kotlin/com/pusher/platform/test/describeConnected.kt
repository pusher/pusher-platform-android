package com.pusher.platform.test;

import org.jetbrains.spek.api.dsl.Spec
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.xdescribe
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Skips test if the url is not reachable.
 */
fun Spec.describeWhenReachable(uri: String, description: String, body: SpecBody.() -> Unit) = if (isReachable(uri)) {
    describe(description, body)
} else {
    xdescribe(description, "Can't reach $uri", body)
}

private fun isReachable(uri: String) : Boolean {
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier({ _, _ -> true })
    return URL(uri)
        .openConnection()
        .let { it as HttpURLConnection }
        .let {
            try {
                it.connect()
                it.disconnect()
                true
            } catch (e: Exception) {
                false
            }
        }
}
