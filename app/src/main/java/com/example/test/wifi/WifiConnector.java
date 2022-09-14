package com.example.test.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.util.List;

public class WifiConnector {
    private IWifiConnectListener iWifiConnectListener;
    private WifiManager wifiManager;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (iWifiConnectListener != null) {
                switch (msg.what) {
                    case 0:
                        iWifiConnectListener.onConnectSuccess();
                        break;
                    case -1:
                        iWifiConnectListener.onConnectFail(" fail = " + msg.obj);
                        break;
                    default:
                        break;
                }
            }
        }
    };


    public void init(WifiManager wifiManager) {
        if (wifiManager == null) {
            throw new IllegalArgumentException("WifiConnector wifiManager cant be null!");
        }
        this.wifiManager = wifiManager;
    }


    private void checkInit() {
        if (wifiManager == null) {
            throw new IllegalArgumentException("You must call init()  before other methods!");
        }
        if (iWifiConnectListener == null) {
            throw new IllegalArgumentException("IWifiConnectListener cant be null!");
        }
    }


    /**
     * 子线程要向UI发送连接失败的消息
     *
     * @param info 消息
     */
    public void sendErrorMsg(String info) {
        if (mHandler != null) {
            Message msg = new Message();
            msg.obj = info;
            msg.what = -1;
            mHandler.sendMessage(msg);// 向Handler发送消息
        }
    }


    /**
     * 子线程向UI主线程发送连接成功的消息
     *
     * @param info
     */
    public void sendSuccessMsg(String info) {
        if (mHandler != null) {
            Message msg = new Message();
            msg.obj = info;
            msg.what = 0;
            mHandler.sendMessage(msg);// 向Handler发送消息
        }
    }

    //WIFICIPHER_WEP是WEP ，WIFICIPHER_WPA是WPA，WIFICIPHER_NOPASS没有密码
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }


    // 提供一个外部接口，传入要连接的无线网
    public void connect(String ssid, String password, WifiCipherType type, IWifiConnectListener listener) {
        this.iWifiConnectListener = listener;
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type));
        thread.start();
    }

    // 查看以前是否也配置过这个网络
    public WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    public WifiConfiguration createWifiInfo(String SSID, String Password, WifiCipherType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        // config.SSID = SSID;
        // nopass
        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            // config.wepTxKeyIndex = 0;
        } else if (Type == WifiCipherType.WIFICIPHER_WEP) {// wep
            if (!TextUtils.isEmpty(Password)) {
                if (isHexWepKey(Password)) {
                    config.wepKeys[0] = Password;
                } else {
                    config.wepKeys[0] = "\"" + Password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type == WifiCipherType.WIFICIPHER_WPA) {// wpa
            config.preSharedKey = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    // 打开wifi功能
    private boolean openWifi() {
        checkInit();
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    class ConnectRunnable implements Runnable {
        private String ssid;

        private String password;

        private WifiCipherType type;

        public ConnectRunnable(String ssid, String password, WifiCipherType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }

        @Override
        public void run() {
            checkInit();
            try {
                // 如果之前没打开wifi,就去打开  确保wifi开关开了
                openWifi();
                iWifiConnectListener.onConnectStart();
                //开启wifi需要等系统wifi刷新1秒的时间
                Thread.sleep(1000);

                // 如果wifi没开启的话就提示错误
                if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                    sendErrorMsg("WIFI 未开启");
                    return;
                }

                // 开启wifi之后开始扫描附近的wifi列表
                wifiManager.startScan();
                Thread.sleep(500);
                boolean hasSsIdWifi = false;
                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (int i = 0; i < scanResults.size(); i++) {
                    ScanResult scanResult = scanResults.get(i);
                    if (TextUtils.equals(scanResult.SSID, ssid)) {
                        hasSsIdWifi = true;
                        break;
                    }
                }
                // 如果就没这个wifi的话直接返回
                if (!hasSsIdWifi) {
                    sendErrorMsg("当前不存在指定的Wifi!");
                    return;
                }


                //禁掉所有wifi
                for (WifiConfiguration c : wifiManager.getConfiguredNetworks()) {
                    wifiManager.disableNetwork(c.networkId);
                }

                //看看当前wifi之前配置过没有
                boolean enabled = false;
                WifiConfiguration tempConfig = isExsits(ssid);
                if (tempConfig != null) {
                    enabled = wifiManager.enableNetwork(tempConfig.networkId, true);
                } else {
                    WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                    if (wifiConfig == null) {
                        sendErrorMsg("wifiConfig is null!");
                        return;
                    }

                    int netID = wifiManager.addNetwork(wifiConfig);
                    enabled = wifiManager.enableNetwork(netID, true);
                }

                if (enabled) {
                    sendSuccessMsg("连接成功! enabled = " + enabled);
                } else {
                    sendErrorMsg("连接失败! enabled = false");
                }

            } catch (Exception e) {
                sendErrorMsg(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

}
