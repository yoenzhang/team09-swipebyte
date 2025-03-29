package com.example.swipebyte.viewmodel

import com.example.swipebyte.ui.viewmodel.PreferencesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferencesViewModelTest {

    private lateinit var viewModel: PreferencesViewModel

    @Before
    fun setup() {
        viewModel = PreferencesViewModel()
    }

    @Test
    fun getSelectedCuisines_returnsEmptyListByDefault() {
        val cuisines = viewModel.getSelectedCuisines()
        assertTrue(cuisines.isEmpty())
    }

    @Test
    fun getSelectedPriceRange_returnsEmptyListByDefault() {
        val priceRanges = viewModel.getSelectedPriceRange()
        assertTrue(priceRanges.isEmpty())
    }


}