package com.example.test.wifi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;

public class WifiManagerProxy implements IWifiManager {
    private static class SingletonHolder {
        private static final WifiManagerProxy INSTANCE = new WifiManagerProxy();
    }

    private WifiManagerProxy() {
    }

    public static WifiManagerProxy get() {
        return SingletonHolder.INSTANCE;
    }


    private WifiManager manager;
    private WifiConnector mConnector = new WifiConnector();

    private void checkInit() {
        if (manager == null) {
            throw new IllegalArgumentException("You must call init()  before other methods!");
        }
    }


    @Override
    public void init(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Application cant be null!");
        }
        if (manager == null) {
            manager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
            mConnector.init(manager);
        }
    }

    @Override
    public void openWifi() {
        checkInit();
        if (!isWifiEnabled()) {
            manager.setWifiEnabled(true);
        }
    }

    @Override
    public void openWifiSettingPage(Activity activity) {
        checkInit();
        if (activity == null) {
            return;
        }
        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    @Override
    public void closeWifi() {
        checkInit();
        if (isWifiEnabled()) {
            manager.setWifiEnabled(false);
        }
    }


    @Override
    public boolean isWifiEnabled() {
        checkInit();
        return manager.isWifiEnabled();
    }

    @Override
    public void connect(String ssId, String pwd, IWifiConnectListener listener) {
        checkInit();
        if (listener == null) {
            throw new IllegalArgumentException(" IWifiConnectListener cant be null !");
        }
        mConnector.connect(ssId, pwd, WifiConnector.WifiCipherType.WIFICIPHER_WPA, listener);
    }

    @Override
    public void disConnect(String ssId, IWifiDisConnectListener listener) {
        checkInit();
        if (listener == null) {
            throw new IllegalArgumentException(" IWifiDisConnectListener cant be null !");
        }
        if (TextUtils.isEmpty(ssId)) {
            listener.onDisConnectFail(" WIFI名称不能为空! ");
            return;
        }
        ssId = "\"" + ssId + "\"";
        WifiInfo wifiInfo = manager.getConnectionInfo();
        if (wifiInfo != null && !TextUtils.isEmpty(ssId) && TextUtils.equals(ssId, wifiInfo.getSSID())) {
            int netId = wifiInfo.getNetworkId();
            manager.disableNetwork(netId);
            listener.onDisConnectSuccess();
        } else {
            listener.onDisConnectFail(" wifi状态异常 或者 此时就没有连接上对应的WIFI ！ ");
        }
    }

}
