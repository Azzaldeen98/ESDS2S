package com.example.esds2s.Helpers;

import static java.sql.DriverManager.println;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.esds2s.ContentApp.ContentApp;
import com.example.esds2s.R;

import java.util.List;


import java.io.File;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;

public class Helper {


    public static boolean isUrl(String text) {
        String urlRegex = "^(https?|ftp):\\/\\/[\\w\\d\\-]+(\\.[\\w\\d\\-]+)+[\\w\\d\\-.,@?^=%&:/~+#]*[\\w\\d\\-@?^=%&/~+#]$";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public static boolean isAudioFile(String filePath) {

        if(!isUrl(filePath))
            return false;

        String[] audioExtensions = {".mp3", ".wav", ".flac", ".aac", ".ogg"};
        for (String extension : audioExtensions) {
            if (filePath.toLowerCase().endsWith(extension)) {
                return true;
            }
        }

        return false;
    }



public static  void  showAlertDialog(Context context)
{
//    AlertDialog.Builder(this@CreateNewChatFragment.context)
//                        .setTitle("Alert")
//        .setIcon(R.drawable.baseline_warning_24)
//        .setMessage(getString(R.string.msg_failed_the_session_create))
//        .setPositiveButton(getString(R.string.btn_ok)) { dialog, which -> }
//                        .create()
//        .show()
}
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

    public static int getDefaultSoundResource() {
        int randomValues = new Random().nextInt(7);
        int sound = com.example.esds2s.R.raw.res1;

        switch (randomValues) {
            case 0:
                sound = com.example.esds2s.R.raw.res1;
                break;
            case 1:
                sound = com.example.esds2s.R.raw.res2;
                break;
            case 2:
                sound = com.example.esds2s.R.raw.res4;
                break;
            case 3:
                sound = com.example.esds2s.R.raw.res5;
                break;
            case 4:
                sound = com.example.esds2s.R.raw.res6;
                break;
            case 5:
                sound = com.example.esds2s.R.raw.res7;
                break;
        }

        return sound;
    }

    public   static void LoadFragment(Fragment fragment, FragmentManager supportFragmentManager,int fragment_container)
    {
        LoadFragment(fragment,supportFragmentManager,fragment_container,null );
    }
    public   static void LoadFragment(Fragment fragment, FragmentManager supportFragmentManager,int fragment_container_id,String addToBackStack){
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.replace(fragment_container_id,fragment);
        transaction.addToBackStack(addToBackStack);
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
        setAnimateAlphaForTool(_view,1500);
    }
    public static void setAnimateAlphaForTool(View _view,int time) {
        //Set button alpha to zero
        _view.setAlpha(0f);
        _view.setTranslationY(200f);
        _view.animate().alpha(1f).translationYBy(-200f).setDuration(time);
    }

}
