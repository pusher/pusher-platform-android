package com.pusher.platform

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AndroidMediaTypeResolverTest(
    private val file: File,
    private val expectedMediaType: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data() = arrayOf(
            case("image.jpg", "image/jpeg"),
            case("image.png", "image/png"),
            case("image.gif", "image/gif"),
            case("image.pdf", "application/pdf")
        )
    }

    @Test
    fun shouldResolveMediaType() {
        val resolver = AndroidMediaTypeResolver()

        val mediaType = resolver.fileMediaType(file)

        Truth.assertThat(mediaType).isEqualTo(expectedMediaType)

    }
}

private fun case(file: String, type: String) = arrayOf(File(file), type)