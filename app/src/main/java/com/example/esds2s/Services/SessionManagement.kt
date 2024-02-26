package com.example.esds2s.Services

import android.content.Context
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.Helpers.JsonStorageManager
import com.example.esds2s.Helpers.LanguageInfo

class SessionManagement {
    companion object {
        fun OnLogOutFromSession(context: Context) {
            if (context == null)
                return
            if (ExternalStorage.existing(context, ContentApp.CURRENT_SESSION_TOKEN)) {
                ExternalStorage.remove(context, ContentApp.CURRENT_SESSION_TOKEN)
            }
            LanguageInfo.removeStorageSelcetedLanguage(context)
            if (JsonStorageManager(context)?.exists(ContentApp.CHATS_LIST_STORAGE)!!)
                JsonStorageManager(context)?.delete(ContentApp.CHATS_LIST_STORAGE)
        }
    }
}