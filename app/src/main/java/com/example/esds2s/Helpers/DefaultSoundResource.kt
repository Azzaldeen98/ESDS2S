package com.example.esds2s.Helpers

import android.util.Log
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.R
import java.util.*

class DefaultSoundResource(private  val gender: GenderType,private  val  language: AvailableLanguages){
    
    val arabicVocalResponses: Array<Array<Array<Array<Int>>>> = arrayOf(
        arrayOf( //before Sound
            arrayOf(
                arrayOf(R.raw.mus1,R.raw.mus2,R.raw.mus3,R.raw.mus4),//Male
                arrayOf()//Female
            ),
        ),
        arrayOf( //After Sound
            arrayOf(
                arrayOf(R.raw.row8,R.raw.row10),//Male
                arrayOf()//Female
            ),
        ),
    )

    val englishVocalResponses: Array<Array<Array<Array<Int>>>> = arrayOf(
        arrayOf( //before Sound
            arrayOf(
                arrayOf(),//Male
                arrayOf()//Female
            ),
        ),
        arrayOf( //After Sound
            arrayOf(
                arrayOf(),//Male
                arrayOf()//Female
            ),
        ),
    )


    fun getInitialAudioResources(lang:AvailableLanguages,gender:GenderType):Array<Int>?{
       return getAudioResources(lang,gender,DefaultAudioStatus.After)
    }

    fun getEndAudioResources(lang:AvailableLanguages,gender:GenderType):Array<Int>?{
        return getAudioResources(lang,gender,DefaultAudioStatus.Before)
    }

   private fun getAudioResources(lang:AvailableLanguages,gender:GenderType,status: DefaultAudioStatus):Array<Int>{
        var res = when(lang){
            AvailableLanguages.ARABIC->arabicVocalResponses.get(status.ordinal).get(gender.ordinal)
            AvailableLanguages.ENGLISH-> englishVocalResponses.get(status.ordinal).get(gender.ordinal)
            AvailableLanguages.SPANISH->englishVocalResponses.get(status.ordinal).get(gender.ordinal)
            AvailableLanguages.FRENCH->englishVocalResponses.get(status.ordinal).get(gender.ordinal)
            else -> {}
        }
       return res as Array<Int>
    }


}