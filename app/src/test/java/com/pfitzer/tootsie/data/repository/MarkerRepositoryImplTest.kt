package com.pfitzer.tootsie.data.repository

import com.pfitzer.tootsie.data.remote.api.MastodonApiService
import com.pfitzer.tootsie.data.remote.dto.MarkerDto
import com.pfitzer.tootsie.domain.repository.MarkerTimeline
import com.pfitzer.tootsie.presentation.screens.notifications.NotificationsUiState
import com.pfitzer.tootsie.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerRepositoryImplTest {

    private val api = mockk<MastodonApiService>()
    private val repository = MarkerRepositoryImpl(api)

    @Test
    fun `markers are read by timeline name, a missing one is null`() = runTest {
        coEvery { api.getMarkers(listOf("home")) } returns mapOf("home" to MarkerDto(lastReadId = "42"))
        coEvery { api.getMarkers(listOf("notifications")) } returns emptyMap()

        assertEquals(Result.Success("42"), repository.getMarker(MarkerTimeline.HOME))
        assertEquals(Result.Success(null), repository.getMarker(MarkerTimeline.NOTIFICATIONS))
    }

    @Test
    fun `a marker is saved as timeline last_read_id`() = runTest {
        coEvery { api.saveMarkers(any()) } returns emptyMap()

        repository.saveMarker(MarkerTimeline.NOTIFICATIONS, "7")

        coVerify { api.saveMarkers(mapOf("notifications[last_read_id]" to "7")) }
    }

    @Test
    fun `notifications newer than the marker are unread`() {
        val state = NotificationsUiState(lastReadId = "100", isReadMarkerLoaded = true)

        assertTrue(state.isUnread("101"))
        assertFalse(state.isUnread("100"))
        assertFalse(state.isUnread("99"))
        // Without a marker nothing is highlighted
        assertFalse(NotificationsUiState().isUnread("101"))
    }
}
