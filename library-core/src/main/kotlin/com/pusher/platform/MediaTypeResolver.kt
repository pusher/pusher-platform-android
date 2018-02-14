package com.pusher.platform

import java.io.File

interface MediaTypeResolver {
    fun fileMediaType(file: File) : String
}