/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.tcp;

/**
 * Created by Luogical on 16/1/15.
 */

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.example.easytopsdk.AndroidUtil;

import java.util.Iterator;
import java.util.List;

public class WifiAutoConnectManager {

    private static final String TAG = "WifiAutoConnectManager";

    WifiManager wifiManager;

    // 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    // 构造函数
    public WifiAutoConnectManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    // 提供一个外部接口，传入要连接的无线网
    public void connect(String ssid, String password, WifiCipherType type) {
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type));
        thread.start();
    }

    public boolean connect(WifiConfiguration configuration) {
        wifiManager.enableNetwork(configuration.networkId, true);
        return wifiManager.reconnect();
    }

    // 查看以前是否也配置过这个网络
    public WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        try {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        // nopass
        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        // wep
        if (Type == WifiCipherType.WIFICIPHER_WEP) {
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
        }
        // wpa
        if (Type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    // 打开wifi功能
    private boolean openWifi() {
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
            // 打开wifi
            openWifi();
            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
            // 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
            while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    // 为了避免程序一直while循环，休息100毫秒检测⋯⋯
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }

            int netID = 0;
            WifiConfiguration tempConfig = isExsits(ssid);

            List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();

            //MIUI8.0安卓7.0版本不能disable
            if (!AndroidUtil.isMiui8NougatVersion()) {
                if (networks != null && !networks.isEmpty()) {
                    Iterator<WifiConfiguration> iterator = networks.iterator();
                    while (iterator.hasNext()) {
                        WifiConfiguration wifiConfig = iterator.next();
                        wifiManager.disableNetwork(wifiConfig.networkId);
                    }
                }
            }

            if (tempConfig != null) {
                //华为EMUI3.X系统作特殊处理
                if (AndroidUtil.isEmui3Version()) {
                    wifiManager.removeNetwork(tempConfig.networkId);
                    WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                    netID = wifiManager.addNetwork(wifiConfig);
                } else if (AndroidUtil.isMiui8NougatVersion()) {
                    WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                    netID = wifiManager.addNetwork(wifiConfig);
                } else if (Build.VERSION.SDK_INT >= 21) {
                    if (isPSK(tempConfig)) {
                        tempConfig.preSharedKey = "\"" + password + "\"";
                    } else if (TextUtils.isEmpty(password)) {
                        tempConfig.wepKeys[0] = "";
                    } else if (isHex(password)) {
                        tempConfig.wepKeys[0] = password;
                    } else {
                        //tempConfig.wepKeys[0] = "\"" + password + "\"";
                        tempConfig.preSharedKey = "\"" + password + "\"";
                    }
                    wifiManager.updateNetwork(tempConfig);
                    netID = tempConfig.networkId;
                } else {
                    wifiManager.removeNetwork(tempConfig.networkId);
                    WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                    netID = wifiManager.addNetwork(wifiConfig);
                }
            } else {
                WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                netID = wifiManager.addNetwork(wifiConfig);
            }

            boolean enabled = wifiManager.enableNetwork(netID, true);
            Log.d(TAG, "enableNetwork status enable=" + enabled);
            boolean connected = wifiManager.reconnect();
            Log.d(TAG, "enableNetwork connected=" + connected);
        }
    }

    public static boolean isPSK(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return true;
        }
        return false;
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }
}
