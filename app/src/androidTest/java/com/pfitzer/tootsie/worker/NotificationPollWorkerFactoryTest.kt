package com.pfitzer.tootsie.worker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.TestListenableWorkerBuilder
import com.pfitzer.tootsie.TootsieApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's HiltWorkerFactory must be able to create the poll worker. Without the
 * androidx.hilt compiler it falls back to reflection and fails with NoSuchMethodException.
 */
@RunWith(AndroidJUnit4::class)
class NotificationPollWorkerFactoryTest {

    @Test
    fun hiltWorkerFactoryCreatesNotificationPollWorker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val factory = (context.applicationContext as TootsieApplication).workManagerConfiguration.workerFactory

        val worker = TestListenableWorkerBuilder<NotificationPollWorker>(context)
            .setWorkerFactory(factory)
            .build()

        assertTrue(worker is NotificationPollWorker)
    }
}
