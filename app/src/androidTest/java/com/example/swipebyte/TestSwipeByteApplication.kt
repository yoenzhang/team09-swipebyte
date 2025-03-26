package com.example.swipebyte

import android.app.Application
import com.example.swipebyte.ui.data.models.MockUserQueryable

/**
 * Application class for test purposes
 * Include this in your androidTest source folder
 */
class TestSwipeByteApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable test mode for mocks
        MockUserQueryable.isTestMode = true
    }
}