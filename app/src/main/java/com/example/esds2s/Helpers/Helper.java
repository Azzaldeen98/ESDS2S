package com.example.esds2s.Helpers;

import static java.sql.DriverManager.println;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import java.util.List;


import java.io.File;

public class Helper {

public static  void  deleteFile(String filePath)
{
        try {

            File file =new File(filePath);
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    println("File deleted successfully");
                } else {
                    println("Failed to delete file");
                }
            } else {
                println("File does not exist");
            }
        }catch (Exception e)
        {
            Log.e("Error",e.getMessage().toString());
        }
    }

    public static boolean isRecordServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {

                    return true;
                }
            }
        }

        return false;
    }

}
