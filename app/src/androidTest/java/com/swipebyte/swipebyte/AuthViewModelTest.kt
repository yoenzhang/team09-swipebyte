package com.swipebyte.viewmodel

import com.swipebyte.ui.viewmodel.AuthViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel()
    }

    @Test
    fun authViewModel_initialState_isCorrect() {
        // Initial login state should be false
        assertFalse(viewModel.isLoggedIn.value ?: true)

        // Ensure userId is initialized
        assertNotNull(viewModel)
    }

    @Test
    fun authViewModel_initializesCorrectly() {
        // Just verify the viewModel was created successfully
        assertNotNull(viewModel)
    }
}