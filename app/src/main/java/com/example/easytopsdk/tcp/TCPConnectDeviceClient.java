/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.tcp;

import android.text.TextUtils;


import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lijunjie on 15/12/11.
 */
public abstract class TCPConnectDeviceClient extends TCPClient {

    private TCPConnectCallback mCallback;
    private String mDeviceId;
    private String mDeviceType;
    volatile private Status mStatus = Status.PENDING;
    private Timer mTimer;
    private Timer mOKTimer;

    public TCPConnectDeviceClient(String ip, int port, TCPConnectCallback callback) {
        super(ip, port);
        mCallback = callback;
        setCallback(mTCPClientCallback);
    }

    private TCPClientCallback mTCPClientCallback = new TCPClientCallback() {
        @Override
        public void onTCPError() {
            if (!mStatus.equals(Status.PENDING)) {
                mStatus = Status.PENDING;
                mCallback.onTCPConnectError();
            }
        }

        @Override
        public void onTCPConnected() {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
            mStatus = Status.CONNECTED;
            mCallback.onTCPConnected2();
            if (mOKTimer != null) {
                mOKTimer.cancel();
                mOKTimer.purge();
                mOKTimer = null;
            }
            mOKTimer = new Timer();
            startOKTimeOutTimer();
        }

        /*
        《关于在AP配置下收到DeviceId后延迟1秒的说明》
          2016年9月1日，在测试部门测试过程中发现有偶现AP切不到设备热点的bug，
          经查，此问题是APP在收到DeviceId后，快速回复SSID和Password给模块，
          模块有偶现uip_send返回-2的错误，-2的Root cause不详，导致APP收不到OK。
          基于此，APP在收到DeviceId后，延迟1秒回复SSID和Password，打孔逻辑待后人优化。
         */
        @Override
        public void onTCPReveived(byte[] result) {
            String msg = new String(result);
            if (msg.contains(getDeviceIdRequestKey())) {// 获取设备ID
                mDeviceId = msg.substring(1, 17);
                try {
                    Thread.sleep(1000);

                    String configInfo = getConfigInfo(mDeviceId);
                    mDeviceType = msg.substring(msg.lastIndexOf("/") + 1);

                    //直联模式收到deviceId后不需要发送ssid和pasword直接成功
                    if (TextUtils.isEmpty(configInfo)) {
                        mCallback.onTCPConfigEnd(mDeviceId + "," + mDeviceType.trim());
                        mStatus = Status.END;
                        disconnectTCP();
                        return;
                    }
                    sendMsg(configInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (msg.contains(getDeviceConfigSuccessRequestKey())) {// 读取配置完成后返回的OK
                mCallback.onTCPConfigEnd(mDeviceId + "," + mDeviceType.trim());
                mStatus = Status.END;
                disconnectTCP();
            } else if (TextUtils.isEmpty(msg)) {
                mCallback.onTCPConnectError();
            }
        }
    };

    private void startTimeOutTimer() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mStatus.equals(Status.CONNECTING)) {
                    disconnect();
                    mCallback.onTCPConnectError();
                }
            }
        }, 3000);
    }

    //如果TCP连接4秒后app还没收到OK，app默认成功
    private void startOKTimeOutTimer() {
        mOKTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mStatus.equals(Status.CONNECTED)) {
                    if (!TextUtils.isEmpty(mDeviceId) && !TextUtils.isEmpty(mDeviceType)) {
                        mCallback.onTCPConfigEnd(mDeviceId + "," + mDeviceType.trim());
                        mStatus = Status.END;
                        disconnectTCP();
                    }
                }
            }
        }, 4000);
    }

    public void disconnectTCP() {
        mStatus = Status.PENDING;
        if (mOKTimer != null) {
            mOKTimer.cancel();
            mOKTimer.purge();
        }
        super.disconnect();
    }

    public void connectTCP() {
        if (!mStatus.equals(Status.CONNECTING)) {
            mStatus = Status.CONNECTING;
            mTimer = new Timer();
            startTimeOutTimer();
            super.connect();
        }
    }

    public interface TCPConnectCallback {
        void onTCPConnected2();
        void onTCPConnectError();
        void onTCPConfigEnd(String data);

    }

    public abstract String getDeviceIdRequestKey();
    public abstract String getDeviceConfigSuccessRequestKey();
    public abstract String getConfigInfo(String deviceId);

    enum Status {
        PENDING,
        CONNECTING,
        CONNECTED,
        END
    }
}
