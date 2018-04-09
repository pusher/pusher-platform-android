package com.pusher.platform

import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.test.*
import mockitox.stub
import org.jetbrains.spek.api.Spek

private const val PATH_10_AND_EOS = "subscribe10"
private const val PATH_3_AND_OPEN = "subscribe_3_continuous"
private const val PATH_0_EOS = "subscribe_0_eos"

private const val HOST = "localhost:10443"

class InstanceIntegrationSpek : Spek({

    describeWhenReachable("https://$HOST", "Instance Subscribe") {
        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            host = HOST,
            logger = stub(),
            connectivityHelper = AlwaysOnlineConnectivityHelper,
            scheduler = SyncScheduler(),
            mainThreadScheduler = SyncScheduler(),
            mediatypeResolver = stub(),
            baseClient = baseClient
        )

        will("subscribe and terminate on EOS after receiving all events") {
            instance.subscribeNonResuming(
                path = PATH_10_AND_EOS,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = observeUntil(
                    open = 1,
                    events = 10,
                    end = 1
                )
            )
        }
    }
})

val baseClient = BaseClient(
    host = HOST,
    scheduler = SyncScheduler(),
    connectivityHelper = AlwaysOnlineConnectivityHelper,
    mainScheduler = SyncScheduler(),
    logger = stub(),
    mediaTypeResolver = stub(),
    client = insecureOkHttpClient
)



