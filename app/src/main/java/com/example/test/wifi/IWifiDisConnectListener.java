package com.example.test.wifi;

public interface IWifiDisConnectListener {
    //断开成功
    void onDisConnectSuccess();

    //断开失败
    void onDisConnectFail(String errorMsg);
}
