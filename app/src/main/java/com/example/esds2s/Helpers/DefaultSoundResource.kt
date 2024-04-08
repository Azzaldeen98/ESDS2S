package com.example.esds2s.Helpers

import android.content.Context
import com.example.esds2s.Helpers.Enums.AvailableLanguages
import com.example.esds2s.Helpers.Enums.DefaultAudioStatus
import com.example.esds2s.Helpers.Enums.GenderType
import com.example.esds2s.Helpers.Enums.TypesOfVoiceResponses
import com.example.esds2s.R
import java.util.*
import kotlin.collections.ArrayList

class DefaultSoundResource{

    companion object {

        private val availableLanguages: Map<AvailableLanguages, Array<Array<Array<Int>>>> = mapOf(
            AvailableLanguages.ARABIC to arrayOf(
                //before Sound
                arrayOf(
                    arrayOf(R.raw.mus1, R.raw.mus2, R.raw.mus3, R.raw.mus4),//Male
                    arrayOf()//Female
                ),

                //After Sound
                arrayOf(
                    arrayOf(R.raw.row8, R.raw.row10),//Male
                    arrayOf()//Female
                ),

                ),
            AvailableLanguages.ENGLISH to arrayOf(
                //before Sound
                arrayOf(
                    arrayOf(),/*Male*/
                    arrayOf()/*Female*/
                ),
                //After Sound
                arrayOf(
                    arrayOf(),//Male
                    arrayOf()//Female
                    ),

        ),
            AvailableLanguages.SPANISH to arrayOf(
                //before Sound
                arrayOf(
                    arrayOf(),/*Male*/
                    arrayOf()/*Female*/
                ),
                //After Sound
                arrayOf(
                    arrayOf(),//Male
                    arrayOf()//Female
                    ),

        ),
            AvailableLanguages.FRENCH to arrayOf(
                //before Sound
                arrayOf(
                    arrayOf(),/*Male*/
                    arrayOf()/*Female*/
                ),
                //After Sound
                arrayOf(
                    arrayOf(),//Male
                    arrayOf()//Female
                    ),

        ),)

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

            return arrayVoices?.get(randomValues)?:-1

        }

        private fun getAudioResources(lang: AvailableLanguages?, gender: GenderType?, status: DefaultAudioStatus?): Array<Int> {
               if(lang==null || gender==null || status ==null)
                   return  emptyArray()

                if(availableLanguages.containsKey(lang)){
                    val langs =availableLanguages?.get(lang);
                    if(langs!=null && langs.size>status.ordinal) {
                        val array_status=langs?.get(status.ordinal)
                        if(array_status!=null && array_status.size>gender.ordinal) {
                            return  array_status?.get(gender.ordinal)?:emptyArray()
                        }
                    }
                }
            return  emptyArray()
        }

//        private fun getAudioResources(lang: AvailableLanguages, gender: GenderType, status: DefaultAudioStatus)
//        : Array<Int> {
//            var res:Array<Int> = when (lang) {
//                AvailableLanguages.ARABIC -> arabicVocalResponses[status.ordinal][gender.ordinal]
//                AvailableLanguages.ENGLISH -> englishVocalResponses[status.ordinal][gender.ordinal]
//                AvailableLanguages.SPANISH -> englishVocalResponses[status.ordinal][gender.ordinal]
//                AvailableLanguages.FRENCH ->  englishVocalResponses[status.ordinal][gender.ordinal]
//                else -> { emptyArray<Int>() }
//            }
//            return res
//        }



        fun getAgainQuestions(context: Context):Int?{

            val modelInfo = ModelInfo(context)
            if(modelInfo!=null) {
                val sounds=getAudioResources(modelInfo?.getLanguage()!!, modelInfo?.getGender()!!, DefaultAudioStatus.After)
                if(sounds!=null && sounds.isNotEmpty())
                    return  sounds?.get(1)?:null
            }
            return  null
        }
        //        private val arabicVocalResponses: Array<Array<Array<Int>>> = arrayOf(
//                //before Sound
//                arrayOf(
//                    arrayOf(R.raw.mus1, R.raw.mus2, R.raw.mus3, R.raw.mus4),//Male
//                    arrayOf()//Female
//                ),
//
//                //After Sound
//                arrayOf(
//                    arrayOf(R.raw.row8, R.raw.row10),//Male
//                    arrayOf()//Female
//                ),
//
//        )
//        private val englishVocalResponses: Array<Array<Array<Int>>> = arrayOf(
//                //before Sound
//                arrayOf(
//                    arrayOf(),//Male
//                    arrayOf()//Female
//                ),
//                //After Sound
//                arrayOf(
//                    arrayOf(),//Male
//                    arrayOf()//Female
//                ),
//
//        )



    }



}