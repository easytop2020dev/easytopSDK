/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.tcp;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lijunjie on 15/12/11.
 */
public class TCPClient implements TCPReadCallback {

    private Selector mSelector;
    private SocketChannel mSocketChannel;
    private String mHostIp;
    private int mHostListenningPort;
    private TCPClientRead mTCPClientRead;
    private TCPClientCallback mCallback;

    volatile private Status mStatus = Status.PENDING;
    private Timer mTimer;
    private TimerTask mConnectTimeoutTimerTask;

    public TCPClient(String ip, int port) {
        mHostIp = ip;
        mHostListenningPort = port;
        mTimer = new Timer();
    }

    public void setCallback(TCPClientCallback callback) {
        mCallback = callback;
    }

    public void connect(final int connectTimeout) {
        if (!mStatus.equals(Status.PENDING)) {
            return;
        }
        try {
            cancelConnectTimeoutTask();
            mConnectTimeoutTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (mStatus.equals(Status.CONNECTING)) {
                        disconnect();
                        mCallback.onTCPError();
                    }
                }
            };
            mTimer.schedule(mConnectTimeoutTimerTask, connectTimeout);
            mSocketChannel = SocketChannel.open();
            mSocketChannel.configureBlocking(false);
            mSocketChannel.socket().setSoTimeout(2000);
            mSelector = Selector.open();
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
            mSocketChannel.connect(new InetSocketAddress(mHostIp, mHostListenningPort));
            mStatus = Status.CONNECTING;
            mTCPClientRead = new TCPClientRead(mSelector, this);
            mTCPClientRead.start();
        } catch (Exception e) {
            e.printStackTrace();
            mCallback.onTCPError();
        }

    }

    private void cancelConnectTimeoutTask() {
        if (mConnectTimeoutTimerTask != null) {
            mConnectTimeoutTimerTask.cancel();
            mTimer.purge();
            mConnectTimeoutTimerTask = null;
        }
    }

    public void connect() {
        connect(3000);
    }

    public void sendMsg(ByteBuffer message) {
        try {
            mSocketChannel.write(message);
        } catch (Exception e) {
            e.printStackTrace();
            mCallback.onTCPError();
        }
    }

    /**
     * 每条message结尾自动加上\n
     * @param msg
     */
    public void sendMsg(String msg) {
        msg = msg.trim() + '\n';
        sendMsg(ByteBuffer.wrap(msg.getBytes()));
    }

    public void disconnect() {
        cancelConnectTimeoutTask();
        mStatus = Status.PENDING;
        try {
            mSocketChannel.socket().close();
            mSocketChannel.close();
            if (mTCPClientRead != null) {
                mTCPClientRead.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface TCPClientCallback {
        void onTCPError();

        void onTCPConnected();

        void onTCPReveived(byte[] result);
    }

    @Override
    public void onReveived(byte[] data) {
        mStatus = Status.END;
        mCallback.onTCPReveived(data);
    }

    @Override
    public void onConnected() {
        cancelConnectTimeoutTask();
        mStatus = Status.CONNECTED;
        mCallback.onTCPConnected();
    }

    @Override
    public void onError() {
        cancelConnectTimeoutTask();
        mStatus = Status.PENDING;
        mCallback.onTCPError();
    }

    enum Status {
        PENDING,
        CONNECTING,
        CONNECTED,
        END
    }
}
