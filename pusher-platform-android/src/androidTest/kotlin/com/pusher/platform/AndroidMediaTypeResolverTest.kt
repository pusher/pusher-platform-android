package com.pusher.platform

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AndroidMediaTypeResolverTest(
    private val file: File,
    private val isValid: StringSubject.() -> Unit
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data() = arrayOf(
            case("image.jpg", { isEqualTo("image/jpeg") }),
            case("image.png", { isEqualTo("image/png") }),
            case("image.gif", { isEqualTo("image/gif") }),
            case("image.pdf", { isEqualTo("application/pdf") }),
            case("no-extension", { isNull() })
        )
    }

    @Test
    fun shouldResolveMediaType() {
        val resolver = AndroidMediaTypeResolver()

        val mediaType = resolver.fileMediaType(file)

        assertThat(mediaType).isValid()
    }

}

private fun case(file: String, type: StringSubject.() -> Unit) = arrayOf(File(file), type)
