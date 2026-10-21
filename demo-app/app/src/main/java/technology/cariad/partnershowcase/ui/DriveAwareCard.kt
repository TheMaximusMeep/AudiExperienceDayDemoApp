package technology.cariad.partnershowcase.ui

import android.car.drivingstate.CarUxRestrictions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import technology.cariad.partnershowcase.audio.FocusState
import technology.cariad.partnershowcase.car.UxState
import technology.cariad.partnershowcase.net.NetworkState

private const val PARTNER_MESSAGE =
    "Tonight at the Audi Forum: the season finale, live and in HD. Doors open at 19:30, " +
        "parking next door is free for Partner+ members. Tap for line-up and menu."

/**
 * Driver distraction in practice: one card, three reactions to the car's UX restrictions.
 * 1. Video stops while the car says NO_VIDEO, and continues when parked.
 * 2. The keyboard goes away with NO_KEYBOARD — closed, not just greyed out.
 * 3. Long text is cut to the length the car allows with LIMIT_STRING_LENGTH.
 */
@Composable
fun DriveAwareCard(ux: UxState, audio: FocusState, network: NetworkState, modifier: Modifier = Modifier) {
    val c = LocalShowcaseColors.current
    ShowcaseCard("Media in the car", tag = "Distraction · audio · network", modifier = modifier) {
        val noVideo = ux.has(CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO)
        // Plays only when the car allows video AND we hold the audio focus (ducked is fine).
        val hasAudio = audio == FocusState.PLAYING || audio == FocusState.DUCKED
        TrailerPlayer(
            playing = !noVideo && hasAudio,
            pausedBecause =
                when {
                    noVideo -> "Video pauses while driving" to "It continues as soon as the car is parked"
                    audio == FocusState.PAUSED -> "Paused for the assistant or a call" to "Resumes when the car hands the audio back"
                    audio == FocusState.LOST -> "Another app is playing" to "We don't grab the audio back on our own"
                    else -> null
                },
            audio = audio,
            modifier = Modifier.fillMaxWidth().height(150.dp),
        )

        SearchField(keyboardAllowed = !ux.has(CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD))

        val message =
            if (ux.has(CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH) &&
                PARTNER_MESSAGE.length > ux.maxStringLength
            )
                PARTNER_MESSAGE.take(ux.maxStringLength).trimEnd() + "…"
            else PARTNER_MESSAGE
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.text)
        if (!network.online) {
            // Tunnels and garages happen: show what we have, say so, retry in the background.
            Pill("Offline: showing saved content", background = c.warning, foreground = c.onAccent)
        }

        RestrictionChips(ux)
    }
}

/** A stand-in for a video: moving shapes and a progress bar, frozen while it may not play. */
@Composable
private fun TrailerPlayer(
    playing: Boolean,
    pausedBecause: Pair<String, String>?,
    audio: FocusState,
    modifier: Modifier = Modifier,
) {
    val c = LocalShowcaseColors.current
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        var last = 0L
        while (playing) {
            withFrameNanos { now ->
                if (last != 0L) seconds = (seconds + (now - last) / 1e9f) % 130f
                last = now
            }
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B0118))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = seconds
            val colors = listOf(Color(0xFF442EE0), c.accent, Color(0xFFEE4C40), Color(0xFFFEF04A))
            for (i in 0..3) {
                val x = size.width * (((t * 0.11f) + i * 0.27f) % 1.2f - 0.1f)
                rotate(degrees = 28f + i * 9f, pivot = Offset(x, size.height / 2)) {
                    drawLine(
                        colors[i],
                        start = Offset(x, size.height * 0.1f),
                        end = Offset(x, size.height * 0.9f),
                        strokeWidth = size.height * 0.07f,
                        cap = StrokeCap.Butt,
                    )
                }
            }
            // Progress bar
            val progress = t / 130f
            drawLine(Color(0x55FFFFFF), Offset(0f, size.height - 6f), Offset(size.width, size.height - 6f), 6f)
            drawLine(c.accent, Offset(0f, size.height - 6f), Offset(size.width * progress, size.height - 6f), 6f)
        }
        Text(
            "Partner+ trailer  ·  %d:%02d / 2:10".format(seconds.toInt() / 60, seconds.toInt() % 60),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        Text(
            audio.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (audio == FocusState.DUCKED) c.warning else Color(0xFFBDB6D0),
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 16.dp, vertical = 14.dp),
        )
        if (!playing && pausedBecause != null) {
            Column(
                Modifier.fillMaxSize().background(Color(0xD912032A)).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    pausedBecause.first,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    pausedBecause.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDB6D0),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SearchField(keyboardAllowed: Boolean) {
    val c = LocalShowcaseColors.current
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Typical mistake: greying out the field but leaving the keyboard open on screen.
    LaunchedEffect(keyboardAllowed) {
        if (!keyboardAllowed) {
            keyboard?.hide()
            focus.clearFocus()
        }
    }

    OutlinedTextField(
        value = if (keyboardAllowed) query else "",
        onValueChange = { query = it },
        enabled = keyboardAllowed,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                if (keyboardAllowed) "Search shows, stations, places…"
                else "Typing is off while driving — just say \"Hey Audi\"",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                disabledBorderColor = c.outline,
                disabledPlaceholderColor = c.muted,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                cursorColor = c.accent,
            ),
        modifier = Modifier.fillMaxWidth().heightIn(min = TouchTarget),
    )
}

/** What the car currently forbids — the same list a developer reads from CarUxRestrictions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestrictionChips(ux: UxState) {
    val c = LocalShowcaseColors.current
    val all =
        listOf(
            CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO to "No video",
            CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD to "No keyboard",
            CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH to "Short text",
            CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT to "Short lists",
            CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP to "No setup",
            CarUxRestrictions.UX_RESTRICTIONS_NO_TEXT_MESSAGE to "No messages",
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((flag, label) in all) {
            val active = ux.has(flag)
            Pill(
                label,
                background = if (active) c.driving else c.cardRaised,
                foreground = if (active) Color.White else c.muted,
            )
        }
    }
    if (ux.requiresDistractionOptimization) {
        Text(
            "Limits now: text ≤ ${ux.maxStringLength} characters · lists ≤ ${ux.maxContentItems} items",
            style = MaterialTheme.typography.labelMedium,
            color = c.muted,
            modifier = Modifier.height(24.dp),
        )
    }
}
