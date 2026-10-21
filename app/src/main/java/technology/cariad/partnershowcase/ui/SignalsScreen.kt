package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import technology.cariad.partnershowcase.car.Signal

/**
 * Every vehicle signal (VHAL property) this app may read, with its current value.
 *
 * A long, scrolling list is exactly what a driver must not read while driving — so this screen
 * exists only when the car is parked, and closes itself when the car starts moving.
 */
@Composable
fun SignalsScreen(signals: List<Signal>, onClose: () -> Unit) {
    val c = LocalShowcaseColors.current
    Column(
        Modifier.fillMaxSize().background(c.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Vehicle signals: ${signals.size}",
                    style = MaterialTheme.typography.displaySmall,
                    color = c.accentText,
                )
                Text(
                    "Every VHAL property this app's permissions let it read. More permissions, longer list.",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.muted,
                )
            }
            BigButton("Back", onClose)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(signals, key = { it.id }) { signal ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(c.card, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        signal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.text,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        signal.value,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.accentText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "0x%08X · %s%s".format(
                            signal.id,
                            signal.mode,
                            if (signal.areas > 1) " · ${signal.areas} areas" else "",
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.muted,
                        modifier = Modifier.weight(0.9f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
