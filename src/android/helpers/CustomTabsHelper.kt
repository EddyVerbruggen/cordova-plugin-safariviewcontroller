package org.chromium.customtabsclient.shared

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.text.TextUtils
import android.util.Log

/**
 * Helper class for Custom Tabs.
 */
class CustomTabsHelper private constructor() {
    companion object {
        private const val TAG = "CustomTabsHelper"
        internal const val STABLE_PACKAGE = "com.android.chrome"
        internal const val BETA_PACKAGE = "com.chrome.beta"
        internal const val DEV_PACKAGE = "com.chrome.dev"
        internal const val LOCAL_PACKAGE = "com.google.android.apps.chrome"
        private const val EXTRA_CUSTOM_TABS_KEEP_ALIVE =
            "android.support.customtabs.extra.KEEP_ALIVE"
        private const val ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService"

        private val activityIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))

        private var packageNameToUse: String? = null

        /**
         * Goes through all apps that handle VIEW intents and have a warmup service. Picks
         * the one chosen by the user if there is one, otherwise makes a best effort to return a
         * valid package name.
         *
         * This is **not** threadsafe.
         *
         * @param context [Context] to use for accessing [PackageManager].
         * @return The package name recommended to use for connecting to custom tabs related components.
         */
        @JvmStatic
        fun getPackageNameToUse(context: Context): String? {
            if (packageNameToUse != null) return packageNameToUse

            val pm = context.packageManager
            val defaultViewHandlerPackageName = getDefaultViewHandlerPackageName(context)
            val packagesSupportingCustomTabs = getPackagesSupportingCustomTabs(context)

            packageNameToUse = when {
                packagesSupportingCustomTabs.isEmpty() -> null
                packagesSupportingCustomTabs.size == 1 -> packagesSupportingCustomTabs[0]
                !TextUtils.isEmpty(defaultViewHandlerPackageName) &&
                    !hasSpecializedHandlerIntents(context, activityIntent) &&
                    packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName) ->
                    defaultViewHandlerPackageName
                packagesSupportingCustomTabs.contains(STABLE_PACKAGE) -> STABLE_PACKAGE
                packagesSupportingCustomTabs.contains(BETA_PACKAGE) -> BETA_PACKAGE
                packagesSupportingCustomTabs.contains(DEV_PACKAGE) -> DEV_PACKAGE
                packagesSupportingCustomTabs.contains(LOCAL_PACKAGE) -> LOCAL_PACKAGE
                else -> null
            }

            return packageNameToUse
        }

        @JvmStatic
        fun getDefaultViewHandlerPackageName(context: Context): String? {
            val pm = context.packageManager
            val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
            return defaultViewHandlerInfo?.activityInfo?.packageName
        }

        @JvmStatic
        fun getPackagesSupportingCustomTabs(context: Context): List<String> {
            val pm = context.packageManager
            val resolvedActivityList = pm.queryIntentActivities(
                activityIntent,
                PackageManager.MATCH_ALL
            )
            val packagesSupportingCustomTabs = ArrayList<String>()
            for (info in resolvedActivityList) {
                val serviceIntent = Intent().apply {
                    action = ACTION_CUSTOM_TABS_CONNECTION
                    `package` = info.activityInfo.packageName
                }
                if (pm.resolveService(serviceIntent, 0) != null) {
                    packagesSupportingCustomTabs.add(info.activityInfo.packageName)
                }
            }
            return packagesSupportingCustomTabs
        }

        /**
         * Used to check whether there is a specialized handler for a given intent.
         * @param intent The intent to check with.
         * @return Whether there is a specialized handler for the given intent.
         */
        private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
            return try {
                val pm = context.packageManager
                val handlers = pm.queryIntentActivities(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER
                )
                if (handlers == null || handlers.isEmpty()) {
                    false
                } else {
                    for (resolveInfo in handlers) {
                        val filter = resolveInfo.filter ?: continue
                        if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
                        if (resolveInfo.activityInfo == null) continue
                        return true
                    }
                    false
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime exception while getting specialized handlers")
                false
            }
        }

        /**
         * @return All possible chrome package names that provide custom tabs feature.
         */
        @JvmStatic
        fun getPackages(): Array<String> {
            return arrayOf("", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE)
        }

        @JvmStatic
        @Throws(InvalidPackageException::class)
        fun setPackageNameToUse(packageName: String, context: Context) {
            if (getPackagesSupportingCustomTabs(context).contains(packageName)) {
                packageNameToUse = packageName
            } else {
                throw InvalidPackageException(packageName)
            }
        }
    }

    class InvalidPackageException(packageName: String) : Exception(
        "$packageName has no Custom Tabs support."
    )
}
