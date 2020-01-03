/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.smartlink;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.example.easytopsdk.protocol.UDPClient;
import com.example.easytopsdk.protocol.UDPData;
import com.example.easytopsdk.protocol.UDPServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class AirKissConfigTask extends ConfigDeviceTask implements UDPClient.UDPClientCallback {

    private Object mLock = new Object();
    private String mAPSSID, mAPPasswd;
    Thread sendUdpThread;
    InetAddress address;
    Random rand = new Random();
    StringBuffer ipData;
    String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private UDPClient mUDP;
    private UDPServer mUDPServer;
    private String TAG =  AirKissConfigTask.class.getName();
    private boolean stopSendPassword;

    AirKissEncoder airKissEncoder;
    char mRandomStr;
    String ip;
    StringBuffer sb;

    volatile boolean sbSended;

    public AirKissConfigTask(Context context, ConfigDeviceCallback callback) {
        super(context, callback);
    }

    @Override
    public Void doInBackground(String... params) {
        ConnectWakeLock.acquireWakeLock(mContext);
        publishProgress(String.valueOf(STEP_START));
        mAPSSID = params[1];
        mAPPasswd = params[2];
        stopSendPassword = false;
        sbSended = false;
        ip = null;
        sb = new StringBuffer("SMNT_0");
        mRandomStr = AB.charAt(rand.nextInt(AB.length()));
        mHandler.sendEmptyMessage(0);
        Log.i("AirKissConfigTask1","random=="+mRandomStr);
        holdTask();
        ConnectWakeLock.releaseWakeLock();
        return null;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (checkWifiConnected()) {
                        enableThread();
                    } else {
                        publishProgress();
                    }
                    break;
                case 1:
                    stopSendPassword = true;
                    break;
            }
        }
    };

    boolean checkWifiConnected() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getIpAddress() == 0)
            return false;
        savePhoneIp(wifiInfo.getIpAddress());
        return true;
    }

    void savePhoneIp(int ipAddress) {
        ipData = new StringBuffer();
        ipData.append((char) (ipAddress & 0xff));
        ipData.append((char) (ipAddress >> 8 & 0xff));
        ipData.append((char) (ipAddress >> 16 & 0xff));
        ipData.append((char) (ipAddress >> 24 & 0xff));
    }

    @Override
    public void finish() {
        cancel(true);
        if (sendUdpThread != null) {
            sendUdpThread.interrupt();
            sendUdpThread = null;
        }

        if (mUDP != null) {
            mUDP.disconnect();
        }

        if (mUDPServer != null) {
            mUDPServer.disconnect();
        }

        mHandler.removeMessages(0);
        mHandler.removeMessages(1);
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    private void holdTask() {
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void onUDPError() {
        publishProgress(String.valueOf(STEP_FAIL), "onUDPError");
    }

    @Override
    public void onUDPEnd(UDPData udpData) {
        try {
            if (TextUtils.isEmpty(ip)) {
                return;
            }
            if (TextUtils.equals(udpData.getIp(), ip)) {
                mHardwareCmds.add(getHardwareCmd(udpData.getData()));
                publishProgress(String.valueOf(STEP_SUCCESS));
                Log.i("UDPClientRead","publishProgress");

            } else {
            }
        } catch (Exception e) {
            logE(e);
            e.printStackTrace();
        }
    }



    public class sendUdpThread extends Thread {

        public void run() {
            while (!isCancelled() && !stopSendPassword) {
                try {
                    SendbroadCast();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void enableThread() {
        if (sendUdpThread == null) {
            sendUdpThread = new sendUdpThread();
            sendUdpThread.start();
        }

        mUDPServer = new UDPServer(10000);
        mUDPServer.setCallback(new UDPServer.UDPServerCallback() {
            @Override
            public void onUDPError() {
//              mUDPServer.disconnect();
                Log.i(TAG,"onUDPError");
            }

            @Override
            public void onUDPEnd(UDPData data) {

                String random = data.getData();
                if (!TextUtils.isEmpty(random) && random.length() > 0) {
                    random = random.substring(0, 1);
                } else {
                    random = "";
                }
                Log.i("AirKissConfigTask1","onUDPEnd_random=="+random);
                Log.i("AirKissConfigTask1","onUDPEnd_mRandomStr=="+mRandomStr);


                if (TextUtils.equals(random, String.valueOf(mRandomStr)) && !sbSended) {
                    mHandler.sendEmptyMessage(1);
                    sbSended = true;
                    ip = data.getIp();
                    mUDP = new UDPClient(mContext,sb.toString(),false );
                    mUDP.setCallback(AirKissConfigTask.this);
                    mUDP.connect();
                }
            }
        });
        mUDPServer.connect();

    }

    public void SendbroadCast() {
        if (airKissEncoder == null) {
            airKissEncoder = new AirKissEncoder(mRandomStr, mAPSSID, mAPPasswd);
        }

        for (int i = 0; i < airKissEncoder.getEncodedData().length; i++) {
            AtomicReference<StringBuffer> sendPacketSeq = new AtomicReference<>(new StringBuffer());
            for (int j = 0; j < airKissEncoder.getEncodedData()[i]+18; j++) {
                sendPacketSeq.get().append(AB.charAt(rand.nextInt(AB.length())));
            }

            //如果需要暂停发送密码就退出发送
            if (stopSendPassword) {
                break;
            }

            try {
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setBroadcast(true);
                address = InetAddress.getByName("255.255.255.255");
                DatagramPacket sendPacketSeqSocket = new DatagramPacket(sendPacketSeq.get().toString().getBytes(), sendPacketSeq.get().toString().length(), address, 8300);
                clientSocket.send(sendPacketSeqSocket);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                clientSocket.close();
                if (isCancelled())
                    return;
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isCancelled()) {
                break;
            }
        }
    }
}
