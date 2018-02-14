package com.pusher.platform
import java.util.TreeMap

data class RequestOptions(
        val method: String = "GET",
        val destination: RequestDestination,
        val headers: elements.Headers = TreeMap(),
        val body: String? = null
) {
    constructor(
            method: String = "GET",
            path: String,
            headers: elements.Headers = TreeMap(),
            body: String? = null
    ): this(method, RequestDestination.Relative(path), headers, body)
}

sealed class RequestDestination {
    class Relative(val path: String) : RequestDestination()
    class Absolute(val url: String) : RequestDestination()
}
