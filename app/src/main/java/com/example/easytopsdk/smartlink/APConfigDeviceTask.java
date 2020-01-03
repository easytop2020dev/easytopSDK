/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.smartlink;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.example.easytopsdk.AndroidUtil;
import com.example.easytopsdk.Constans;
import com.example.easytopsdk.tcp.TCPConnectDeviceClient;
import com.example.easytopsdk.tcp.WifiAutoConnectManager;


public class APConfigDeviceTask extends ConfigDeviceTask implements TCPConnectDeviceClient.TCPConnectCallback {

    private WifiManager mWifiManager;

    private String mDeviceSSID, mAPSSID, mAPPasswd, mAPNetworkId;

    private Object mLock = new Object();
    private WifiAutoConnectManager mAutoWifiConnectManager;
    //直连模式
    private boolean isDirectConnection;
    //TCP成功标志位
    private boolean isTcpSuccess;

    public APConfigDeviceTask(Context context , ConfigDeviceCallback callback) {
        super(context, callback);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mAutoWifiConnectManager = new WifiAutoConnectManager(mWifiManager);
    }

    @Override
    protected Void doInBackground(String... params) {
        ConnectWakeLock.acquireWakeLock(mContext);
        publishProgress(String.valueOf(STEP_START));
        mDeviceSSID = params[0];
        mAPSSID = params[1];
        mAPPasswd = params[2];
        mAPNetworkId = params[3];
        isTcpSuccess = false;
        isConfigSuccess = false;
        if (TextUtils.isEmpty(mAPSSID) && TextUtils.isEmpty(mAPPasswd)) {
            isDirectConnection = true;
        }
        connect();
        ConnectWakeLock.releaseWakeLock();
        return null;
    }

