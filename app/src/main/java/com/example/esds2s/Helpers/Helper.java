package com.example.esds2s.Helpers;

import static java.sql.DriverManager.println;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.esds2s.R;

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

    public   static void LoadFragment(Fragment fragment, FragmentManager supportFragmentManager,int fragment_container_id){
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.replace(fragment_container_id,fragment);
        transaction.commit();
    }
    public static void setEditTextError(EditText editText, String message)
    {
        if(editText!=null)
        {
            editText.setError(message);
            editText.requestFocus();
        }
    }

    public static void setAnimateAlphaForTool(View _view) {
        //Set button alpha to zero
        _view.setAlpha(0f);
        _view.setTranslationY(200f);
        _view.animate().alpha(1f).translationYBy(-200f).setDuration(1500);
    }

}
