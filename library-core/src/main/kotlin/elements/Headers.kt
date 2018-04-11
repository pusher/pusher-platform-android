package elements

typealias Headers = Map<String, List<String>>

inline val Headers.retryAfter : Long
    get() = (this["Retry-After"]?.firstOrNull()?.toLong()?:0) * 1_000
