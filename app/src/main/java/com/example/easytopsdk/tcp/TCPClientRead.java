/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.tcp;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Created by lijunjie on 15/12/11.
 */
public class TCPClientRead extends Thread {

    private Selector mSelector;
    private TCPReadCallback mCallBack;
    private SocketChannel mChannel;

    public TCPClientRead(Selector selector, TCPReadCallback callback) {
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
                };
                keySet.clear();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mCallBack.onError();
        }
    }

    private synchronized void handle(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isConnectable()) {
            mChannel = (SocketChannel) selectionKey.channel();
            if (mChannel.isConnectionPending()) {
                mChannel.finishConnect();
                mCallBack.onConnected();
            }
            mChannel.register(mSelector, SelectionKey.OP_READ);
        } else if (selectionKey.isReadable()) {
            mChannel = (SocketChannel) selectionKey.channel();
            byte[] data = receiveData(mChannel);
            if (data.length < 0) {
                throw new IOException("read empty error");
            } else {
            }
            mCallBack.onReveived(data);
        } else {
            throw new IOException("read error");
        }
    }

    private byte[] receiveData(SocketChannel socketChannel) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try {
            byte[] bytes;
            int size = 0;
            while ((size = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                bytes = new byte[size];
                buffer.get(bytes);
                baos.write(bytes);
                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch(Exception ex) {}
        }
        return baos.toByteArray();
    }
}
