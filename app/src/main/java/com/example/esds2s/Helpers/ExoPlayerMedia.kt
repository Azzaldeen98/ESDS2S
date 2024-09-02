package com.example.esds2s.Helpers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.media3.common.*
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.esds2s.ApiClient.Interface.ICustomPlayerListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resumeWithException

class ExoPlayerMedia(private val context: Context?) {

    var player: ExoPlayer? =null
    var listener : Listener?=null;
    @androidx.annotation.OptIn(UnstableApi::class)
    fun startPlayer(streamUrl: String, callback: ICustomPlayerListener<ExoPlayer>) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()

        // إعداد MediaItem باستخدام عنوان URL للدفق
        val mediaItem = MediaItem.fromUri(streamUrl)

        // إنشاء MediaSource
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        // إعداد خصائص الصوت
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // إنشاء ExoPlayer وإعداده
        val exoPlayer = ExoPlayer.Builder(context!!).build().apply {
            setAudioAttributes(audioAttributes, true)
            addListener(object : Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> Log.d("ExoPlayer", "Player is ready to play")
                        Player.STATE_BUFFERING -> Log.d("ExoPlayer", "Buffering...")
                        Player.STATE_ENDED -> {
                            Log.d("ExoPlayer", "Playback completed")
                            callback.onCompletionListener(this@apply)
                        }
                        Player.STATE_IDLE -> Log.d("ExoPlayer", "Player is idle")
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("ExoPlayer", "Error occurred: ${error.message}")
                    callback.onErrorListener(this@apply)
                }
            })
        }

        // إعداد وتشغيل ExoPlayer
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }
    @SuppressLint("UnsafeOptInUsageError", "SuspiciousIndentation")
    fun start(url: String?, callback: ICustomPlayerListener<ExoPlayer>): ExoPlayer? {

        try{
//
//            if(player!=null)
//                player?.release()

            player = ExoPlayer.Builder(context!!).build().also { exoPlayer ->

                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .setUsage(C.USAGE_MEDIA)
                        .build()

                    exoPlayer.setAudioAttributes(audioAttributes, true)

//                val dataSourceFactory = DefaultHttpDataSource.Factory()
//                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
//                    .createMediaSource(MediaItem.fromUri(url!!))
//                exoPlayer?.setMediaSource(mediaSource)

//                    val mediaItem = MediaItem.fromUri(url!!)
                val mediaItem = MediaItem.Builder()
                    .setUri(url!!)
                    .setMimeType(MimeTypes.AUDIO_WAV)
//                    .setStreamKeys()
//                    .setRequestMetadata()
//                    .setLiveConfiguration()
                    .build()

                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                    exoPlayer?.playWhenReady = true


                    exoPlayer?.addListener(playbackStateListener(mediaItem,callback))

                }

            return  player;

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ExoPlayer", "IOException: ${e.message}")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Log.e("ExoPlayer", "IllegalArgumentException: ${e.message}")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Log.e("ExoPlayer", "SecurityException: ${e.message}")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Log.e("ExoPlayer", "IllegalStateException: ${e.message}")
        }
        catch (e: Exception) {
            e.printStackTrace()
            Log.e("ExoPlayer", "IllegalStateException: ${e.message}")
            callback.onErrorListener(player!!)
        }
        return  player
    }

    fun playbackStateListener(mediaItem : MediaItem?=null,callback: ICustomPlayerListener<ExoPlayer>) = object : Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {

                when (playbackState) {

                    ExoPlayer.STATE_IDLE -> {
                        println("ExoPlayer.STATE_IDLE     -")
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                        println("ExoPlayer.STATE_BUFFERING     -")
                    }
                    ExoPlayer.STATE_READY -> {

                        println("ExoPlayer.STATE_READY     -")
                    }
                    ExoPlayer.STATE_ENDED -> {

                        println("ExoPlayer.STATE_ENDED     -")
                        callback?.onCompletionListener(player!!)
                    }
                    else -> {

                        println("ExoPlayer.UNKNOWN_STATE     -")
                    }
                }
//            Log.d(TAG, "changed state to $stateString")
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error occurred: ${error.message}")
                if (error.cause is AudioSink.UnexpectedDiscontinuityException && mediaItem != null) {
                    player?.release()
                    player = ExoPlayer.Builder(context!!).build().apply {
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }else
                        callback?.onErrorListener(player!!)
            }
    }
    suspend fun waitForRemainingTime() {
        try {
            while (player!!.isPlaying) {
                delay(100) // الانتظار لمدة نصف ثانية قبل التحقق مرة أخرى
            }
        }catch (e:Exception){}

    }
    suspend fun waitForPlaybackToComplete() = suspendCancellableCoroutine<Unit> { cont ->
        val listener = object : Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    cont.resume(Unit) {}
                    player?.removeListener(this)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                cont.resumeWithException(error)
                player?.removeListener(this)
            }

        }
        player?.addListener(listener)

        // في حالة الإلغاء، إزالة المستمع
        cont.invokeOnCancellation {
            player?.removeListener(listener)
        }
    }


    fun seekToCompletion() {
    player?.seekBackIncrement
        if(player!=null && player!!.isPlaying)
            player?.seekTo(player!!.duration)
    }

     fun getRemainingTime(): Long {
        val duration = player?.duration ?:0
        val currentPosition = player?.currentPosition ?:0
        return if (duration != C.TIME_UNSET) {
                duration!! - currentPosition!!
        } else {
            0
        }
    }

    fun onDestroy() {
        if(player!=null)
            player!!.release()
    }
    fun isPlayer():Boolean {
        if(player!=null)
            return  player?.isPlaying!!
        return  false;
    }
    fun stop() {
        try{
            if( player?.isPlaying==true){
                player?.stop()
                player?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //    @SuppressLint("UnsafeOptInUsageError")
//    fun sampleStart(url: String?, callback: ICustomPlayerListener<SimpleExoPlayer>): SimpleExoPlayer? {
//
//        try{
//
//            if(player!=null)
//                player?.release()
//
////            val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(2).build()
////                val exoPlayer = SimpleExoPlayer.Builder(context!!).setLoadControl(loadControl).build()
////
////
////
////            val bandWidth= DefaultBandwidthMeter.Builder(context!!).build()
////            var adaptiveTrackSelection = AdaptiveTrackSelection.Factory(bandWidth)
//            var build=SimpleExoPlayer.Builder(context!!)
////            build.setTrackSelector(adaptiveTrackSelection)
//            player = build.build().also { exoPlayer ->
//
//                val audioAttributes = AudioAttributes.Builder()
//                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
//                    .setUsage(C.USAGE_MEDIA)
//                    .build()
//
//                exoPlayer.setAudioAttributes(audioAttributes, true)
//
////                val dataSourceFactory = DefaultHttpDataSource.Factory()
////                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
////                    .createMediaSource(MediaItem.fromUri(url!!))
////                exoPlayer?.setMediaSource(mediaSource)
//
//                val mediaItem = MediaItem.fromUri(url!!)
//                exoPlayer?.setMediaItem(mediaItem)
//                exoPlayer?.prepare()
//                exoPlayer?.playWhenReady = true
//
//
//                exoPlayer?.addListener(object : Player.Listener {
//                    override fun onPlaybackStateChanged(playbackState: Int) {
//                        Log.d("ExoPlayer", "$playbackState")
//                        when (playbackState) {
//                            Player.STATE_ENDED -> {
//                                Log.d("ExoPlayer", "Playback completed")
//                                callback.onCompletionListener(player!!)
//                                // Perform action when playback is completed
//                            }
//                            Player.STATE_READY -> {
//                                Log.d("ExoPlayer", "Player is ready to play")
//                            }
//                            Player.STATE_BUFFERING -> {
//                                Log.d("ExoPlayer", "Buffering...")
//                                callback.onCompletionListener(player!!)
//                            }
//                            Player.STATE_IDLE -> {
//                                Log.d("ExoPlayer", "Player is idle")
//                            }
//                        }
//                    }
//                    override fun onPlayerError(error: PlaybackException) {
//                        Log.e("ExoPlayer", "Error occurred: ${error.message}")
//                        if (error.cause is AudioSink.UnexpectedDiscontinuityException) {
//                            player?.release()
//                            player = ExoPlayer.Builder(context).build().apply {
//                                setMediaItem(mediaItem)
//                                prepare()
//                                playWhenReady = true
//                            }
//                        }
////                            callback.onErrorListener(player!!)
//                    }
//                })
//            }
//            player.setT
//            return  player;
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//            Log.e("ExoPlayer", "IOException: ${e.message}")
//        } catch (e: IllegalArgumentException) {
//            e.printStackTrace()
//            Log.e("ExoPlayer", "IllegalArgumentException: ${e.message}")
//        } catch (e: SecurityException) {
//            e.printStackTrace()
//            Log.e("ExoPlayer", "SecurityException: ${e.message}")
//        } catch (e: IllegalStateException) {
//            e.printStackTrace()
//            Log.e("ExoPlayer", "IllegalStateException: ${e.message}")
//        }
//        return  player
//    }

}