/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.smartlink;

import java.io.Serializable;

/**
 * @author xindu
 * {device 设备操作实例}
 */
public class HardwareCmd implements Serializable {
    public String deviceId;
    public String optionCodeString;
    public String data;
    public int optionCode;
    public String sensorType;
    public String ip;
    public String deviceType;

    public HardwareCmd(String deviceId, String optionCode, String sensorType, String data, String deviceType) {
        this.deviceId = deviceId;
        this.optionCodeString = optionCode;
        this.data = data;
        this.optionCode = OptionCode.getOptionCode(optionCode);
        this.sensorType = sensorType;
        this.deviceType = deviceType;
    }

    public HardwareCmd(String deviceId, int optionCode, String sensorType, String data, String deviceType) {
        this(deviceId, OptionCode.getOptionCode(optionCode), sensorType, data,deviceType);
    }

    public HardwareCmd(String deviceId, int optionCode, String data, String deviceType) {
        this(deviceId, optionCode, "1", data,deviceType);
    }

    public HardwareCmd(String deviceId, String optionCode, String data, String deviceType) {
        this(deviceId, optionCode, "1", data,deviceType);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public static HardwareCmd parse(String fullCmdString) {
        try {
            String[] list = fullCmdString.split("/",-1);
            String deviceId = list[1];
            try {
                String optionCodeString = list[2];
                String sensorType = list[3];
                String data = list[4];
                String deviceType = data.substring(4,6);
                return new HardwareCmd(deviceId, optionCodeString, sensorType, data,deviceType);
            } catch (Exception e) {
                return new HardwareCmd(deviceId, null, null,null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("/%s/%s/%s/%s", deviceId, optionCodeString, sensorType, data);
    }
}
