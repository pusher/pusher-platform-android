package com.pusher.platform

import elements.emptyHeaders

data class RequestOptions(
    val destination: RequestDestination,
    val method: String = "GET",
    val headers: elements.Headers = emptyHeaders(),
    val body: String? = null
) {
    constructor(
        path: String,
        method: String = "GET",
        headers: elements.Headers = emptyHeaders(),
        body: String? = null
    ) : this(RequestDestination.Relative(path), method, headers, body)
}

sealed class RequestDestination {
    class Relative(val path: String) : RequestDestination()
    class Absolute(val url: String) : RequestDestination()
}
