package com.customtabplugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import android.text.TextUtils
import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.chromium.customtabsclient.shared.CustomTabsHelper

class ChromeCustomTabPlugin : CordovaPlugin() {

    companion object {
        const val TAG = "ChromeCustomTabPlugin"
        const val CUSTOM_TAB_REQUEST_CODE = 1
    }

    private lateinit var customTabPluginHelper: CustomTabServiceHelper
    private var wasConnected: Boolean = false
    private var callbackContextRef: CallbackContext? = null
    private var startAnimationBundle: Bundle? = null

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)
        customTabPluginHelper = CustomTabServiceHelper(cordova.activity)
    }

    @Throws(JSONException::class)
    override fun execute(action: String, args: JSONArray, callbackContext: CallbackContext): Boolean {
        val context: Context = cordova.context

        when (action) {
            "isAvailable" -> {
                callbackContext.sendPluginResult(
                    PluginResult(PluginResult.Status.OK, customTabPluginHelper.isAvailable)
                )
                return true
            }

            "show" -> {
                val options = args.getJSONObject(0)
                val url = options.optString("url")
                if (TextUtils.isEmpty(url)) {
                    val result = JSONObject().apply {
                        put("error", "expected argument 'url' to be non empty string.")
                    }
                    val pluginResult = PluginResult(PluginResult.Status.ERROR, result)
                    callbackContext.sendPluginResult(pluginResult)
                    return true
                }

                val toolbarColor = options.optString("toolbarColor")
                val showDefaultShareMenuItem = options.optBoolean("showDefaultShareMenuItem")
                var transition = ""
                startAnimationBundle = null
                val animated = options.optBoolean("animated", true)
                if (animated) transition = options.optString("transition", "slide")

                val result = JSONObject()
                val pluginResult: PluginResult = if (isAvailable()) {
                    try {
                        show(url, getColor(toolbarColor), showDefaultShareMenuItem, transition)
                        result.put("event", "loaded")
                        PluginResult(PluginResult.Status.OK, result).also {
                            it.keepCallback = true
                            callbackContextRef = callbackContext
                        }
                    } catch (ex: Exception) {
                        result.put("error", ex.message)
                        PluginResult(PluginResult.Status.ERROR, result)
                    }
                } else {
                    result.put("error", "custom tabs are not available")
                    PluginResult(PluginResult.Status.ERROR, result)
                }

                callbackContext.sendPluginResult(pluginResult)
                return true
            }

            "getViewHandlerPackages" -> {
                val result = JSONObject().apply {
                    put("defaultHandler", CustomTabsHelper.getDefaultViewHandlerPackageName(context))
                    put(
                        "customTabsImplementations",
                        JSONArray(CustomTabsHelper.getPackagesSupportingCustomTabs(context))
                    )
                }
                val pluginResult = PluginResult(PluginResult.Status.OK, result)
                callbackContext.sendPluginResult(pluginResult)
                return true
            }

            "useCustomTabsImplementation" -> {
                val result = JSONObject()
                val packageName = args.optString(0)
                val pluginResult: PluginResult = if (TextUtils.isEmpty(packageName)) {
                    result.put("error", "expected argument 'packageName' to be non empty string.")
                    PluginResult(PluginResult.Status.ERROR, result)
                } else {
                    try {
                        customTabPluginHelper.setPackageNameToBind(packageName, context)
                        PluginResult(PluginResult.Status.OK, true)
                    } catch (e: CustomTabsHelper.InvalidPackageException) {
                        PluginResult(PluginResult.Status.ERROR, "Invalid package: $packageName")
                    }
                }
                callbackContext.sendPluginResult(pluginResult)
                return true
            }

            "connectToService" -> {
                if (bindCustomTabsService()) {
                    callbackContext.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
                } else {
                    callbackContext.sendPluginResult(
                        PluginResult(PluginResult.Status.ERROR, "Failed to connect to service")
                    )
                }
                return true
            }

            "warmUp" -> {
                if (warmUp()) {
                    callbackContext.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
                } else {
                    callbackContext.sendPluginResult(
                        PluginResult(PluginResult.Status.ERROR, "Failed to warm up service")
                    )
                }
                return true
            }

            "mayLaunchUrl" -> {
                val url = args.getString(0)
                if (mayLaunchUrl(url)) {
                    callbackContext.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
                } else {
                    callbackContext.sendPluginResult(
                        PluginResult(
                            PluginResult.Status.ERROR,
                            String.format("Failed prepare to launch url: %s", url)
                        )
                    )
                }
                return true
            }

            "hide" -> {
                cordova.activity.startActivity(
                    Intent(cordova.activity, cordova.activity.javaClass)
                )
                callbackContext.sendPluginResult(PluginResult(PluginResult.Status.OK, true))
                return true
            }
        }

        return false
    }

    private fun isAvailable(): Boolean = customTabPluginHelper.isAvailable

    private fun show(url: String, @ColorInt toolbarColor: Int, showDefaultShareMenuItem: Boolean, transition: String) {
        val builder = CustomTabsIntent.Builder(session).setToolbarColor(toolbarColor)
        if (showDefaultShareMenuItem) {
            builder.addDefaultShareMenuItem()
        }
        if (!TextUtils.isEmpty(transition)) {
            addTransition(builder, transition)
        }

        val customTabsIntent = builder.build()
        val packageName = CustomTabsHelper.getPackageNameToUse(cordova.activity)
        if (packageName != null) {
            customTabsIntent.intent.`package` = packageName
        }

        startCustomTabActivity(url, customTabsIntent.intent)
    }

    private fun addTransition(builder: CustomTabsIntent.Builder, transition: String) {
        val animType = "anim"
        when (transition) {
            "slide" -> {
                startAnimationBundle = ActivityOptionsCompat.makeCustomAnimation(
                    cordova.activity,
                    getIdentifier("slide_in_right", animType),
                    getIdentifier("slide_out_left", animType)
                ).toBundle()
                builder.setExitAnimations(
                    cordova.activity,
                    getIdentifier("slide_in_left", animType),
                    getIdentifier("slide_out_right", animType)
                )
            }

            else -> {
                startAnimationBundle = ActivityOptionsCompat.makeCustomAnimation(
                    cordova.activity,
                    getIdentifier("slide_in_right", animType),
                    getIdentifier("slide_out_left", animType)
                ).toBundle()
                builder.setExitAnimations(
                    cordova.activity,
                    getIdentifier("slide_in_left", animType),
                    getIdentifier("slide_out_right", animType)
                )
            }
        }
    }

    private fun startCustomTabActivity(url: String, intent: Intent) {
        intent.data = Uri.parse(url)
        if (startAnimationBundle == null) {
            cordova.startActivityForResult(this, intent, CUSTOM_TAB_REQUEST_CODE)
        } else {
            cordova.setActivityResultCallback(this)
            ActivityCompat.startActivityForResult(
                cordova.activity,
                intent,
                CUSTOM_TAB_REQUEST_CODE,
                startAnimationBundle
            )
        }
    }

    private fun warmUp(): Boolean {
        val client = customTabPluginHelper.clientInstance
        return client?.warmup(0) ?: false
    }

    private fun mayLaunchUrl(url: String): Boolean {
        if (customTabPluginHelper.clientInstance != null) {
            val session: CustomTabsSession? = session
            return session?.mayLaunchUrl(Uri.parse(url), null, null) ?: false
        }
        return false
    }

    private fun getColor(color: String?): Int {
        if (TextUtils.isEmpty(color)) return Color.LTGRAY
        return try {
            Color.parseColor(color)
        } catch (ex: IllegalArgumentException) {
            Log.i(TAG, String.format("Unable to parse Color: %s", color))
            Color.LTGRAY
        }
    }

    private val session: CustomTabsSession?
        get() = customTabPluginHelper.session

    private fun bindCustomTabsService(): Boolean =
        customTabPluginHelper.bindCustomTabsService(cordova.activity)

    private fun unbindCustomTabsService(): Boolean =
        customTabPluginHelper.unbindCustomTabsService(cordova.activity)

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CUSTOM_TAB_REQUEST_CODE) {
            val result = JSONObject()
            try {
                result.put("event", "closed")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            callbackContextRef?.let {
                it.success(result)
                callbackContextRef = null
            }
        }
    }

    private fun getIdentifier(name: String, type: String): Int {
        val activity: Activity = cordova.activity
        return activity.resources.getIdentifier(name, type, activity.packageName)
    }

    override fun onStop() {
        wasConnected = unbindCustomTabsService()
        super.onStop()
    }

    override fun onStart() {
        if (wasConnected) {
            bindCustomTabsService()
        }
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        customTabPluginHelper.setConnectionCallback(null)
    }
}
