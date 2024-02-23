package com.example.esds2s.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ExternalStorage {


    private  static String storageName="LocalStorage";

    public  static String   getDefaultStorageName()
    {
        return storageName;
    }
    public  static boolean  existing(Context activity, String key)
    {
        SharedPreferences preferences =activity.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        Log.d("existing", preferences.contains(key)+"");
        return preferences.contains(key);
    }

    public  static Object getValue(Context activity,String key) {
      return   getValue(activity,storageName,key);
    }
    public  static Object getValue(Context activity,String storageName,String key)
    {
        SharedPreferences preferences = activity.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        if(preferences.contains(key))
            return  preferences.getString(key,null);
        return  null;
    }


    public  static boolean remove(Context activity,String key)
    {
        SharedPreferences preferences = activity.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if(preferences.contains(key))
        {
            editor.remove(key);
            return  editor.commit();
        }

        return  false;
    }

    public  static boolean storage(Context activity,String key ,String value) {

       return storage(activity,storageName,key,value);
    }

    public  static boolean storage(Context activity,String storageName,String key ,String value)
    {

        if(!key.isEmpty() && !value.isEmpty())
        {
            SharedPreferences preferences = activity.getSharedPreferences(storageName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            //if(!preferences.contains(key))
            {
                editor.putString(key, value);
                return editor.commit();
            }
        }

        return false;
    }


}
