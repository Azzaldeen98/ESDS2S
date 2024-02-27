package com.example.esds2s.Ui.Helpers

import android.content.Context
import android.widget.ImageView
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.Enums.AudioPlayerStatus
import com.example.esds2s.Helpers.ExternalStorage
import com.example.esds2s.R

class Options {

    companion object {
        fun controlStopRobotVoice(
            context: Context?,
            robotSpeekerBtn: ImageView?,
            isMuteVoice: Boolean
        ) {
            var isMuteVoice = isMuteVoice
            if (robotSpeekerBtn == null) return
            if (!isMuteVoice) {
                robotSpeekerBtn.setImageResource(R.drawable.baseline_voice_over_off_24)
                ExternalStorage.storage(
                    context,
                    ContentApp.ROBOT_CHAT_SETTINGS,
                    ContentApp.PLAYER_ROBOT_AUDIO,
                    AudioPlayerStatus.PAUSE.ordinal.toString())
            } else {
                robotSpeekerBtn.setImageResource(R.drawable.baseline_record_voice_over_24)
                ExternalStorage.storage(
                    context,
                    ContentApp.ROBOT_CHAT_SETTINGS,
                    ContentApp.PLAYER_ROBOT_AUDIO,
                    AudioPlayerStatus.RESUME.ordinal.toString()
                )
            }
            isMuteVoice = !isMuteVoice
        }
    }
}