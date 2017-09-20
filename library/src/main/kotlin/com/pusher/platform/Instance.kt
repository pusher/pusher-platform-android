package com.pusher.platform


class Instance(
        instanceId: String,
        serviceName: String,
        serviceVersion: String,
        host: String? = null
        ) {



    val id: String = instanceId.split(":")[2]
    val cluster: String = instanceId.split(":")[1]
    val platformVersion: String = instanceId.split(":")[0]


    val baseClient: BaseClient? = null

    fun subscribeResuming(): Subscription {
        //TODO("Not yet implemented")
        throw NotImplementedError("Not yet implemented")
    }

    fun subscribeNonResuming(): Subscription {
        //TODO("Not yet implemented")
        throw NotImplementedError("Not yet implemented")
    }

    fun request(options: RequestOptions): CancelableRequest {
        //TODO("Not yet implemented")
        throw NotImplementedError("Not yet implemented")
    }


}


