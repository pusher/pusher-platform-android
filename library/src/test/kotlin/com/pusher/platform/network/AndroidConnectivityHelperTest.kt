package com.pusher.platform.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.NetworkInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import mockitox.handles
import mockitox.returns
import mockitox.returnsStubAs
import mockitox.stub

class AndroidConnectivityHelperTest {

    companion object {
        private val networkInfoThat = NetworkInfoThat()
    }

    @Test
    fun shouldBe_connected() {
        val context = stub<Context> { networkInfoReturns(networkInfoThat.hasActiveConnection) }
        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isTrue()
    }

    @Test
    fun shouldBe_notConnected_whenActiveNetworkMissing() {
        val context = stub<Context> { networkInfoReturns(networkInfoThat.isAbsent) }
        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldBe_notConnected_whenActiveNetworkIsNotConnected() {
        val context = stub<Context> { networkInfoReturns(networkInfoThat.hasNoConnection) }
        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldExecuteRetryAction_whenConnected() {
        val context = stub<Context> { networkInfoReturns(networkInfoThat.hasActiveConnection) }
        val helper = AndroidConnectivityHelper(context)
        var result: String? = null

        helper.onConnected { result = "success" }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun shouldExecuteRetryAction_whenReconnected() {
        val context = stub<Context> {
            networkInfoReturns(networkInfoThat.isAbsent, networkInfoThat.hasActiveConnection)
            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) handles { (r, c) ->
                (r as BroadcastReceiver).onReceive(c as Context, null)
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
            networkInfoReturns(networkInfoThat.isAbsent, networkInfoThat.hasActiveConnection)
            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) handles { (r, _) ->
                receivers += r as BroadcastReceiver
            }
            unregisterReceiver(any()) handles { (r) -> receivers -= r as BroadcastReceiver }
        }
        val helper = AndroidConnectivityHelper(context)

        helper.onConnected { /* Do nothing */ }

        helper.cancel()

        assertThat(receivers).isEmpty()
    }

}

private fun Context.networkInfoReturns(vararg infos: NetworkInfo?) =
    getSystemService(Context.CONNECTIVITY_SERVICE).returnsStubAs<ConnectivityManager> {
        infos.forEach { activeNetworkInfo returns it }
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

