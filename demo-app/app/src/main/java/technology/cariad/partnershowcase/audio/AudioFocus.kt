package technology.cariad.partnershowcase.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who may be heard right now, as far as our trailer is concerned. */
enum class FocusState(val label: String) {
    IDLE("Audio: idle"),
    PLAYING("Audio focus: ours"),
    DUCKED("Ducked: someone speaks over us"),
    PAUSED("Paused: another app has the audio"),
    LOST("Stopped: another app took over"),
}

/**
 * Audio focus, done the car way.
 *
 * In a car, several sources compete for one cabin: media, navigation prompts, phone calls, the
 * voice assistant, warning chimes. The car arbitrates; apps ask for focus before they play and
 * follow the answer:
 * - LOSS_TRANSIENT_CAN_DUCK: keep playing, quieter (a navigation prompt),
 * - LOSS_TRANSIENT: pause, resume on GAIN (the voice assistant, a call),
 * - LOSS: stop and do not come back on your own (the driver started another media app).
 *
 * Typical mistakes: playing without asking, ignoring the loss, or grabbing focus again on resume.
 */
class AudioFocus(context: Context) {

    private val audio = context.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(FocusState.IDLE)
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val request =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { change ->
                android.util.Log.i("PartnerShowcase", "Audio focus change: $change")
                _state.value =
                    when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> FocusState.PLAYING
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusState.DUCKED
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusState.PAUSED
                        else -> FocusState.LOST
                    }
            }
            .build()

    /** Ask before playing. The car may say no — then we do not play. */
    fun requestToPlay() {
        if (_state.value == FocusState.PLAYING || _state.value == FocusState.DUCKED) return
        _state.value =
            when (audio.requestAudioFocus(request)) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> FocusState.PLAYING
                else -> FocusState.PAUSED
            }
    }

    /** Give the cabin back as soon as we stop. */
    fun release() {
        audio.abandonAudioFocusRequest(request)
        _state.value = FocusState.IDLE
    }
}
