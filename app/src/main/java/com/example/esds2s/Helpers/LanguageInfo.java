package com.example.esds2s.Helpers;

import android.content.Context;

import com.example.esds2s.ContentApp.ContentApp;

public class LanguageInfo {

        private String code;
        private int index;



        public LanguageInfo(String code, int index) {
            this.code = code;
            this.index = index;
        }

        public String getCode() {
            return code;
        }

        public int getIndex() {
            return index;
        }

    public static  void  setStorageSelcetedLanguage(Context context, String langCode, int langIndex){
        ExternalStorage.storage(context, ContentApp.LANGUAGE,langCode);
        ExternalStorage.storage(context,ContentApp.LANGUAGE_INDEX,langIndex);
    }

    public static LanguageInfo getStorageSelcetedLanguage(Context context){

        if(ExternalStorage.existing(context,ContentApp.LANGUAGE) && ExternalStorage.existing(context,ContentApp.LANGUAGE_INDEX)) {

            Object code = ExternalStorage.getValue(context, ContentApp.LANGUAGE);
            if(code==null) return null;
            int index = ExternalStorage.getIntValue(context,ContentApp.LANGUAGE_INDEX);
            LanguageInfo languageInfo = new LanguageInfo(code.toString().trim(),index);
            return languageInfo;
        }
        return null;
    }

    public static void removeStorageSelcetedLanguage(Context context){

        ExternalStorage.remove(context,ContentApp.LANGUAGE);
        ExternalStorage.remove(context,ContentApp.LANGUAGE_INDEX);
    }


}
