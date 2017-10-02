package com.example.webview_bluetooth_gps;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import static com.example.webview_bluetooth_gps.App.appInstance;

public class WebAppInterface {
    FullscreenActivity mContext;


    WebAppInterface(FullscreenActivity c) {
        mContext = c;
    }


    @JavascriptInterface
    public void selectGPSDevice() {
        mContext.selectGPSDevice();
    }

    @JavascriptInterface
    public String getPositions() {
        return App.appInstance.getPositions().toString();
    }

}
