package com.pfitzer.tootsie.presentation.components.status

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.pfitzer.tootsie.domain.model.Status

/**
 * ID of the logged-in account, to recognize own posts in status cards. Provided by the
 * nav host; null: unknown, so no post is treated as own.
 */
val LocalActiveAccountId = compositionLocalOf<String?> { null }

/**
 * Opens the compose screen to edit an own status. Provided once by the nav host, like
 * [LocalOnQuoteStatus]. Null: editing not available.
 */
val LocalOnEditStatus = staticCompositionLocalOf<((Status) -> Unit)?> { null }

/**
 * Deletes an own status (after confirmation in the card); `redraft` opens it as a draft
 * afterwards. Provided by the nav host. Null: deleting not available.
 */
val LocalOnDeleteStatus = staticCompositionLocalOf<((status: Status, redraft: Boolean) -> Unit)?> { null }

/**
 * Opens the edit history of a status. Null: history not available.
 */
val LocalOnShowEditHistory = staticCompositionLocalOf<((Status) -> Unit)?> { null }
