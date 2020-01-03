/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.protocol;

/**
 * Created by lijunjie on 15/12/13.
 */
public interface UDPReadCallback {

    void onError();
    void onEnd(UDPData data);
    void onPortError(int port);
}
