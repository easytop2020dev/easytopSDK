/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.protocol;

import android.content.Context;
import android.text.TextUtils;


import com.example.easytopsdk.Constans;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

public class UDPClient implements UDPReadCallback {

    private Context mContext;
    private UDPClientRead mReadThread;
    private UDPClientCallback mCallback;
    private MultiBroadcastThread mMultiBroadcastThread;
    private String mBroadcastData;
    private volatile long mFirstConfigTime;
    boolean isInited;
    private int mPortUDP;
    private boolean isOne2One ;

    public UDPClient(Context context, String broadcastData) {
        this(context, broadcastData, true);
    }

    public UDPClient(Context context, String broadcastData, boolean isOne2One) {
        mContext = context;
        mBroadcastData = broadcastData;
        isInited = false;
        this.isOne2One = isOne2One;
    }

    public void connect(){
        mPortUDP = getRandomPort();
        mReadThread = new UDPClientRead(mContext, mPortUDP, this, isOne2One);
        mReadThread.start();
        mMultiBroadcastThread = new MultiBroadcastThread();
        mMultiBroadcastThread.start();
    }

    private int getRandomPort() {
        int port = 10001 + new Random().nextInt(25534);
        return port;
    }

    public boolean isConnect() {
        return mMultiBroadcastThread != null;
    }

    public void disconnect() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }

        if (mMultiBroadcastThread != null) {
            mMultiBroadcastThread.finish();
            mMultiBroadcastThread = null;
        }
    }

    private void sendMultiBroadcast() {
        String host = "255.255.255.255";
        String data = TextUtils.isEmpty(mBroadcastData) ? "S0000" : mBroadcastData;
        try {
            InetAddress ip = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data.getBytes(),
                    data.length(), ip, Constans.DEVICE_UDP_PORT);
            InetAddress group = InetAddress.getByName(Constans.DEVICE_UDP_GROUP_IP);
            //随机端口发送
            MulticastSocket ms1 = new MulticastSocket();
            ms1.setLoopbackMode(true);
            ms1.joinGroup(group);
            ms1.send(packet);
            ms1.close();
            //指定端口发送
            MulticastSocket ms2 = new MulticastSocket(mPortUDP);
            ms2.setLoopbackMode(true);
            ms2.joinGroup(group);
            ms2.send(packet);
            ms2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MultiBroadcastThread extends Thread {

        boolean isFinish = false;

        void finish() {
            isFinish = true;
        }
        @Override
        public void run() {
            log("udp MultiBroadcastThread start");
            while (!isFinish) {
                if (!isInited) {
                    isInited = true;
                    mFirstConfigTime = System.currentTimeMillis();
                }
                sendMultiBroadcast();
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    log("udp MultiBroadcastThread die");
                    break;
                }
            }
        }
    }


    //UDP读取失败
    @Override
    public void onError() {
        if (mCallback != null) {
            mCallback.onUDPError();
        }
        if (mMultiBroadcastThread != null)
            mMultiBroadcastThread.interrupt();
    }

    @Override
    public void onEnd(UDPData data) {
        log("send broadcast time is  = " + (System.currentTimeMillis() - mFirstConfigTime));
        if (mCallback != null) {
            mCallback.onUDPEnd(data);
        }
        if (isOne2One) {
            if (mMultiBroadcastThread != null) {
                mMultiBroadcastThread.interrupt();
            }
        }
    }



    @Override
    public void onPortError(int port) {
        mReadThread.interrupt();
        mReadThread = new UDPClientRead(mContext, port, this);
        mReadThread.start();
    }


    private void log(String log) {
    }

    public interface UDPClientCallback {
        void onUDPError();
        void onUDPEnd(UDPData data);
    }

    public void setCallback(UDPClientCallback callback) {
        mCallback = callback;
    }


}
