package com.example.swipebyte.ui.pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swipebyte.ui.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesView(
    navController: NavController,
    preferencesViewModel: PreferencesViewModel
) {
    val cuisineTypes = listOf("American", "Chinese", "Italian", "Mexican", "Japanese",
        "Indian", "Thai", "Mediterranean", "French", "Korean",
        "Vietnamese", "Greek", "BBQ", "Seafood", "Vegetarian",
        "Vegan", "Fast Food", "Pizza", "Burger", "Sushi")

    // Load and manage state for the preferences
    val selectedCuisines = remember { mutableStateListOf<String>().apply { addAll(preferencesViewModel.getSelectedCuisines()) } }
    val priceRanges = listOf("$", "$$", "$$$", "$$$$")
    val selectedPriceRanges = remember { mutableStateListOf<String>().apply { addAll(preferencesViewModel.getSelectedPriceRange()) } }
    var isSaving by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()


    Log.d("selectedCuuisines", selectedCuisines.toString())
    // Trigger load preferences on first composition
    LaunchedEffect(true) {
        preferencesViewModel.loadPreferences {
            // After loading, update the state (if any changes are needed)
            selectedCuisines.clear()
            selectedCuisines.addAll(preferencesViewModel.getSelectedCuisines())
            selectedPriceRanges.clear()
            selectedPriceRanges.addAll(preferencesViewModel.getSelectedPriceRange())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Preferences") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Price Range Card
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Price Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select your preferred price range for restaurants", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        priceRanges.forEach { priceRange ->
                            val isSelected = selectedPriceRanges.contains(priceRange)
                            Card(
                                modifier = Modifier.weight(1f).padding(4.dp).selectable(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedPriceRanges.remove(priceRange)
                                        } else {
                                            selectedPriceRanges.add(priceRange)
                                        }
                                    }
                                ),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    Text(priceRange, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // Cuisine Types Card
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cuisine Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select your preferred cuisine types", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    cuisineTypes.chunked(2).forEach { rowCuisines ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCuisines.forEach { cuisine ->
                                val isSelected = selectedCuisines.contains(cuisine)
                                Row(modifier = Modifier.weight(1f).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                        if (checked) selectedCuisines.add(cuisine) else selectedCuisines.remove(cuisine)
                                    })
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(cuisine, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            if (rowCuisines.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isSaving = true
                    preferencesViewModel.savePreferences(
                        selectedCuisines = selectedCuisines.toList(),
                        priceRange = selectedPriceRanges.toList()
                    ) { success ->
                        isSaving = false
                        if (success) showSavedMessage = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Preferences")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (showSavedMessage) {
                AlertDialog(
                    onDismissRequest = {
                        showSavedMessage = false
                        navController.popBackStack()
                    },
                    title = { Text("Success") },
                    text = { Text("Your preferences have been saved.") },
                    confirmButton = {
                        Button(onClick = {
                            showSavedMessage = false
                            navController.popBackStack()
                        }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
