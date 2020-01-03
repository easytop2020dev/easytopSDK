/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.tcp;

/**
 * Created by lijunjie on 15/12/12.
 */
public interface TCPReadCallback {
    void onError();
    void onReveived(byte[] data);
    void onConnected();
}
