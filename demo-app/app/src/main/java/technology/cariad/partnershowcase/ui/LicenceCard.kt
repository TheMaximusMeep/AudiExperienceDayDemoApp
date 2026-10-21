package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class LicenceState { CHECKING, ACTIVE, NOT_ACTIVE }

/**
 * "Partner+" is sold by the car maker's store on the partner's behalf; the app only asks whether
 * this vehicle holds it. NOT_ACTIVE also means "could not confirm" — so we degrade, never lock out.
 */
@Composable
fun LicenceCard(
    state: LicenceState,
    onUnlock: () -> Unit,
    onCheckAgain: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalShowcaseColors.current
    ShowcaseCard("Partner+ licence", tag = "Entitlement", modifier = modifier) {
        when (state) {
            LicenceState.CHECKING ->
                Readout("Asking the store…", "Checking", highlight = c.muted)
            LicenceState.ACTIVE ->
                Readout("Bought in the car's store", "Active: ad-free · HD", highlight = c.accentText)
            LicenceState.NOT_ACTIVE ->
                Readout("Free tier — all core features work", "Not active", highlight = c.warning)
        }
        Text(
            if (state == LicenceState.ACTIVE) "One checkout, one bill, the customer's car account."
            else "Unlock in the store: Audi sells it for us, the app just checks.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state == LicenceState.ACTIVE) {
                QuietButton("Reset demo", onReset)
            } else {
                BigButton("Unlock in store", onUnlock, enabled = state != LicenceState.CHECKING)
            }
            QuietButton("Check again", onCheckAgain)
        }
    }
}
