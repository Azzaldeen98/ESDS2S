package com.example.esds2s.Helpers;

import static java.sql.DriverManager.println;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses;
import com.example.esds2s.R;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {


    public static boolean isUrl(String text) {
        String urlRegex = "^(https?|ftp):\\/\\/[\\w\\d\\-]+(\\.[\\w\\d\\-]+)+[\\w\\d\\-.,@?^=%&:/~+#]*[\\w\\d\\-@?^=%&/~+#]$";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public static boolean isLocalAudioFile(String filePath) {
        return isHasAudioExtensionFile(filePath);
    }
    public static boolean isHasAudioExtensionFile(String filePath) {

        String[] audioExtensions = {".mp3", ".wav", ".flac", ".aac", ".ogg","mp3"};
        for (String extension : audioExtensions) {
            if (filePath.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return  false;
    }
    public static boolean isAudioFile(String filePath) {

        if(!isUrl(filePath))
            return false;
       return isHasAudioExtensionFile(filePath);
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
    public static  void  deleteFile(String filePath) {
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

    public static void audioFocusRequest(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioFocusRequest  focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setOnAudioFocusChangeListener(focusChange -> {
                        // Handle focus change
                    }).build();

            audioManager.requestAudioFocus(focusRequest);
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


    public static String removeStars(String input) {
            return input.replaceAll("[*]", "");
    }
    public static int getDefaultSoundResource() {
       return getDefaultSoundResource(-1);
    }
    public static int responseIsNull() {
        return getDefaultSoundResource(-1);
    }
    public static int getDefaultSoundResource( Context context) {


       return getDefaultSoundResource(-1);
    }
    public static int getDefaultSoundResource(int soundNum) {

        int randomValues = new Random().nextInt(TypesOfVoiceResponses.values().length);
        int sound = R.raw.row11;

        Log.d("getDefaultSoundResource",randomValues+"");
        if(soundNum>-1 && soundNum < TypesOfVoiceResponses.values().length)
            randomValues=soundNum;

//        randomValues+=6;
        switch (randomValues) {
//            case 0:
//                sound = com.example.esds2s.R.raw.res1;
//                break;
//            case 1:
//                sound = com.example.esds2s.R.raw.res2;
//                break;
//            case 2:
//                sound = com.example.esds2s.R.raw.res4;
//                break;
//            case 3:
//                sound = com.example.esds2s.R.raw.res5;
//                break;
//            case 4:
//                sound = com.example.esds2s.R.raw.res6;
//                break;
//            case 5:
//                sound = com.example.esds2s.R.raw.res7;
//                break;
            case 0:
                sound = R.raw.mus1;
                break;
            case 1:
                sound = R.raw.mus2;
                break;
            case 2:
                sound = R.raw.mus3;
                break;
            case 3:
                sound = R.raw.mus4;
                break;


//            case 5:
//                sound = com.example.esds2s.R.raw.res7;
//                break;


            default:
                return  -1;
        }

        return sound;
    }

    public static int getDefaultSoundResource2(int soundNum) {


        int randomValues = new Random().nextInt(TypesOfVoiceResponses.values().length);
        int sound = R.raw.row11;

        Log.d("getDefaultSoundResource",randomValues+"");
        if(soundNum>-1 && soundNum < TypesOfVoiceResponses.values().length)
            randomValues=soundNum;

        switch (randomValues) {

            case 0:
                sound = R.raw.mus1;
                break;
            case 1:
                sound = R.raw.mus2;
                break;
            case 2:
                sound = R.raw.mus3;
                break;
            case 3:
                sound = R.raw.mus4;
                break;

            default:
                return   sound = R.raw.mus1;
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
