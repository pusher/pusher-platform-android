package com.pusher.platform.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.NetworkInfo
import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.handles
import org.mockito.returns
import org.mockito.returnsStubAs
import org.mockito.stub

class AndroidConnectivityHelperTest {

    companion object {
        private val networkInfoThat = NetworkInfoThat()
    }

    @Test
    fun shouldBe_connected() {
        val context = InstrumentationRegistry.getContext()
        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isTrue()
    }

    @Test
    fun shouldBe_notConnected_whenActiveNetworkMissing() {
        val context = stub<Context> {
            getSystemService(Context.CONNECTIVITY_SERVICE).returnsStubAs<ConnectivityManager> {
                activeNetworkInfo returns networkInfoThat.isAbsent
            }
        }

        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldBe_notConnected_whenActiveNetworkIsNotConnected() {
        val context = stub<Context> {
            getSystemService(Context.CONNECTIVITY_SERVICE).returnsStubAs<ConnectivityManager> {
                activeNetworkInfo returns networkInfoThat.hasNoConnection
            }
        }

        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldExecuteRetryAction_whenConnected() {
        val context = InstrumentationRegistry.getContext()
        val helper = AndroidConnectivityHelper(context)
        var result: String? = null

        helper.onConnected { result = "success" }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun shouldExecuteRetryAction_whenReconnected() {
        val context = stub<Context> { context ->
            getSystemService(Context.CONNECTIVITY_SERVICE).returnsStubAs<ConnectivityManager> {
                activeNetworkInfo returns networkInfoThat.isAbsent
                activeNetworkInfo returns networkInfoThat.hasActiveConnection
            }
            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) handles { (r, _) ->
                (r as BroadcastReceiver).onReceive(context, null)
            }
        }
        val helper = AndroidConnectivityHelper(context)
        var result: String? = null

        helper.onConnected {
            result = "success"
        }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun should_notCallAction_afterCancel() {
        val receivers = mutableListOf<BroadcastReceiver>()
        val context = stub<Context> {
            getSystemService(Context.CONNECTIVITY_SERVICE).returnsStubAs<ConnectivityManager> {
                activeNetworkInfo returns networkInfoThat.isAbsent
                activeNetworkInfo returns networkInfoThat.hasActiveConnection
            }
            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) handles { (r, _) ->
                receivers += r as BroadcastReceiver
            }
            unregisterReceiver(any()) handles { (r) -> receivers -= r as BroadcastReceiver }
        }
        val helper = AndroidConnectivityHelper(context)

        helper.onConnected {
            // Do nothing
        }

        helper.cancel()

        assertThat(receivers).isEmpty()
    }

}

private class NetworkInfoThat {

    val hasActiveConnection = stub<NetworkInfo> {
        isConnectedOrConnecting returns true
    }

    val hasNoConnection = stub<NetworkInfo> {
        isConnectedOrConnecting returns false
    }

    val isAbsent: NetworkInfo? = null

}

