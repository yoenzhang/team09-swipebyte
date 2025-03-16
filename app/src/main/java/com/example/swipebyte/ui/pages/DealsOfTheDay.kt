package com.example.swipebyte.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.twotone.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.swipebyte.ui.db.repository.DailyDeal
import com.example.swipebyte.ui.db.repository.DailyDealsRepository
import kotlinx.coroutines.delay
import java.util.*
import androidx.compose.material3.OutlinedTextFieldDefaults

// Function to extract start time from a deal description string
fun extractStartTime(dealStr: String): String? {
    val timeRangePattern = "^([\\d:]+[ap]m)\\s*-\\s*[\\d:]+[ap]m:.*$".toRegex(RegexOption.IGNORE_CASE)
    val match = timeRangePattern.find(dealStr)
    return match?.groupValues?.get(1)
}

// Function to see if a deal matches the selected hour
fun dealMatchesHour(dealStr: String, selectedHour: String): Boolean {
    if (selectedHour == "All Day") return true

    val extractedStartTime = extractStartTime(dealStr) ?: return false

    // Parse the hour from the extracted start time
    val hourRegex = "(\\d+):?\\d*([ap]m)".toRegex(RegexOption.IGNORE_CASE)
    val hourMatch = hourRegex.find(extractedStartTime)

    if (hourMatch != null) {
        val (hourStr, amPm) = hourMatch.destructured
        val hour = hourStr.toIntOrNull() ?: return false
        val extractedAmPm = amPm.lowercase()

        // Parse the selected hour
        val selectedHourMatch = hourRegex.find(selectedHour)

        if (selectedHourMatch != null) {
            val (selectedHourStr, selectedAmPm) = selectedHourMatch.destructured
            val selectedHourInt = selectedHourStr.toIntOrNull() ?: return false
            val selectedAmPmLower = selectedAmPm.lowercase()

            // Convert to 24-hour for comparison
            val extractedHour24 = when {
                extractedAmPm == "pm" && hour != 12 -> hour + 12
                extractedAmPm == "am" && hour == 12 -> 0
                else -> hour
            }

            val selectedHour24 = when {
                selectedAmPmLower == "pm" && selectedHourInt != 12 -> selectedHourInt + 12
                selectedAmPmLower == "am" && selectedHourInt == 12 -> 0
                else -> selectedHourInt
            }

            // For hourly filtering, check if the deal starts at the selected hour
            return extractedHour24 == selectedHour24
        }
    }

    return false
}

// Function to extract just the deal description (after the colon in time range)
fun extractDealDescription(dealStr: String): String {
    val dealPattern = "^[\\d:]+[ap]m\\s*-\\s*[\\d:]+[ap]m:\\s*(.+)$".toRegex(RegexOption.IGNORE_CASE)
    val match = dealPattern.find(dealStr)

    return if (match != null) {
        match.groupValues[1].trim()
    } else if (dealStr.contains(":")) {
        dealStr.substringAfter(":").trim()
    } else {
        dealStr
    }
}

// Function to extract the full time range from a deal string
fun extractTimeRange(dealStr: String): String {
    val timeRangePattern = "^([\\d:]+[ap]m\\s*-\\s*[\\d:]+[ap]m):.*$".toRegex(RegexOption.IGNORE_CASE)
    val match = timeRangePattern.find(dealStr)
    return match?.groupValues?.get(1) ?: "Other Times"
}

fun dealIsActiveDuring(dealStr: String, selectedHour: String): Boolean {
    if (selectedHour == "All Day") return true

    // Extract the time range from the deal string
    val timeRangePattern = "^([\\d:]+[ap]m)\\s*-\\s*([\\d:]+[ap]m):.*$".toRegex(RegexOption.IGNORE_CASE)
    val match = timeRangePattern.find(dealStr) ?: return false

    if (match.groupValues.size < 3) return false

    val startTimeStr = match.groupValues[1].trim()
    val endTimeStr = match.groupValues[2].trim()

    // Parse the hour from the times
    val hourRegex = "(\\d+):?(\\d*)([ap]m)".toRegex(RegexOption.IGNORE_CASE)

    // Parse start time
    val startMatch = hourRegex.find(startTimeStr) ?: return false
    val (startHourStr, startMinStr, startAmPm) = startMatch.destructured
    val startHour = startHourStr.toIntOrNull() ?: return false
    val startMin = if (startMinStr.isEmpty()) 0 else startMinStr.toIntOrNull() ?: 0

    // Parse end time
    val endMatch = hourRegex.find(endTimeStr) ?: return false
    val (endHourStr, endMinStr, endAmPm) = endMatch.destructured
    val endHour = endHourStr.toIntOrNull() ?: return false
    val endMin = if (endMinStr.isEmpty()) 0 else endMinStr.toIntOrNull() ?: 0

    // Parse selected hour
    val selectedMatch = hourRegex.find(selectedHour) ?: return false
    val (selectedHourStr, selectedMinStr, selectedAmPm) = selectedMatch.destructured
    val selectedHourInt = selectedHourStr.toIntOrNull() ?: return false
    val selectedMin = if (selectedMinStr.isEmpty()) 0 else selectedMinStr.toIntOrNull() ?: 0

    // Convert all to 24-hour format for easier comparison
    val start24Hour = convertTo24Hour(startHour, startAmPm.lowercase())
    val end24Hour = convertTo24Hour(endHour, endAmPm.lowercase())
    val selected24Hour = convertTo24Hour(selectedHourInt, selectedAmPm.lowercase())

    // Convert to minutes since midnight for comparison
    val startMinutes = start24Hour * 60 + startMin
    val endMinutes = end24Hour * 60 + endMin
    val selectedMinutes = selected24Hour * 60 + selectedMin

    // Check if the selected time falls within the deal time range
    return if (endMinutes > startMinutes) {
        // Normal case: e.g., 5pm-9pm
        selectedMinutes in startMinutes..endMinutes
    } else {
        // Overnight case: e.g., 10pm-2am
        selectedMinutes >= startMinutes || selectedMinutes <= endMinutes
    }
}

