package com.cordovaplugin;


import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* This class echoes a string called from JavaScript.
*/
public class ChromeCustomTabPlugin extends CordovaPlugin {

    public static final String TAG = "ChromeCustomTabPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("isAvailable")) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
            return true;
        } else if (action.equals("show")) {
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
        }
        return false;
    }

    private void show(String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(cordova.getActivity(), Uri.parse(url));
    }
}