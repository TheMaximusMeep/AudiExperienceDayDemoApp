package technology.cariad.partnershowcase

import androidx.compose.ui.graphics.Color

/**
 * The two knobs for the live change on stage ("change one line, build, update").
 *
 * Version 1 ships as it is. For version 2, change the greeting and the accent, then run
 * `scripts/build-install.ps1 -Version 2` — the car updates the app in place.
 */
object DemoConfig {
    const val GREETING = "Hello, Audi In-Car Experience Day!"

    /** CARIAD mint. On stage, try a light violet: Color(0xFF7B6CFF), or coral: Color(0xFFEE4C40). */
    val ACCENT = Color(0xFF1EEF97)
}