// Helper function to convert 12-hour time to 24-hour format
fun convertTo24Hour(hour: Int, amPm: String): Int {
    return when {
        amPm == "am" && hour == 12 -> 0         // 12am is 0 in 24-hour
        amPm == "am" -> hour                    // Other am hours stay the same
        amPm == "pm" && hour == 12 -> 12        // 12pm is 12 in 24-hour
        amPm == "pm" -> hour + 12               // Other pm hours add 12
        else -> hour                            // Default fallback
    }
}





@Composable
fun CompactDropdownSelector(
    label: String,
    icon: @Composable () -> Unit,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DealsOfTheDayView(navController: NavController) {
    // Create an instance of DailyDealsRepository
    val dailyDealsRepo = DailyDealsRepository()

    // State holding the list of deals
    val dailyDealsList = remember { mutableStateListOf<DailyDeal>() }

    // Filtered deals list
    val filteredDealsList = remember { mutableStateListOf<DailyDeal>() }

    // Loading state while fetching data from Firestore
    var isLoading by remember { mutableStateOf(true) }

    // State to track selected day
    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    // Get current day of week
    val calendar = Calendar.getInstance()
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY is 1, we need 0-based index
    var selectedDay by remember { mutableStateOf(daysOfWeek[currentDayOfWeek]) }

    // Search text for restaurant name
    var searchText by remember { mutableStateOf("") }

    // State to track whether to show the search field
    var showSearch by remember { mutableStateOf(false) }

    // State for dialog
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedDeal by remember { mutableStateOf<DailyDeal?>(null) }

    // Create hourly time options from 6am to midnight
    val hourlyTimeOptions = remember {
        val options = mutableListOf("All Day")
        for (hour in 6..23) { // 6am to 11pm
            val amPm = if (hour < 12) "am" else "pm"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            options.add("$displayHour:00$amPm")
        }
        options
    }

    var selectedStartTime by remember { mutableStateOf("All Day") }

    // Function to update filtered deals list
    fun updateFilteredDeals(
        source: List<DailyDeal>,
        target: MutableList<DailyDeal>,
        day: String,
        startTime: String,
        nameFilter: String
    ) {
        target.clear()
        if (source.isEmpty()) return

        // Apply all filters in sequence
        var filtered = source

        // 1. First filter by day
        filtered = filtered.filter { dailyDeal ->
            dailyDeal.deals[day]?.description?.isNotEmpty() == true
        }

        // 2. Then filter by time if not "All Day"
        if (startTime != "All Day") {
            filtered = filtered.filter { dailyDeal ->
                val dealDescriptions = dailyDeal.deals[day]?.description ?: emptyList()
                dealDescriptions.any { dealStr ->
                    dealIsActiveDuring(dealStr, startTime)
                }
            }
        }

        // 3. Finally, filter by restaurant name if search text is not empty
        if (nameFilter.isNotEmpty()) {
            filtered = filtered.filter { dailyDeal ->
                dailyDeal.name.contains(nameFilter, ignoreCase = true)
            }
        }

        // Add all filtered deals to the target list
        target.addAll(filtered)
    }

    // Load daily deals on first composition
    LaunchedEffect(Unit) {
        try {
            val fetchedDeals = dailyDealsRepo.getDailyDeals()
            dailyDealsList.clear()
            dailyDealsList.addAll(fetchedDeals)

            // Initialize filtered list with all deals for the current day
            updateFilteredDeals(
                dailyDealsList,
                filteredDealsList,
                selectedDay,
                selectedStartTime,
                searchText
            )
        } catch (e: Exception) {
            //
        } finally {
            // Small delay to better visualize the loading state, if needed.
            delay(300)
            isLoading = false
        }
    }

    // Filter deals whenever selection changes
    LaunchedEffect(selectedDay, selectedStartTime, searchText, dailyDealsList.size) {
        updateFilteredDeals(
            dailyDealsList,
            filteredDealsList,
            selectedDay,
            selectedStartTime,
            searchText
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Deals of the Day",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search field
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search restaurant name...") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Compact filters side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day selector
            CompactDropdownSelector(
                label = "Day",
                icon = {
                    Icon(
                        imageVector = Icons.TwoTone.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                value = selectedDay,
                options = daysOfWeek,
                onOptionSelected = { selectedDay = it },
                modifier = Modifier.weight(1f)
            )

            // Time selector
            CompactDropdownSelector(
                label = "During",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                value = selectedStartTime,
                options = hourlyTimeOptions,
                onOptionSelected = { selectedStartTime = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            // Show loading indicator
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredDealsList.isEmpty()) {
            // Show empty state if no deals found
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No deals found",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchText.isNotEmpty()) {
                        Text("No restaurants matching \"$searchText\"")

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { searchText = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Clear Search")
                        }
                    } else {
                        Text(
                            text = "for $selectedDay" +
                                    if (selectedStartTime != "All Day") " during $selectedStartTime" else ""
                        )
                    }
                }
            }
        } else {
            // Display deals in a LazyColumn (full width)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredDealsList) { deal ->
                    DailyDealCard(
                        deal = deal,
                        selectedDay = selectedDay,
                        selectedStartTime = selectedStartTime,
                        onCardClick = {
                            selectedDeal = deal
                            showDetailDialog = true
                        }
                    )
                }
            }
        }
    }

    // Detail dialog
    if (showDetailDialog && selectedDeal != null) {
        DealDetailDialog(
            deal = selectedDeal!!,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
fun DailyDealCard(
    deal: DailyDeal,
    selectedDay: String,
    selectedStartTime: String,
    onCardClick: () -> Unit
) {
    // Get deal descriptions for the selected day
    val dealDescriptions = deal.deals[selectedDay]?.description ?: emptyList()

    // Filter descriptions based on hourly selection
    val filteredDescriptions = if (selectedStartTime == "All Day") {
        dealDescriptions
    } else {
        dealDescriptions.filter { dealStr ->
            dealIsActiveDuring(dealStr, selectedStartTime)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Show the first image if available.
                if (deal.images.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = deal.images.first()),
                        contentDescription = "Deal Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Restaurant name overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = deal.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = deal.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Deals content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Available on $selectedDay" +
                            if (selectedStartTime != "All Day") " starting at $selectedStartTime" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display the filtered deals
                if (filteredDescriptions.isNotEmpty()) {
                    filteredDescriptions.forEach { dealStr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Extract just the deal part (after the colon)
                            val dealContent = extractDealDescription(dealStr)
                            val timeRange = extractTimeRange(dealStr)

                            Text(
                                text = "• $timeRange: $dealContent",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No specific deals available at this time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap for all deals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DealDetailDialog(
    deal: DailyDeal,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // State to track which day is selected in the detail view
    val daysWithDeals = deal.deals.filter { (_, dayDeals) ->
        dayDeals.description.isNotEmpty()
    }.keys.toList()

    var selectedTabDay by remember {
        mutableStateOf(daysWithDeals.firstOrNull() ?: "Sunday")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    // Image
                    if (deal.images.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = deal.images.first()),
                            contentDescription = "Restaurant Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Overlay for better visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    // Restaurant info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = deal.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = deal.address,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }

                // Day tabs for navigation
                if (daysWithDeals.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = daysWithDeals.indexOf(selectedTabDay).coerceAtLeast(0),
                        edgePadding = 16.dp
                    ) {
                        daysWithDeals.forEach { day ->
                            Tab(
                                selected = selectedTabDay == day,
                                onClick = { selectedTabDay = day },
                                text = { Text(day) }
                            )
                        }
                    }
                }

                // Deals for the selected day
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Get deals for the selected day
                    val dayDeals = deal.deals[selectedTabDay]?.description ?: emptyList()

                    if (dayDeals.isNotEmpty()) {
                        // Group deals by time range if possible
                        val dealsByTime = dayDeals.groupBy { dealStr ->
                            extractTimeRange(dealStr)
                        }

                        dealsByTime.forEach { (timeRange, dealsForTime) ->
                            item {
                                if (timeRange.isNotEmpty() && timeRange != "Other Times") {
                                    Text(
                                        text = timeRange,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Other Deals",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }

                                // List all deals for this time range
                                dealsForTime.forEach { dealStr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp, horizontal = 16.dp)
                                    ) {
                                        // Extract just the deal description
                                        val dealDescription = extractDealDescription(dealStr)

                                        Text(
                                            text = "• $dealDescription",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "No deals available for $selectedTabDay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }

                    // Website link
                    item {
                        if (deal.url.isNotEmpty()) {
                            Text(
                                text = "Visit Website",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        // Open URL in browser
                                        val browserIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(deal.url)
                                        )
                                        context.startActivity(browserIntent)
                                    }
                                    .padding(vertical = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}