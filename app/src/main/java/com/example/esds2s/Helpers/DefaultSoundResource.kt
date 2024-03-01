package com.example.esds2s.Helpers

import android.content.Context
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.R
import java.util.*

class DefaultSoundResource{

        companion object {

            private val arabicVocalResponses: Array<Array<Array<Array<Int>>>> = arrayOf(
                arrayOf(
                    //before Sound
                    arrayOf(
                        arrayOf(R.raw.mus1, R.raw.mus2, R.raw.mus3, R.raw.mus4),//Male
                        arrayOf()//Female
                    ),
                ),
                arrayOf(
                    //After Sound
                    arrayOf(
                        arrayOf(R.raw.row8, R.raw.row10),//Male
                        arrayOf()//Female
                    ),
                ),
            )

            private val englishVocalResponses: Array<Array<Array<Array<Int>>>> = arrayOf(
                arrayOf(
                    //before Sound
                    arrayOf(
                        arrayOf(),//Male
                        arrayOf()//Female
                    ),
                ),
                arrayOf(
                    //After Sound
                    arrayOf(
                        arrayOf(),//Male
                        arrayOf()//Female
                    ),
                ),
            )

            @JvmStatic
            fun getInitialAudioResources(lang: AvailableLanguages, gender: GenderType): Array<Int>? {
                return getAudioResources(lang, gender, DefaultAudioStatus.After)
            }

            @JvmStatic
             fun getEndAudioResources(lang: AvailableLanguages, gender: GenderType): Array<Int>? {
                return getAudioResources(lang, gender, DefaultAudioStatus.Before)
            }
            @JvmStatic
            fun getCurrentAudioResources(context: Context, status: DefaultAudioStatus): Array<Int> {
                val modelInfo = ModelInfo(context)
                return getAudioResources(modelInfo?.getLanguage()!!, modelInfo?.getGender()!!, status)
            }

            @JvmStatic
            fun getAudioResource(context:Context,status:DefaultAudioStatus,index:Int=-1):Int{

                val arrayVoices: Array<Int> = getCurrentAudioResources(context,status)
                if(arrayVoices==null || arrayVoices?.size!! < 1)
                    return  -1

                var randomValues:Int = Random().nextInt(arrayVoices?.size!!)
                if(index>-1 && index<arrayVoices?.size!!)
                    randomValues=index

                return arrayVoices?.get(randomValues)!!

            }

            private fun getAudioResources(
                lang: AvailableLanguages,
                gender: GenderType,
                status: DefaultAudioStatus
            ): Array<Int> {
                var res = when (lang) {
                    AvailableLanguages.ARABIC -> arabicVocalResponses.get(status.ordinal)
                        .get(gender.ordinal)
                    AvailableLanguages.ENGLISH -> englishVocalResponses.get(status.ordinal)
                        .get(gender.ordinal)
                    AvailableLanguages.SPANISH -> englishVocalResponses.get(status.ordinal)
                        .get(gender.ordinal)
                    AvailableLanguages.FRENCH -> englishVocalResponses.get(status.ordinal)
                        .get(gender.ordinal)
                    else -> {}
                }
                return res as Array<Int>
            }



            fun getAgainQuestions():Int{
                return  R.raw.row10
            }

        }



}