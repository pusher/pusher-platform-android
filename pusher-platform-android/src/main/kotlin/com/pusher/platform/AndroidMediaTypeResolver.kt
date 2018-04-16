package com.pusher.platform

import android.webkit.MimeTypeMap
import java.io.File

class AndroidMediaTypeResolver : MediaTypeResolver {
    override fun fileMediaType(file: File): String? =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
}

