package com.pusher.platform

import android.os.Handler
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.subscription.Retry
import com.pusher.platform.subscription.RetryStrategyResult
import elements.Error
import elements.ErrorResponse
import elements.NetworkError

class ErrorResolver(val connectivityHelper: ConnectivityHelper) {

    var errorBeingResolved: Any = {}
    val handler = Handler()
    var retryNow: (() -> Unit)? = null

    fun resolveError(error: Error, callback: (RetryStrategyResult) -> Unit){

        when(error){
            is NetworkError -> {
                retryNow = { callback(Retry(0))}
                connectivityHelper.onConnected(retryNow!!)
            }
            is ErrorResponse -> {

                //Retry-After present
                if (error.headers["Retry-After"] != null) {
                    val retryAfter = error.headers["Retry-After"]!![0].toLong() * 1000
                    retryNow = { callback(Retry(retryAfter))}
                    handler.postDelayed(retryNow, retryAfter)
                }

                //Retry-After NOT present

            }
        }


        //network error - > use conn helper

        //error has a Retry-After -> wait that long

        //error has a generic error -> use exponential backoff
    }


    fun cancel() {
        if(retryNow != null){
            handler.removeCallbacks(retryNow)
        }

    }

}