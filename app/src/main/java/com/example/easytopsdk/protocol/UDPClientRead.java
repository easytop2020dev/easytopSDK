/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.protocol;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UDPClientRead extends Thread {
    private Context mContext;
    private int mPort;
    private UDPReadCallback mCallback;
    private boolean isOne2One = true;

    public UDPClientRead(Context context, int port, UDPReadCallback callback) {
        this(context, port, callback, true);
    }

    public UDPClientRead(Context context, int port, UDPReadCallback callback, boolean isOne2One) {
        mContext = context;
        mPort = port;
        mCallback = callback;
        this.isOne2One = isOne2One;
    }


    @Override
    public void run() {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(null);
            ds.setReuseAddress(true);
            ds.setBroadcast(true);
            ds.bind(new InetSocketAddress(mPort));
            //ds = new DatagramSocket(mPort);
            while (true) {
                byte[] b = new byte[1024];
                DatagramPacket dp = new DatagramPacket(b, b.length);
                ds.receive(dp);
                String deviceIP = dp.getAddress().getHostAddress();
                String wifiIP = getWifiIP(mContext);
                if (TextUtils.isEmpty(wifiIP)) {
                    sleep(5000);
                    wifiIP = getWifiIP(mContext);
                }
                String message = new String(dp.getData(), 0, dp.getLength(), "utf-8").trim();
                if (TextUtils.equals(deviceIP.trim(), wifiIP.trim())||deviceIP.equals("192.168.0.1")) {
                    continue;
                } else {
                        if (mCallback != null) {
                            mCallback.onEnd(new UDPData(deviceIP, mPort, message));
                            Log.i("test====","onEnd");

                        }
                        if (isOne2One) {
                            sleep(100000);
                            break;
                        } else {
                            sleep(10);
                        }
                }
            }
            if (ds.isConnected()) {
                ds.close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
            if (mCallback != null) {
                mCallback.onError();
            }

        } finally {
            if (ds != null && ds.isConnected()) {
                ds.disconnect();
                ds = null;
            }
        }
    }


    public  String getWifiIP(Context context) {
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

    public  String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }
}
