package com.pusher.platform
import java.util.TreeMap

data class RequestOptions(
        val method: String = "GET",
        val path: String,
        val headers: elements.Headers = TreeMap(),
        val body: String? = null
)