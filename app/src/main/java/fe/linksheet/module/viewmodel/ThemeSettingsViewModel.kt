package fe.linksheet.module.viewmodel

import androidx.lifecycle.ViewModel
import app.linksheet.api.preference.AppPreferenceRepository
import fe.android.preference.helper.Preference
import fe.composekit.preference.ViewModelStatePreference
import fe.linksheet.composable.ui.ThemeV2
import fe.linksheet.module.preference.app.AppPreferences
import fe.linksheet.util.DefaultLinkAssets
import fe.linksheet.util.LinkAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeSettingsViewModel(
    val preferenceRepository: AppPreferenceRepository,
) : RootViewModel() {
    override val themeV2 = preferenceRepository.asViewModelState(AppPreferences.themeV2.themeV2)
    override val themeAmoled = preferenceRepository.asViewModelState(AppPreferences.themeV2.amoled)
    override val themeMaterialYou = preferenceRepository.asViewModelState(AppPreferences.themeV2.materialYou)
    override val linkAssets = MutableStateFlow(DefaultLinkAssets)
}

abstract class RootViewModel : ViewModel(){
    abstract val themeV2: ViewModelStatePreference<ThemeV2, ThemeV2, Preference.Mapped<ThemeV2, String>>
    abstract val themeAmoled: ViewModelStatePreference<Boolean, Boolean, Preference.Default<Boolean>>
    abstract val themeMaterialYou: ViewModelStatePreference<Boolean, Boolean, Preference.Default<Boolean>>
    abstract val linkAssets: StateFlow<LinkAssets>
}
