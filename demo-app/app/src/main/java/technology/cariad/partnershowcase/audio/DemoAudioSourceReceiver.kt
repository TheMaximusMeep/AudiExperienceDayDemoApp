package technology.cariad.partnershowcase.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Demo only: plays the part of another audio source in the cabin, so the stage can show how the
 * trailer reacts. It asks the car for focus the way that source would, holds it a few seconds,
 * then gives it back.
 *
 * `adb shell am broadcast -a technology.cariad.partnershowcase.DEMO_AUDIO_SOURCE
 *  -p technology.cariad.partnershowcase --es source assistant|navigation|call`
 */
class DemoAudioSourceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val source = intent.getStringExtra("source") ?: "assistant"
        val (usage, gain) =
            when (source) {
                "navigation" -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE to
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                "call" -> AudioAttributes.USAGE_VOICE_COMMUNICATION to AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                else -> AudioAttributes.USAGE_ASSISTANT to AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            }
        val audio = context.getSystemService(AudioManager::class.java)
        val request =
            AudioFocusRequest.Builder(gain)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(usage).build())
                .build()
        val result = audio.requestAudioFocus(request)
        Log.i("PartnerShowcase", "Demo audio source '$source' asked for focus: result=$result")

        // Not goAsync(): the broadcast returns at once, the app is on screen and stays alive.
        Handler(Looper.getMainLooper()).postDelayed({
            audio.abandonAudioFocusRequest(request)
            Log.i("PartnerShowcase", "Demo audio source '$source' gave the focus back")
        }, HOLD_MS)
    }

    private companion object {
        const val HOLD_MS = 5_000L
    }
}
