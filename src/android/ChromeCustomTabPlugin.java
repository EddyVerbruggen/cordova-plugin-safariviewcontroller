package com.customtabplugin;


import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChromeCustomTabPlugin extends CordovaPlugin{

    public static final String TAG = "ChromeCustomTabPlugin";
    public static final int CUSTOM_TAB_REQUEST_CODE = 1;

    private CustomTabServiceHelper mCustomTabPluginHelper;
    private boolean wasConnected;
    private  CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mCustomTabPluginHelper = new CustomTabServiceHelper(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        switch (action) {
            case "isAvailable":
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, mCustomTabPluginHelper.isAvailable()));
                return true;

            case "show": {
                final JSONObject options = args.getJSONObject(0);
                final String url = options.getString("url");
                final String toolbarColor = options.optString("toolbarColor");

                PluginResult pluginResult;
                JSONObject result = new JSONObject();
                if(isAvailable()) {
                    try {
                        this.show(url, getColor(toolbarColor));
                        result.put("event", "loaded");
                        pluginResult = new PluginResult(PluginResult.Status.OK, result);
                        pluginResult.setKeepCallback(true);
                        this.callbackContext = callbackContext;
                    } catch (Exception ex) {
                        result.put("error", ex.getMessage());
                        pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
                    }
                } else {
                    result.put("error", "custom tabs are not available");
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
                }
                callbackContext.sendPluginResult(pluginResult);
                return true;
            }
            case "connectToService": {
                if (bindCustomTabsService())
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                else
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Failed to connect to service"));
                return true;
            }
            case "warmUp": {
                if (warmUp()) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Failed to warm up service"));
                }
                return true;
            }
            case "mayLaunchUrl": {
                final String url = args.getString(0);
                if(mayLaunchUrl(url)){
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,String.format("Failed prepare to launch url: %s", url)));
                }
                return true;
            }
        }
        return false;
    }

    private boolean isAvailable(){
        return mCustomTabPluginHelper.isAvailable();
    }

    private void show(String url, @ColorInt int toolbarColor) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(getSession())
                .setToolbarColor(toolbarColor)
                .build();

        startCustomTabActivity(url, customTabsIntent.intent);
    }

    private void startCustomTabActivity(String url, Intent intent) {
        intent.setData(Uri.parse(url));
        cordova.startActivityForResult(this, intent, CUSTOM_TAB_REQUEST_CODE);
    }

    private boolean warmUp(){
        boolean success = false;
        final CustomTabsClient client = mCustomTabPluginHelper.getClient();
        if (client != null) success = client.warmup(0);
        return success;
    }

    private boolean mayLaunchUrl(String url){
        boolean success = false;
        if (mCustomTabPluginHelper.getClient() != null) {
            CustomTabsSession session = getSession();
            success = session.mayLaunchUrl(Uri.parse(url), null, null);
        }

        return success;
    }

    private int getColor(String color) {
        if(TextUtils.isEmpty(color)) return Color.LTGRAY;

        try {
            return Color.parseColor(color);
        } catch (NumberFormatException ex) {
            Log.i(TAG, String.format("Unable to parse Color: %s", color));
            return Color.LTGRAY;
        }
    }

    private CustomTabsSession getSession() {
        return mCustomTabPluginHelper.getSession();
    }

    private boolean bindCustomTabsService() {
        return mCustomTabPluginHelper.bindCustomTabsService(cordova.getActivity());
    }

    private boolean unbindCustomTabsService() {
        return mCustomTabPluginHelper.unbindCustomTabsService(cordova.getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == CUSTOM_TAB_REQUEST_CODE){
            JSONObject result = new JSONObject();
            try {
                result.put("event", "closed");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
            if(callbackContext != null){
                callbackContext.success(result);
                callbackContext = null;
            }
        }
    }

    @Override
    public void onStop() {
        wasConnected = unbindCustomTabsService();
        super.onStop();
    }

    @Override
    public void onStart() {
        if(wasConnected){
            bindCustomTabsService();
        }
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCustomTabPluginHelper.setConnectionCallback(null);
    }
}