    private void holdTask() {
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private void connect() {
        while (!isCancelled()) {
            boolean isConnectSuccess = false;
            try {

                if (!connectDeviceAP()) {
                    throw new Exception("Connect AP fail");
                }

                if (isCancelled()) {
                    break;
                }

                if (!checkConnectDeviceAPSuccess()) {
                    throw new Exception("Check AP fail");
                }

                if (isCancelled()) {
                    break;
                }

                if (!checkPingDeviceSuccess()) {
                    throw new Exception("Ping AP fail");
                }

                if (isCancelled()) {
                    break;
                }
                isConnectSuccess = true;

            } catch (Exception e) {
                e.printStackTrace();
                isConnectSuccess = false;
            }

            if (isConnectSuccess) {
                sleep(5000);
                connectTCP();
                holdTask();
            }

            if (!isTcpSuccess) {
                sleep(Constans.THREAD_SLEEP_TIMELONG);
            } else {
                break;
            }
        }

        if (isCancelled()) {
            return;
        }

        //还没成功的需要连接到路由器热点
        if (!isConfigSuccess) {
            while (!isCancelled()) {
                connectTargetWifi();
                sleep(5000);
                String current = AndroidUtil.getWifiName(mContext);
                if (mAPSSID.equals(current)) {
                    isConfigSuccess = true;
                    publishProgress(String.valueOf(STEP_SUCCESS));
                    finish();
                    break;
                }
            }
        }
    }

    private void tcpSuccess() {
        if (isCancelled() || getStatus().equals(Status.FINISHED)) {
            return;
        }
        //直联模式收到deviceId后不需要切换到原来AP直接成功
        if (isDirectConnection) {
            isConfigSuccess = true;
        }
        isTcpSuccess = true;
        releaseLock();
    }

    private void tcpFail() {
        if (isCancelled() || getStatus().equals(Status.FINISHED)) {
            return;
        }
        isTcpSuccess = false;
        releaseLock();
    }

    private void releaseLock() {
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    private void connectTCP() {
        mTCPConnectDeviceClient.connectTCP();
    }

    @Override
    public void finish() {
        cancel(true);
        releaseLock();
    }

    private boolean connectDeviceAP() {
        WifiConfiguration wc = mAutoWifiConnectManager.isExsits(mDeviceSSID);

        //如果当前连接的AP就是设备AP不再连接
        String current = AndroidUtil.getWifiName(mContext);
        if (mDeviceSSID.equals(current)) {
            return true;
        }

        if (AndroidUtil.isEmui4Version() && wc != null) {
            String current2 = AndroidUtil.getWifiName(mContext);
            if (!mDeviceSSID.equals(current2)) {
                WifiConfiguration cu = mAutoWifiConnectManager.isExsits(current2);
                if (cu != null) {
                    mWifiManager.disableNetwork(cu.networkId);
                }
            }
            mWifiManager.enableNetwork(wc.networkId, true);
            mWifiManager.reconnect();
            return true;
        }

        if (wc != null) {
            mWifiManager.removeNetwork(wc.networkId);
        }

        wc = mAutoWifiConnectManager.isExsits(mDeviceSSID);
        if (wc != null) {
            mWifiManager.removeNetwork(wc.networkId);
        }

        mAutoWifiConnectManager.connect(mDeviceSSID, "", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS);
        return true;
    }

    private boolean checkConnectDeviceAPSuccess(int... retry) {
        if (isCancelled()) {
            return false;
        }

        int leftRetryTimes = Constans.RETRY_TIMES_CHECK;

        if (retry != null && retry.length > 0) {
            leftRetryTimes = retry[0];
        }

        --leftRetryTimes;


        if (leftRetryTimes < 0) {
            return false;
        }
        String ssid = AndroidUtil.getWifiName(mContext);
        if (!ssid.contains(mDeviceSSID)) {
            sleep(Constans.THREAD_SLEEP_TIMELONG);
            return checkConnectDeviceAPSuccess(leftRetryTimes);
        }
        return true;
    }

    private boolean checkPingDeviceSuccess(int... retry) {
        if (isCancelled()) {
            return false;
        }
        int leftRetryTimes = Constans.RETRY_TIMES_CHECK;

        if (retry != null && retry.length > 0) {
            leftRetryTimes = retry[0];
        }

        --leftRetryTimes;


        if (leftRetryTimes < 0) {
            return false;
        }
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 " + Constans.DEVICE_TCP_IP);
            if (p.waitFor() != 0) {
                sleep(Constans.THREAD_SLEEP_TIMELONG);
                return checkPingDeviceSuccess(leftRetryTimes);
            } else {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    TCPConnectDeviceClient mTCPConnectDeviceClient = new TCPConnectDeviceClient(Constans.DEVICE_TCP_IP, Constans.DEVICE_TCP_PORT, this) {
        @Override
        public String getDeviceIdRequestKey() {
            return "/type/1";
        }

        @Override
        public String getDeviceConfigSuccessRequestKey() {
            return "/OK";
        }

        @Override
        public String getConfigInfo(String deviceId) {
            //直联模式收到deviceId后不需要发送ssid和pasword直接成功
            return isDirectConnection ? null : configInfo(deviceId, mAPSSID, mAPPasswd);
        }
    };

    public String configInfo(String deviceId, String ssid, String password) {
        StringBuffer info = new StringBuffer();
        info.append("/").append(deviceId).append("/")
                .append("S99").append("/").append("1")
                .append("/").append(ssid).append(",")
                .append(password).append("!");
        return info.toString();
    }

    private void sleep(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException e) {
            publishProgress(String.valueOf(STEP_FAIL), e.getMessage());
        }
    }

    private void connectTargetWifi() {
        WifiConfiguration wc = mAutoWifiConnectManager.isExsits(mAPSSID);

        //如果当前连接的AP就是路由器AP不再连接
        String current2 = AndroidUtil.getWifiName(mContext);
        if (mAPSSID.equals(current2)) {
            return;
        }

        if (AndroidUtil.isEmui4Version() && wc != null) {
            String current = AndroidUtil.getWifiName(mContext);
            if (!mAPSSID.equals(current)) {
                WifiConfiguration cu = mAutoWifiConnectManager.isExsits(current);
                if (cu != null) {
                    mWifiManager.disableNetwork(cu.networkId);
                }
            }
            mWifiManager.enableNetwork(wc.networkId, true);
            mWifiManager.reconnect();
            //注意这里没有返回
        }

        if (wc != null) {
            mWifiManager.removeNetwork(wc.networkId);
        }

        wc = mAutoWifiConnectManager.isExsits(mAPSSID);
        if (wc != null) {
            mWifiManager.removeNetwork(wc.networkId);
        }
        if (TextUtils.isEmpty(mAPPasswd)) {
            mAutoWifiConnectManager.connect(mAPSSID, "", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS);
        } else {
            WifiAutoConnectManager.WifiCipherType type = WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS;
            try {
                for (ScanResult result : mWifiManager.getScanResults()) {
                    if (mAPSSID.equals(result.SSID)) {
                        String capabilities = result.capabilities;
                        if (!TextUtils.isEmpty(capabilities)) {
                            if (capabilities.toUpperCase().contains("WPA")) {
                                type = WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA;
                            } else if (capabilities.toUpperCase().contains("WEP")) {
                                type = WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WEP;
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
            }

            mAutoWifiConnectManager.connect(mAPSSID, mAPPasswd, type);
        }
    }

    @Override
    public void onTCPConnected2() {
    }

    @Override
    public void onTCPConnectError() {
        tcpFail();
    }

    @Override
    public void onTCPConfigEnd(String id) {
        mHardwareCmds.add(getHardwareCmd(String.format("/%s/%s/%s/%s", id.split(",")[0],"type", "1", id.split(",")[1])));
        tcpSuccess();
    }



}
