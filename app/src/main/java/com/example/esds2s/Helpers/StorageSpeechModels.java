package com.example.esds2s.Helpers;

import android.content.Context;
import android.util.Log;

import com.example.esds2s.ContentApp.ContentApp;
import com.example.esds2s.Models.SpeechModelInfo;

public class StorageSpeechModels {

    public static void setModel(Context context, String selectedModel, int selectedModelIndex) {
        ExternalStorage.storage(context, ContentApp.SPEECHMODELSELECTED, selectedModel);
        ExternalStorage.storage(context, ContentApp.SPEECHMODELSELECTEDINDEX, selectedModelIndex);
    }

    public static SpeechModelInfo getModel(Context context) {

        if (ExternalStorage.existing(context, ContentApp.SPEECHMODELSELECTED) && ExternalStorage.existing(context, ContentApp.SPEECHMODELSELECTEDINDEX)) {

            Object value = ExternalStorage.getValue(context, ContentApp.SPEECHMODELSELECTED);
            if (value == null) return null;
            Log.d("getModel",value.toString());
            int index = ExternalStorage.getIntValue(context, ContentApp.SPEECHMODELSELECTEDINDEX);
            SpeechModelInfo modelInfo = new SpeechModelInfo(value.toString(), index);
            return modelInfo;
        }else{
            Log.d("getModel","-----");
        }
        return null;
    }
}
