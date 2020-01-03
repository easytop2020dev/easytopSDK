package com.example.easytopsdk.protocol;

/**
 * udp广播返回数据接口
 */
public interface OnUdpDataReceivedListener {
    void onUdpDataReceived(final int api, String fromIp, String result);
    void onUdpDataError(final int api, final Throwable error);
    void onUdpTimeout();
}
