package com.pusher.platform.network

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ThreadNomerTest {

    @Test
    fun `changes name of thread`() {
        ThreadNomer("some name")

        assertThat(Thread.currentThread().name).isEqualTo("some name")
    }

    @Test
    fun `restores name of thread`() {
        val previousName = Thread.currentThread().name
        ThreadNomer("some name").restore()

        assertThat(Thread.currentThread().name).isEqualTo(previousName)
    }


}
