package com.example.esds2s.Services.Broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.esds2s.Services.RecordVoiceService;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, RecordVoiceService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                context.startForegroundService(serviceIntent);
            else
                context.startService(serviceIntent);
        }
    }
}
