package org.chromium.customtabsclient.shared

import android.content.ComponentName
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import java.lang.ref.WeakReference

/**
 * Implementation for the CustomTabsServiceConnection that avoids leaking the
 * ServiceConnectionCallback.
 */
class ServiceConnection(connectionCallback: ServiceConnectionCallback) : CustomTabsServiceConnection() {

    // A weak reference to the ServiceConnectionCallback to avoid leaking it.
    private val connectionCallbackRef: WeakReference<ServiceConnectionCallback> =
        WeakReference(connectionCallback)

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        val callback = connectionCallbackRef.get()
        callback?.onServiceConnected(client)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        val callback = connectionCallbackRef.get()
        callback?.onServiceDisconnected()
    }
}
