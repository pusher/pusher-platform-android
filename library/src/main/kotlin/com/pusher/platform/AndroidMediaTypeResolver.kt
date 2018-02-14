package com.pusher.platform

import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType
import java.io.File

class AndroidMediaTypeResolver : MediaTypeResolver {
    override fun fileMediaType(file: File): String = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString()))
}