package com.pusher.platform.network

fun String.replaceMultipleSlashesInUrl(): String = this.replace("(?<=[^:\\s])(/+/)".toRegex(), "/").trimEnd('/')
