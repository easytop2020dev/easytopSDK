/*
 * Copyright (c) 2017 Guangdong Scinan IoT, Inc.
 *
 * This software is the property of Guangdong Scinan IoT, Inc.
 * You have to accept the terms in the license file before use.
 */

package com.example.easytopsdk.smartlink;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

public abstract class ConfigDeviceTask extends AsyncTask<String, String, Void> {

    private int taskType;

    static final int STEP_START                            = 0x30;
    static final int STEP_PROGRESS                         = 0x31;
    static final int STEP_SUCCESS                          = 0x32;
    static final int STEP_FAIL                             = 0x33;

    Context mContext;
    ConfigDeviceCallback mConfigDeviceCallback;
    ArrayList<HardwareCmd> mHardwareCmds;

    //全局成功标志位(目前只有在AP配置里面，全局成功和TCP成功不是一回事，其他都是一回事)
    boolean isConfigSuccess;

    public ConfigDeviceTask(Context context, ConfigDeviceCallback callback) {
        this.mContext = context;
        this.mConfigDeviceCallback = callback;
        this.mHardwareCmds = new ArrayList<>();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (!getStatus().equals(Status.RUNNING)) {
            return;
        }

        switch (Integer.valueOf(values[0])) {
            case STEP_FAIL:
                 if (mConfigDeviceCallback != null) {
                    mConfigDeviceCallback.onFailConfig(getTaskType(), values[1]);
                }
                break;
            case STEP_START:
                if (mConfigDeviceCallback != null) {
                    mConfigDeviceCallback.onStartConfig(getTaskType());
                }
                break;
            case STEP_PROGRESS:
                if (mConfigDeviceCallback != null) {
                    mConfigDeviceCallback.onProgressConfig(getTaskType(), values[1]);
                }
                break;
            case STEP_SUCCESS:
                if (mConfigDeviceCallback != null) {
                    HardwareCmd[] arrays = new HardwareCmd[mHardwareCmds.size()];
                    mHardwareCmds.toArray(arrays);
                    mConfigDeviceCallback.onSuccessConfig(getTaskType(), arrays);

                    Log.i("test====","STEP_SUCCESS");

                }
                finish();
                break;
            default:

        }
    }

    String getOldVersionResponse() {
        if (mHardwareCmds.size() == 1) {
            return mHardwareCmds.get(0).deviceId + "," + mHardwareCmds.get(0).data;
        } else if (mHardwareCmds.size() > 1) {
            StringBuffer sb = new StringBuffer();
            for (HardwareCmd cmd : mHardwareCmds) {
                sb.append(cmd.deviceId);
                sb.append(",");
                sb.append(cmd.data);
                sb.append(",");
            }
            return sb.toString();
        }
        return "";
    }

    String getKeywords() {
      return "/type/1";
    }

    protected void logD(String msg) {
        publishProgress(String.valueOf(STEP_PROGRESS), msg);
    }

    protected void logE(Throwable throwable) {
        publishProgress(String.valueOf(STEP_PROGRESS), throwable.getMessage());
    }

    protected HardwareCmd getHardwareCmd(String fullData) {
        if (fullData.endsWith("/")) {
            fullData = fullData + "1";
        }

        HardwareCmd cmd = HardwareCmd.parse(fullData);
        if (cmd == null) {
            cmd = new HardwareCmd(fullData.split("/")[1], "type", "1", fullData.substring(fullData.lastIndexOf("/") + 1).trim());
        }
        return cmd;
    }

    public int getTaskType() {
        return taskType;
    }

    public ConfigDeviceTask setTaskType(int taskType) {
        this.taskType = taskType;
        return this;
    }

    public abstract void finish();
}
