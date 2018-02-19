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
import org.mockito.answers
import org.mockito.handles
import org.mockito.returns
import org.mockito.stub

class AndroidConnectivityHelperTest {

    companion object {
        private val networkInfo = NetworkInfo()
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
            val connectivityManager = stub<ConnectivityManager> {
                activeNetworkInfo returns null
            }
            getSystemService(Context.CONNECTIVITY_SERVICE) returns connectivityManager
        }

        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldBE_notConnected_whenActiveNetworkIsNotConnected() {
        val context = stub<Context> {
            val connectivityManager = stub<ConnectivityManager> {
                activeNetworkInfo returns networkInfo.withoutConnection
            }
            getSystemService(Context.CONNECTIVITY_SERVICE) returns connectivityManager
        }

        val helper = AndroidConnectivityHelper(context)

        assertThat(helper.isConnected()).isFalse()
    }

    @Test
    fun shouldExecuteRetryAction_whenConnected() {
        val context = InstrumentationRegistry.getContext()
        val helper = AndroidConnectivityHelper(context)
        var result: String? = null

        helper.onConnected {
            result = "success"
        }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun shouldExecuteRetryAction_whenReconnected() {
        val context = stub<Context> { context ->
            val connectivityManager = stub<ConnectivityManager> {
                activeNetworkInfo returns networkInfo.absent
                activeNetworkInfo returns networkInfo.withActiveConnection
            }
            getSystemService(Context.CONNECTIVITY_SERVICE) returns connectivityManager
            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) answers { (receiver, _) ->
                (receiver as BroadcastReceiver).onReceive(context, null)
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
            val connectivityManager = stub<ConnectivityManager> {
                activeNetworkInfo returns networkInfo.absent
                activeNetworkInfo returns networkInfo.withActiveConnection
            }
            getSystemService(Context.CONNECTIVITY_SERVICE) returns connectivityManager

            registerReceiver(any(), eq(IntentFilter(CONNECTIVITY_ACTION))) handles { (receiver, _) ->
                receivers += (receiver as BroadcastReceiver)
            }
            unregisterReceiver(any()) handles { (receiver) ->
                receivers -= receiver as BroadcastReceiver
            }
        }

        val helper = AndroidConnectivityHelper(context)

        helper.onConnected {
            // Do nothing
        }

        helper.cancel()

        assertThat(receivers).isEmpty()
    }

}

private class NetworkInfo {

    val withActiveConnection = stub<NetworkInfo> {
        isConnectedOrConnecting returns true
    }

    val withoutConnection = stub<NetworkInfo> {
        isConnectedOrConnecting returns false
    }

    val absent : NetworkInfo? = null

}

