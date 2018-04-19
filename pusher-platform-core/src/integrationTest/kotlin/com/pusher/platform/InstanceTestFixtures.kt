package com.pusher.platform

import com.pusher.SdkInfo
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.test.AlwaysOnlineConnectivityHelper
import com.pusher.platform.test.AsyncScheduler
import com.pusher.platform.test.describeWhenReachable
import com.pusher.platform.test.insecureOkHttpClient
import mockitox.stub
import okhttp3.OkHttpClient
import java.util.logging.Level
import java.util.logging.Logger.getLogger as findJavaLoggerFor

/**
 * If nothing is set up the default location to the sdk tester is assumed to be localhost:10443.
 * When the property 'pusher_platform_sdk_tester_host' is set in ~/.gradle/gradle.properties or
 * as an environment variable. In either case, if the path is not reachable, any tests using
 * [describeWhenReachable] will be skipped.
 */
val HOST: String = System.getProperty("pusher_platform_sdk_tester_host", "localhost:10443")

val testHttpClient = BaseClient(
    host = HOST,
    dependencies = TestDependencies()
).also {
    findJavaLoggerFor(OkHttpClient::class.java.name).level = Level.FINE
}

val insecureHttpClient = testHttpClient.copy(client = insecureOkHttpClient)

class TestDependencies : PlatformDependencies {
    override val logger: Logger = object : Logger {
        override fun verbose(message: String, error: Error?) = log("V:", message, error)
        override fun debug(message: String, error: Error?) = log("D:", message, error)
        override fun info(message: String, error: Error?) = log("I:", message, error)
        override fun warn(message: String, error: Error?) = log("W:", message, error)
        override fun error(message: String, error: Error?) = log("E:", message, error)
        private fun log(type: String, message: String, error: Error?) =
            println("$type: $message ${error?.let { "\n" + it } ?: ""}")
    }
    override val mediaTypeResolver: MediaTypeResolver = stub()
    override val connectivityHelper: ConnectivityHelper = AlwaysOnlineConnectivityHelper
    override val sdkInfo: SdkInfo = SdkInfo(
        product = "Instance Integration Tests",
        language = "Spek",
        platform = "JUnit",
        sdkVersion = "test"
    )
    override val scheduler: Scheduler = AsyncScheduler()
    override val mainScheduler: MainThreadScheduler = AsyncScheduler()
}
