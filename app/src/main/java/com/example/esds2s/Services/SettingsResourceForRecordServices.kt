package com.example.esds2s.Services

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.Enums.AudioPlayerStatus
import com.example.esds2s.Helpers.ExternalStorage

class SettingsResourceForRecordServices {

    companion object {
        fun checkAudioPlayerSettings(context: Context, player: AudioPlayer) {


            if(player==null || context==null)
                return

            val status = ExternalStorage.getValue(context!!,ContentApp.ROBOT_CHAT_SETTINGS,ContentApp.PLAYER_ROBOT_AUDIO) as String?

            Log.d("player444",status.toString())
            if (status !=null && status.equals(AudioPlayerStatus.PAUSE.ordinal.toString()) && player.isPlayer()) {

                ExternalStorage.storage(context,
                    ContentApp.ROBOT_CHAT_SETTINGS,
                    ContentApp.PLAYER_ROBOT_AUDIO,
                    AudioPlayerStatus.START.ordinal.toString())

                player?.mediaPlayer?.seekTo(player?.mediaPlayer!!.duration)
                Log.d("player33","Stop")
            }
            else if (status !=null && status.equals(AudioPlayerStatus.RESUME.ordinal.toString())  && !player.isPlayer())
            {
                player.resume()

                Log.d("player33","resume")
            }
            else if (status !=null && status.equals(AudioPlayerStatus.STOP.ordinal.toString())  && !player.isPlayer())
            {
                player.stop()
                Log.d("player33","stop")
            }


        }
    }
}