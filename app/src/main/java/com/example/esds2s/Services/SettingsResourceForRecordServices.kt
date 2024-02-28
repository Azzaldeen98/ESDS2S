package com.example.esds2s.Services

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings
import android.util.Log
import com.example.esds2s.ContentApp.ContentApp
import com.example.esds2s.Helpers.AudioPlayer
import com.example.esds2s.Helpers.Enums.AudioPlayerStatus
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.ExternalStorage

class SettingsResourceForRecordServices {



   private val stopWords: Array<List<String>> = emptyArray()
    init{
        stopWords[0] = listOf("توقف", "قف", "اصمت","تكفى","كفى","يكفي","لا تتحدث")
        stopWords[1] = listOf("stop")
        stopWords[2] = listOf("stop")
        stopWords[3] = listOf("stop")
    }

    fun getStopWords(lang:AvailableLanguages):List<String>{
        return stopWords.get(lang.ordinal)
    }

    fun jaccardSimilarity(sentence: String, wordSet: Set<String>): Double {
        val sentenceSet = sentence.split(" ").toSet()
        val intersectionSize = sentenceSet.intersect(wordSet).size
        val unionSize = sentenceSet.union(wordSet).size
        return intersectionSize.toDouble() / unionSize.toDouble()
    }

    fun isStopWord(lang:AvailableLanguages,word:String):Boolean{
        if(word.isNullOrEmpty())
            return  false

        val num=jaccardSimilarity(word,setOf("توقف", "قف", "اصمت","تكفى","كفى","يكفي","لا تتحدث"))

//            getStopWords(lang).toSet())
//            return  getStopWords(lang)?.contains(word.trim()) ?: false
         return  num>0.0f
    }

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


        fun DidTheUserAskToStopTalking(text:String,audioPlayer: AudioPlayer){

        }

        fun DiscoverStopWords(text:String){

        }
        fun vibrateSoundMode(activity:Activity) {

            val audioManager = activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager: NotificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (audioManager.isVolumeFixed) {
                val intent= Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                activity?.startActivity(intent)
            } else  if( audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
                // قم بتحويل الهاتف إلى وضع الاهتزاز
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }

        }
    }
}