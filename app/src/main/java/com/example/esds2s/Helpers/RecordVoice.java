package com.example.esds2s.Helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
public class RecordVoice {



    // string variable is created for storing a file name
    private static String mFileName = null;

    private  Context context;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String AudioSavaPath = null;

public  RecordVoice( Context context){
    this.context= context;
}



    public void startRecording() {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (checkPermissions()) {

            AudioSavaPath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+"recordingAudio.mp3";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(AudioSavaPath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
    public void stopRecord() {

        if(mediaRecorder==null) return;
        mediaRecorder.stop();
        mediaRecorder.release();
        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show();
    }
    public void plyerAudio() {

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(AudioSavaPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(x->stopPlyerAudio());
            Toast.makeText(context, "Start playing", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopPlyerAudio() {

        if (mediaPlayer != null) {

            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();

            mediaPlayer.release();
            Toast.makeText(context, "Stopped playing", Toast.LENGTH_SHORT).show();
        }
    }





    private boolean checkPermissions() {
//        int first = context.checkSelfPermission(context,
//                android.Manifest.permission.RECORD_AUDIO);
//        int second = context.checkSelfPermission(context,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//        return first == PackageManager.PERMISSION_GRANTED &&
//                second == PackageManager.PERMISSION_GRANTED;

        return  true;
    }




}

