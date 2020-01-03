package com.example.easytopsdk.smartlink;
import android.content.Context;
import com.example.easytopsdk.Constans;

/**
 * @author duxin.
 * @date 2020-01-02.
 * email：
 * description：
 */
public class ConfigDevicesFactory {

    public static ConfigDeviceTask getTask(Context context, int mode, ConfigDeviceCallback callback){
        ConfigDeviceTask task = null;
        switch (mode){

            case Constans.SMART_AIRKISS:

                task = new AirKissConfigTask(context,callback);

                break;

            case Constans.SMART_AIRKISS_FACTORY:

                task = new AirKissMultiConfigTask(context,callback);

                break;

            case Constans.AP_CONFIG:

                task = new APConfigDeviceTask(context,callback);

                break;

        }
        return task.setTaskType(mode);

    }

}
