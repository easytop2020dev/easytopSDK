/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.smartlink;

import android.text.TextUtils;

/**
 * Created by lijunjie on 16/1/10.
 */
public abstract class OptionCode {

    public static final int STATUS_ON_OFF_LINE                       = -1;
    public static final int STATUS_ALL                               = 0;
    public static final int STATUS_WORK                               = 4;
    public static final int STATUS_ERROR                             = -100;
    public static final int STATUS_UNKNOWN                           = -2;

    /*
    @param S00
    @return 0
     */
    public static int getOptionCode(String stringCode) {
        if (TextUtils.isEmpty(stringCode)) {
            return STATUS_UNKNOWN;
        }
        try {
            return Integer.valueOf(stringCode.substring(1));
        } catch (Exception e) {
            if (TextUtils.equals("SEE", stringCode.toUpperCase())) {
                return STATUS_ERROR;
            }
        }

        return STATUS_UNKNOWN;
    }

    /*
    @param 0
    @return S00
     */
    public static String getOptionCode(int optionCode) {
        return String.format("S%02d", optionCode);
    }

    /*
    @param 0
    @return B00
    */
    public static String get6120OptionCode(int optionCode) {
        return String.format("B%02d", optionCode);
    }
}
