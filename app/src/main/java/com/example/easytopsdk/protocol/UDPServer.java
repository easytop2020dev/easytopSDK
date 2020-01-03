/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.protocol;


import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Created by lijunjie on 15/12/11.
 */
public class UDPServer implements UDPReadCallback {

    private Selector mSelector;
    private DatagramChannel mSocketChannel;
    private int mHostListenningPort;
    private UDPServerRead mUDPServerRead;
    private UDPServerCallback mCallback;

    public UDPServer(int port) {
        mHostListenningPort = port;
    }

    public void setCallback(UDPServerCallback callback) {
        mCallback = callback;
    }

    public void connect() {
        try {
            mSocketChannel = DatagramChannel.open();
            mSocketChannel.configureBlocking(false);
            mSocketChannel.socket().setSoTimeout(2000);
            mSocketChannel.socket().bind(new InetSocketAddress(mHostListenningPort));
            mSelector = Selector.open();
            mSocketChannel.register(mSelector, SelectionKey.OP_READ);
            mUDPServerRead = new UDPServerRead(mSelector, this);
            mUDPServerRead.start();
        } catch (Exception e) {
            mCallback.onUDPError();
        }
    }

    public void disconnect() {
        try {
            mSocketChannel.socket().close();
            mSocketChannel.close();
            if (mUDPServerRead != null) {
                mUDPServerRead.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError() {
        mCallback.onUDPError();
    }

    @Override
    public void onEnd(UDPData data) {
        mCallback.onUDPEnd(data);
    }

    @Override
    public void onPortError(int port) {
    }

    public interface UDPServerCallback {
        void onUDPError();
        void onUDPEnd(UDPData data);
    }
}
