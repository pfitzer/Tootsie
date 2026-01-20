package com.pfitzer.tootsie.domain.usecase.settings

import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import javax.inject.Inject

/**
 * Use case to update the app theme preference.
 */
class UpdateThemeUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(theme: String) {
        preferencesManager.saveTheme(theme)
    }
}
