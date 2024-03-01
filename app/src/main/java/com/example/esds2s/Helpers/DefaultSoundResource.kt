package com.example.esds2s.Helpers

import android.util.Log
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.R
import java.util.*

class DefaultSoundResource(private  val gender: GenderType,private  val  language: AvailableLanguages){


    val fourValuesArray: Array<Int> = arrayOf(R.raw.mus1,R.raw.mus2,R.raw.mus3,R.raw.mus4)


    fun getAudioResources(){

    }

//    fun getDefaultSoundResource(soundNum: Int): Int {
//
//    }

    class DefaultMaleSoundResource {

        fun getArabicSoundResource(soundNum: Int): Int {

            var randomValues = Random().nextInt(TypesOfVoiceResponses.values().size)
            var sound: Int = R.raw.row11
            if (soundNum > -1 && soundNum < TypesOfVoiceResponses.values().size)
                randomValues = soundNum

            sound = when (randomValues) {
                0 -> R.raw.row8
                1 -> R.raw.row9
                2 -> R.raw.row10
                3 -> R.raw.row11
                else -> return -1
            }
            return sound
        }
        fun getEnglishSoundResource(soundNum: Int): Int {
            var randomValues = Random().nextInt(TypesOfVoiceResponses.values().size)
            var sound: Int = R.raw.row11
            Log.d("getDefaultSoundResource", randomValues.toString() + "")
            if (soundNum > -1 && soundNum < TypesOfVoiceResponses.values().size) randomValues =
                soundNum
            sound = when (randomValues) {
                0 -> R.raw.row8
                1 -> R.raw.row9
                2 -> R.raw.row10
                3 -> R.raw.row11
                else -> return -1
            }
            return -1
        }
        fun getFrenchSoundResource(soundNum: Int): Int {

            var randomValues=soundNum
            if(soundNum<0)
                randomValues = Random().nextInt(TypesOfVoiceResponses.values().size)
            else if ( soundNum < TypesOfVoiceResponses.values().size)
                randomValues = soundNum

          val sound = when (randomValues) {
                0 -> R.raw.row8
                1 -> R.raw.row9
                2 -> R.raw.row10
                3 -> R.raw.row11
                else -> return -1
            }
            return sound
        }
        fun getSpanishSoundResource(soundNum: Int): Int {
            var randomValues = Random().nextInt(TypesOfVoiceResponses.values().size)
            var sound: Int = R.raw.row11
            Log.d("getDefaultSoundResource", randomValues.toString() + "")
            if (soundNum > -1 && soundNum < TypesOfVoiceResponses.values().size) randomValues =
                soundNum
            sound = when (randomValues) {
                0 -> R.raw.row8
                1 -> R.raw.row9
                2 -> R.raw.row10
                3 -> R.raw.row11
                else -> return -1
            }
            return sound
        }
    }
}