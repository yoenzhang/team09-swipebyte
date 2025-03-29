package com.example.swipebyte.viewmodel

import com.example.swipebyte.ui.viewmodel.MyLikesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MyLikesViewModelTest {

    private lateinit var viewModel: MyLikesViewModel

    @Before
    fun setup() {
        viewModel = MyLikesViewModel()
    }

    @Test
    fun myLikesViewModel_initialState_isCorrect() {
        // Initial liked restaurants should be empty
        val initialLikes = viewModel.likedRestaurants.value
        assertTrue(initialLikes.isEmpty())

        // Initial timestamps map should be empty
        val timestampsMap = viewModel.timestampsMap.value
        assertTrue(timestampsMap.isEmpty())

        // Initial friend likes map should be empty
        val friendLikes = viewModel.friendLikesMap.value
        assertTrue(friendLikes.isEmpty())
    }

    @Test
    fun myLikesViewModel_initializesCorrectly() {
        // Just verify the viewModel was created successfully
        assertNotNull(viewModel)
    }
}