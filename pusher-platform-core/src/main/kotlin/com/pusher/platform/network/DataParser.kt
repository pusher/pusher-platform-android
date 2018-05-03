package com.pusher.platform.network

import com.pusher.util.Result
import elements.Error

/**
 * Used to do type safe message and response parsing.
 */
typealias DataParser<A> = (String) -> Result<A, Error>
