
package com.example.easytopsdk;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AndroidUtil {

    public static boolean isMiui8NougatVersion() {
        String miui = getSystemProperty("ro.miui.ui.version.name");
        if (TextUtils.isEmpty(miui)) {
            return false;
        }

        miui = miui.trim().toLowerCase();
        int start = miui.indexOf("v");
        if (start < 0)
            return false;

        try {
            boolean miui8 = Integer.valueOf(miui.substring(1).trim()) >= 8;
            return miui8 && (Build.VERSION.SDK_INT >= 24);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }




    public static boolean isEmui3Version() {
        String emui = getSystemProperty("ro.build.version.emui");
        if (TextUtils.isEmpty(emui)) {
            return false;
        }

        emui = emui.trim().toLowerCase();

        if (!emui.contains("emotionui_") && emui.length() > 10)
            return false;
        int start = emui.indexOf("_");
        try {
            if (emui.contains(".")) {
                int firstPointIndex = emui.indexOf(".");
                return Integer.valueOf(emui.substring(start + 1, firstPointIndex)) < 4;
            } else {
                return Integer.valueOf(emui.substring(start + 1)) < 4;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }




    public static String getWifiName(Context context) {
        String ssid = "";
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiinfo = wifiManager.getConnectionInfo();
        if (wifiinfo == null) {
            return ssid;
        }
        String router = wifiinfo.getSSID();
        if (TextUtils.isEmpty(router))
            return ssid;
        if ("0.0.0.0".equals(getWifiIP(context))) {
            return ssid;
        }
        if (!"<unknown ssid>".equals(router) && !"0X".equals(router) && !"0x".equals(router))
            ssid = router.replace("\"", "");
        return ssid;
    }

    public static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    public static String getWifiIP(Context context) {
        String ip = null;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiinfo = wifiManager.getConnectionInfo();
        if (wifiinfo != null) {
            int ipAddress = wifiinfo.getIpAddress();
            ip = intToIp(ipAddress);
        }
        return ip;
    }




    public static boolean isEmui4Version() {
        String emui = getSystemProperty("ro.build.version.emui");
        if (TextUtils.isEmpty(emui)) {
            return false;
        }

        emui = emui.trim().toLowerCase();
        if (!emui.contains("emotionui_") && emui.length() > 10)
            return false;
        int start = emui.indexOf("_");
        try {
            if (emui.contains(".")) {
                int firstPointIndex = emui.indexOf(".");
                return Integer.valueOf(emui.substring(start + 1, firstPointIndex)) > 3;
            } else {
                return Integer.valueOf(emui.substring(start + 1)) > 3;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }





























}
