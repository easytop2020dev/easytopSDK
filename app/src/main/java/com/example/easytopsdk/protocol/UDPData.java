package com.example.easytopsdk.protocol;

import java.io.Serializable;

/**
 * Created by lijunjie on 17/3/3.
 */

public class UDPData implements Serializable {

    private String data;
    private String ip;
    private int port;

    public UDPData(String ip, int port, String data) {
        this.ip = ip;
        this.port = port;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "address is "+ ip + ":" + port + ",data is " + data;
    }
}
