package com.pusher.util

import com.google.common.truth.DefaultSubject
import com.google.common.truth.Subject
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.*
import elements.Error

object ResultAssertions {

    fun <A> assertSuccess(result: Result<A, Error>): Subject<DefaultSubject, Any> {
        assertThat(result).isInstanceOf(Result.Success::class.java)
        return assertThat((result as Result.Success).value)
    }

    fun <A> assertFailure(result: Result<A, Error>): ThrowableSubject {
        assertThat(result).isInstanceOf(Result.Failure::class.java)
        return assertThat((result as Result.Failure).error)
    }
}
