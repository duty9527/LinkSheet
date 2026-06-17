package fe.linksheet.debug.module

import app.linksheet.compose.debug.DebugPreferenceProvider
import app.linksheet.compose.debug.NoOpDebugPreferenceProvider
import fe.composekit.preference.FakePreferences
import fe.linksheet.composable.ui.ThemeV2
import fe.linksheet.module.viewmodel.RootViewModel
import fe.linksheet.util.DefaultLinkAssets
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val TestRootModule = module {
    single<DebugPreferenceProvider> { NoOpDebugPreferenceProvider }
    viewModel<RootViewModel> { TestRootViewModel() }
}

class TestRootViewModel : RootViewModel() {
    override val themeV2 = FakePreferences.mapped(ThemeV2.System, ThemeV2).vm
    override val themeAmoled = FakePreferences.boolean(false).vm
    override val themeMaterialYou = FakePreferences.boolean(false).vm
    override val linkAssets = MutableStateFlow(DefaultLinkAssets)
}
