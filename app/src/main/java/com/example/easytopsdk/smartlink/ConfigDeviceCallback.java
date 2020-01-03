package com.example.easytopsdk.smartlink;




public interface ConfigDeviceCallback {

    void onStartConfig(int type);

    void onProgressConfig(int type, String msg);

    void onSuccessConfig(int type, HardwareCmd[] hardwareCmds);

    void onFailConfig(int type, String msg);
}
