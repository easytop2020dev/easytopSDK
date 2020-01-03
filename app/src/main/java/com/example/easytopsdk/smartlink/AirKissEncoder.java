package com.example.easytopsdk.smartlink;

import java.util.Arrays;


public class AirKissEncoder {
    private int mEncodedData[] = new int[2 << 14];
    private int mLength = 0;

    // Random char should be in range [0, 127).
    public AirKissEncoder(char random, String ssid, String password) {
        //构建锁屏数据
        leadingPart();

        //构建magicCode
        for (int a = 0; a < 10; a++) {
            magicCode(ssid, password);
        }

        //构建prefixCode
        for (int a = 0; a < 4; a++) {
            prefixCode(password);
        }

        //构建sequenceData
        for (int j = 0; j < 10; j++) {
            byte[] data3 = new byte[password.length() + 1 + ssid.getBytes().length];
            System.arraycopy(password.getBytes(), 0, data3, 0, password.length());
            data3[password.length()] = (byte) random;
            System.arraycopy(ssid.getBytes(), 0, data3, password.length() + 1, ssid.getBytes().length);

            int index;
            byte content[] = new byte[4];
            for (index = 0; index < data3.length / 4; ++index) {
                System.arraycopy(data3, index * 4, content, 0, content.length);
                sequence(index, content);
            }

            if (data3.length % 4 != 0) {
                content = new byte[data3.length % 4];
                System.arraycopy(data3, index * 4, content, 0, content.length);
                sequence(index, content);
            }
        }
    }

    public int[] getEncodedData() {
        return Arrays.copyOf(mEncodedData, mLength);
    }

    private void appendEncodedData(int length) {
        mEncodedData[mLength++] = length;
    }

    private int CRC8(byte data[]) {
        int len = data.length;
        int i = 0;
        byte crc = 0x00;
        while (len-- > 0) {
            byte extract = data[i++];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) ((sum & 0xFF) & 0x01);
                crc = (byte) ((crc & 0xFF) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xFF) ^ 0x8C);
                }
                extract = (byte) ((extract & 0xFF) >>> 1);
            }
        }
        return (crc & 0xFF);
    }

    private int CRC8(String stringData) {
        return CRC8(stringData.getBytes());
    }

    private void leadingPart() {
        for (int i = 0; i < 100; ++i) {
            for (int j = 1; j <= 4; ++j)
                appendEncodedData(j);
        }
    }

    private void magicCode(String ssid, String password) {
        int length = ssid.getBytes().length + password.length() + 1;
        int magicCode[] = new int[4];
        magicCode[0] = 0x00 | (length >>> 4 & 0xF);
        if (magicCode[0] == 0)
            magicCode[0] = 0x08;
        magicCode[1] = 0x10 | (length & 0xF);
        int crc8 = CRC8(ssid);
        magicCode[2] = 0x20 | (crc8 >>> 4 & 0xF);
        magicCode[3] = 0x30 | (crc8 & 0xF);
        for (int j = 0; j < 4; ++j)
            appendEncodedData(magicCode[j]);
    }

    private void prefixCode(String password) {
        int length = password.length();
        int prefixCode[] = new int[4];
        prefixCode[0] = 0x40 | (length >>> 4 & 0xF);
        prefixCode[1] = 0x50 | (length & 0xF);
        int crc8 = CRC8(new byte[]{(byte) length});
        prefixCode[2] = 0x60 | (crc8 >>> 4 & 0xF);
        prefixCode[3] = 0x70 | (crc8 & 0xF);
        for (int j = 0; j < 4; ++j)
            appendEncodedData(prefixCode[j]);
    }

    private void sequence(int index, byte data[]) {
        byte content[] = new byte[data.length + 1];
        content[0] = (byte) (index & 0xFF);
        System.arraycopy(data, 0, content, 1, data.length);
        int crc8 = CRC8(content);

        int sequence[] = new int[data.length + 2];
        sequence[0] = 0x80 | crc8;
        sequence[1] = 0x80 | index;

        for (int i = 0; i < data.length; i++) {
            sequence[2 + i] = 0x100 | (data[i] & 0xFF);
        }

        for (int a : sequence) {
            appendEncodedData(a);
        }
    }
}
