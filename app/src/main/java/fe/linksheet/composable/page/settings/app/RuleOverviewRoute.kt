package fe.linksheet.composable.page.settings.app

import androidx.compose.runtime.Composable
import app.linksheet.feature.engine.ui.route.ScenarioOverviewRoute
import fe.composekit.route.Route

@Composable
fun RuleOverviewRoute(
    onBackPressed: () -> Unit,
    navigate: (Route) -> Unit,
) {
    ScenarioOverviewRoute(
        onBackPressed = onBackPressed,
        navigate = navigate
    )
}
