package org.mockito

import org.mockito.BDDMockito.*
import org.mockito.invocation.InvocationOnMock

fun <T> spy(instance: T, f: T.() -> Unit): T =
    spy(instance).also(f)

inline fun <reified T> mock(f: T.(T) -> Unit): T =
    mock(T::class.java).also { it.f(it) }

inline fun <reified T> mock(): T =
    mock(T::class.java)

inline fun <reified T> stub(f: T.(T) -> Unit): T =
    mock(T::class.java, withSettings().stubOnly()).also { it.f(it) }

inline fun <reified T> stub(): T =
    mock(T::class.java, withSettings().stubOnly())

infix fun <T> T.returns(value: T) {
    given(this).willReturn(value)
}

inline infix fun <reified T> T.returnsStub(f: T.(T) -> Unit) {
    val mock = mock(T::class.java, withSettings().stubOnly())//.also { it.f(it) }
    given(this).willReturn(mock)
    mock.f(mock)
}

inline infix fun <reified T> Any.returnsStubAs(f: T.(T) -> Unit) {
    val mock = mock(T::class.java, withSettings().stubOnly())//.also { it.f(it) }
    given(this).willReturn(mock)
    mock.f(mock)
}

infix fun <T> T.answers(f: InvocationOnMock.(Array<*>) -> Any) {
    given(this).willAnswer { it.f(it.arguments) }
}

infix fun <T> T.handles(f: InvocationOnMock.(Array<*>) -> Unit) {
    given(this).willAnswer { it.f(it.arguments) }
}


