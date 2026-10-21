package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import technology.cariad.partnershowcase.parking.ParkingEvent
import technology.cariad.partnershowcase.parking.ParkingSession

/**
 * "Car parked" from the App Store, and what a parking partner does with it: offer to start a
 * session right where the driver stopped — without the driver opening the app first.
 */
@Composable
fun ParkingCard(
    event: ParkingEvent?,
    session: ParkingSession?,
    locationShared: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    onAllowLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalShowcaseColors.current
    ShowcaseCard("Car parked", tag = "Store event", modifier = modifier) {
        when {
            session != null -> {
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(session) {
                    while (true) {
                        now = System.currentTimeMillis()
                        delay(1000)
                    }
                }
                val elapsed = ((now - session.startedAtMillis) / 1000).coerceAtLeast(0)
                Highlight("Parking session running", session.zone)
                Text(
                    "%02d:%02d".format(elapsed / 60, elapsed % 60),
                    style = MaterialTheme.typography.displaySmall,
                    color = c.accentText,
                )
                QuietButton("Stop session", onStop)
            }
            event != null -> {
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.atMillis))
                val where =
                    if (event.hasPosition) "%.4f, %.4f".format(event.latitude, event.longitude)
                    else "position not shared"
                Highlight("Parked at $time", where)
                Text(
                    "Start parking here? Zone 4711 · Audi Forum",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.text,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton("Start parking", onStart)
                    QuietButton("Not now", onDismiss)
                }
            }
            else -> {
                Text(
                    "Waiting for the car to park. Shift to P and the store tells us — " +
                        "even when this app is closed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                )
            }
        }
        // Only worth a line while there is no position to show.
        if (!locationShared && session == null && event?.hasPosition != true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The store adds the position only with location \"Allow all the time\".",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.muted,
                    modifier = Modifier.weight(1f),
                )
                QuietButton("Allow", onAllowLocation)
            }
        }
    }
}

@Composable
private fun Highlight(title: String, subtitle: String) {
    val c = LocalShowcaseColors.current
    Column(
        Modifier.fillMaxWidth()
            .background(c.cardRaised, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
}
