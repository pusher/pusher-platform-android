package com.pusher.platform.network

fun nameCurrentThread(name: String) =
    ThreadNomer(name)

class ThreadNomer(newName: String) {

    private val thread = Thread.currentThread()
    private val oldName = thread.name

    init {
        thread.name = newName
    }

    fun restore() {
        thread.name = oldName
    }

}