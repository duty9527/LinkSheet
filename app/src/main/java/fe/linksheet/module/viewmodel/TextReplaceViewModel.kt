package fe.linksheet.module.viewmodel

import app.linksheet.api.preference.AppPreferenceRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fe.linksheet.module.preference.app.AppPreferences
import fe.linksheet.module.preference.app.TextReplaceRule
import fe.linksheet.module.viewmodel.base.BaseViewModel

class TextReplaceViewModel(
    val preferenceRepository: AppPreferenceRepository,
    private val gson: Gson
) : BaseViewModel(preferenceRepository) {

    private val rulesJsonState = preferenceRepository.asViewModelState(AppPreferences.textReplaceRulesJson)

    fun getRules(): List<TextReplaceRule> {
        val json = rulesJsonState.value
        val type = object : TypeToken<List<TextReplaceRule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<TextReplaceRule>) {
        val json = gson.toJson(rules)
        preferenceRepository.put(AppPreferences.textReplaceRulesJson, json)
    }
}
