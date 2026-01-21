package com.customtabplugin

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import org.chromium.customtabsclient.shared.CustomTabsHelper
import org.chromium.customtabsclient.shared.ServiceConnection
import org.chromium.customtabsclient.shared.ServiceConnectionCallback

/**
 * Helper class to manage the connection to the Custom Tabs Service.
 */
class CustomTabServiceHelper(context: Context) : ServiceConnectionCallback {

    private var packageNameToBind: String? = CustomTabsHelper.getPackageNameToUse(context)
    private var customTabsSession: CustomTabsSession? = null
    private var client: CustomTabsClient? = null
    private var connection: CustomTabsServiceConnection? = null
    private var connectionCallback: ConnectionCallback? = null

    val isAvailable: Boolean
        get() = !TextUtils.isEmpty(packageNameToBind)

    @Throws(CustomTabsHelper.InvalidPackageException::class)
    fun setPackageNameToBind(packageName: String, context: Context) {
        CustomTabsHelper.setPackageNameToUse(packageName, context)
        packageNameToBind = packageName
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service.
     * @param activity the activity that is connected to the service.
     */
    fun unbindCustomTabsService(activity: Activity): Boolean {
        if (connection == null) return false
        activity.unbindService(connection!!)
        client = null
        customTabsSession = null
        connection = null
        return true
    }

    /**
     * Creates or retrieves an existing CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    val session: CustomTabsSession?
        get() {
            if (client == null) {
                customTabsSession = null
            } else if (customTabsSession == null) {
                customTabsSession = client?.newSession(null)
            }
            return customTabsSession
        }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service.
     */
    fun setConnectionCallback(connectionCallback: ConnectionCallback?) {
        this.connectionCallback = connectionCallback
    }

    /**
     * Binds the Activity to the Custom Tabs Service.
     * @param activity the activity to be bound to the service.
     */
    fun bindCustomTabsService(activity: Activity): Boolean {
        if (client != null) return false
        if (packageNameToBind == null) return false

        connection = ServiceConnection(this)
        CustomTabsClient.bindCustomTabsService(activity, packageNameToBind!!, connection!!)
        return true
    }

    /**
     * @see CustomTabsSession.mayLaunchUrl
     * @return true if call to mayLaunchUrl was accepted.
     */
    fun mayLaunchUrl(uri: Uri, extras: Bundle?, otherLikelyBundles: List<Bundle>?): Boolean {
        if (client == null) return false
        val session = session ?: return false
        return session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    override fun onServiceConnected(client: CustomTabsClient) {
        this.client = client
        connectionCallback?.onCustomTabsConnected()
    }

    override fun onServiceDisconnected() {
        client = null
        customTabsSession = null
        connectionCallback?.onCustomTabsDisconnected()
    }

    val clientInstance: CustomTabsClient?
        get() = client

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected.
     */
    interface ConnectionCallback {
        /**
         * Called when the service is connected.
         */
        fun onCustomTabsConnected()

        /**
         * Called when the service is disconnected.
         */
        fun onCustomTabsDisconnected()
    }
}
