package com.pusher.platform.logger

class AndroidLogger(val threshold: LogLevel): Logger {

    override fun verbose(message: String, error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun debug(message: String, error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun info(message: String, error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun warn(message: String, error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun error(message: String, error: Error?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun log(logLevel: LogLevel, message: String, error: Error?){
        if(logLevel >= threshold){

        }
    }
}