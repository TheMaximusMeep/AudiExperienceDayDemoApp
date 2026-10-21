package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Minimum touch target in the car: 76 dp. A bump in the road should not turn a tap into a miss. */
val TouchTarget = 76.dp

/** One capability of the car, one card. [tag] says which kind of integration it is. */
@Composable
fun ShowcaseCard(
    title: String,
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalShowcaseColors.current
    Column(
        modifier
            .background(c.card, RoundedCornerShape(28.dp))
            .border(1.dp, c.outline, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Pill(tag, background = c.cardRaised, foreground = c.muted)
        }
        content()
    }
}

@Composable
fun Pill(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = foreground, maxLines = 1)
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalShowcaseColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = TouchTarget),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = c.accent,
                contentColor = c.onAccent,
                disabledContainerColor = c.cardRaised,
                disabledContentColor = c.muted,
            ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalShowcaseColors.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = TouchTarget),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** A label above a big value, the way a cluster shows it. */
@Composable
fun Readout(label: String, value: String, modifier: Modifier = Modifier, highlight: Color? = null) {
    val c = LocalShowcaseColors.current
    Column(
        modifier
            .background(c.cardRaised, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted, maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = highlight ?: c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
