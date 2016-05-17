package com.customtabsplugin;


import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.chromium.customtabsclient.shared.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* This class echoes a string called from JavaScript.
*/
public class ChromeCustomTabPlugin extends CordovaPlugin implements ServiceConnectionCallback {

    public static final String TAG = "ChromeCustomTabPlugin";
    public static final int CUSTOM_TAB_REQUEST_CODE = 1;

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;
    private String mPackageNameToBind;
    private boolean wasConnected;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mPackageNameToBind = CustomTabsHelper.getPackageNameToUse(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) {
            case "isAvailable":
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isAvailable()));
                return true;

            case "show":
                final JSONObject options = args.getJSONObject(0);
                final String url = options.getString("url");

                PluginResult pluginResult;
                JSONObject result = new JSONObject();
                try {
                    this.show(url);
                    result.put("event", "loaded");
                    pluginResult = new PluginResult(PluginResult.Status.OK, result);
                } catch (Exception ex) {
                    result.put("error", ex.getMessage());
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
                }
                callbackContext.sendPluginResult(pluginResult);
                return true;

            case "connectToService":
                bindCustomTabsService();
                if (mConnection != null)
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                else
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Failed to connect to service"));
                return true;

            case "warmUp":
                if (warmUp()) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Failed to warm up service"));
                }
                return true;
        }
        return false;
    }

    private boolean isAvailable(){
        return !TextUtils.isEmpty(mPackageNameToBind);
    }

    private void show(String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(getSession()).build();
        Intent intent = customTabsIntent.intent;
        intent.setData(Uri.parse(url));
        cordova.startActivityForResult(this, intent, CUSTOM_TAB_REQUEST_CODE);
    }

    private boolean warmUp(){
        boolean success = false;
        if (mClient != null) success = mClient.warmup(0);
        return success;
    }

    private CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(new CustomTabsCallback());
        }
        return mCustomTabsSession;
    }

    private void bindCustomTabsService() {
        if (mClient != null || !isAvailable()) return;

        mConnection = new ServiceConnection(this);
        boolean ok = CustomTabsClient.bindCustomTabsService(cordova.getActivity(), mPackageNameToBind, mConnection);
        if(!ok) {
            mConnection = null;
        }
    }

    private void unbindCustomTabsService() {
        if (mConnection == null) return;
        cordova.getActivity().unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, String.format("requestCode: %d, resultCode: %d.", requestCode, resultCode));
    }

    @Override
    public void onServiceConnected(CustomTabsClient client) {
        mClient = client;
    }

    @Override
    public void onServiceDisconnected() {
        mClient = null;
    }

    @Override
    public void onPause(boolean multitasking) {
        if(mClient != null){
            wasConnected = true;
            unbindCustomTabsService();
        }
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        if(wasConnected){
            bindCustomTabsService();
        }
        super.onResume(multitasking);
    }

    @Override
    public void onDestroy() {
        unbindCustomTabsService();
        super.onDestroy();
    }
}