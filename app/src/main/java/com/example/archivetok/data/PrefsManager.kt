package com.example.archivetok.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("archivetok_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_TUTORIAL_SHOWN = "tutorial_shown"
        private const val KEY_SELECTED_TAGS = "selected_tags"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isTutorialShown: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_SHOWN, value).apply()

    var selectedTags: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_TAGS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_TAGS, value).apply()
}
