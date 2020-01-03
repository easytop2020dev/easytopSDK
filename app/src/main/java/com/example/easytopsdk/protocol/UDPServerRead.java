/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.protocol;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class UDPServerRead extends Thread {

    private Selector mSelector;
    private UDPReadCallback mCallBack;
    private DatagramChannel mChannel;

    public UDPServerRead(Selector selector, UDPReadCallback callback) {
        this.mSelector = selector;
        this.mCallBack = callback;
    }

    @Override
    public void run() {
        try {
            while (mSelector.select() > 0) {
                Set<SelectionKey> keySet = mSelector.selectedKeys();
                for (final SelectionKey key : keySet) {
                    handle(key);
                }
                keySet.clear();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mCallBack.onError();
        }
    }

    private synchronized void handle(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isReadable()) {
            mChannel = (DatagramChannel) selectionKey.channel();
            mCallBack.onEnd(receiveData(mChannel));
        } else {
            throw new IOException("read error");
        }
    }

    private UDPData receiveData(DatagramChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        InetSocketAddress address = (InetSocketAddress) socketChannel.receive(buffer);
        buffer.flip();
        if (buffer.remaining() < 1) {
            return null;
        }
        byte b[] = new byte[buffer.remaining()];
        for (int i = 0; i < buffer.remaining(); i++) {
            b[i] = buffer.get(i);
        }
        return new UDPData(address.getAddress().getHostAddress(), address.getPort(), new String(b, "UTF-8").trim());
    }
}
