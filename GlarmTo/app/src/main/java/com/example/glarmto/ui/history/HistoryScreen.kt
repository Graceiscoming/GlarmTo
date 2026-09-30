package com.example.glarmto.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glarmto.GlarmToApplication
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun HistoryScreen(isMonthly: Boolean = false, onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(application, application.repository)
    )

    LaunchedEffect(isMonthly) {
        viewModel.setViewMode(isMonthly)
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val workouts by viewModel.workouts.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val nutrition by viewModel.nutrition.collectAsState()

    val workoutsBySession = workouts.groupBy { it.sessionId }
    val matchedSessionIds = mutableSetOf<Int?>()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setSelectedDate(it)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMonthly) "Monthly Log History" else "Daily Log History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val formatStr = if (isMonthly) "MMMM yyyy" else "EEEE, dd MMMM yyyy"
            val dateStr = SimpleDateFormat(formatStr, Locale.getDefault()).format(Date(selectedDate))
            Text(dateStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (workouts.isEmpty() && nutrition.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs for this date", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (workouts.isNotEmpty()) {
                        item {
                            Text("Workouts", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        // Show Sessions
                        val groupedSessions = if (isMonthly) {
                            sessions.groupBy {
                                val cal = Calendar.getInstance().apply { timeInMillis = it.dateInMillis }
                                cal.apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }.toSortedMap(compareByDescending { it })
                        } else {
                            mapOf(selectedDate to sessions)
                        }

                        groupedSessions.forEach { (dateMs, dailySessions) ->
                            if (isMonthly && dailySessions.isNotEmpty()) {
                                item(key = "date_header_sessions_$dateMs") {
                                    val dailyStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(dateMs))
                                    Text(dailyStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                            }
                            items(dailySessions) { session ->
                                val sessionWorkouts = workoutsBySession[session.sessionId] ?: emptyList()
                                matchedSessionIds.add(session.sessionId)
                                var expanded by remember { mutableStateOf(false) }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { expanded = !expanded }, 
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(session.sessionName.ifBlank { "Session ${session.sessionId}" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                val mm = session.durationSeconds / 60
                                                val ss = session.durationSeconds % 60
                                                Text("${sessionWorkouts.size} sets • Duration: $mm min $ss sec", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (session.exhaustionLevel > 0) {
                                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Text(session.exhaustionLevel.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Icon(
                                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                    contentDescription = "Expand"
                                                )
                                            }
                                        }
                                        
                                        AnimatedVisibility(visible = expanded) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (session.notes.isNotBlank()) {
                                                    Text("Notes:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text(session.notes, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                                }

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text("Exhaustion", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                                        Row { (1..5).forEach { i -> Icon(Icons.Filled.Star, null, modifier = Modifier.size(12.dp), tint = if (i <= session.exhaustionLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) } }
                                                    }
                                                    Column {
                                                        Text("Satisfaction", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                                        Row { (1..5).forEach { i -> Icon(Icons.Filled.Star, null, modifier = Modifier.size(12.dp), tint = if (i <= session.satisfactionLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) } }
                                                    }
                                                }
                                                
                                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                                                sessionWorkouts.forEach { workout ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Column {
                                                            Text(workout.exerciseName, fontWeight = FontWeight.SemiBold)
                                                            Text("${workout.weight} kg x ${workout.reps} reps", fontSize = 14.sp)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                    if (workout != sessionWorkouts.last()) Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                                }
                                                Spacer(Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Show Uncategorized
                        val unmatchedIds = workoutsBySession.keys.filter { it !in matchedSessionIds }
                        if (unmatchedIds.isNotEmpty()) {
                            val allUnmatched = unmatchedIds.flatMap { workoutsBySession[it] ?: emptyList() }
                            val groupedUnmatched = if (isMonthly) {
                                allUnmatched.groupBy {
                                    val cal = Calendar.getInstance().apply { timeInMillis = it.dateInMillis }
                                    cal.apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }.toSortedMap(compareByDescending { it })
                            } else {
                                mapOf(selectedDate to allUnmatched)
                            }

                            groupedUnmatched.forEach { (dateMs, dailyUnmatched) ->
                                if (dailyUnmatched.isNotEmpty()) {
                                    item {
                                        if (isMonthly) {
                                            val dailyStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(dateMs))
                                            Text("$dailyStr - Uncategorized", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { expanded = !expanded }, 
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Uncategorized Sets", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        Text("${dailyUnmatched.size} sets", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                                    }
                                                    Icon(
                                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                        contentDescription = "Expand"
                                                    )
                                                }
                                                AnimatedVisibility(visible = expanded) {
                                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        dailyUnmatched.forEach { workout ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                                Column {
                                                                    Text(workout.exerciseName, fontWeight = FontWeight.SemiBold)
                                                                    Text("${workout.weight} kg x ${workout.reps} reps", fontSize = 14.sp)
                                                                }
                                                                IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                                }
                                                            }
                                                            if (workout != dailyUnmatched.last()) Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } // End item
                                } // End if (dailyUnmatched.isNotEmpty())
                            } // End groupedUnmatched.forEach
                        } // End if (unmatchedIds.isNotEmpty())
                    } // End if (workouts.isNotEmpty())

                    if (nutrition.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("Nutrition", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        val groupedNutrition = if (isMonthly) {
                            nutrition.groupBy {
                                val cal = Calendar.getInstance().apply { timeInMillis = it.dateInMillis }
                                cal.apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }.toSortedMap(compareByDescending { it })
                        } else {
                            mapOf(selectedDate to nutrition)
                        }

                        groupedNutrition.forEach { (dateMs, dailyNutrition) ->
                            if (isMonthly && dailyNutrition.isNotEmpty()) {
                                item(key = "date_header_nutrition_$dateMs") {
                                    val dailyStr = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(dateMs))
                                    Text(dailyStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                            }
                            items(dailyNutrition) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.foodName, fontWeight = FontWeight.Bold)
                                            Text("${item.calories} kcal")
                                        }
                                    }
                                }
                            } // End items
                        } // End groupedNutrition.forEach
                    } // End if (nutrition.isNotEmpty())
                } // End LazyColumn
            } // End else branch
        } // End Column
    } // End Scaffold
} // End HistoryScreen
