package com.example.glarmto.ui.routines

import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glarmto.GlarmToApplication
import com.example.glarmto.data.util.ExercisePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val viewModel: RoutineViewModel = viewModel(
        factory = RoutineViewModelFactory(application.repository)
    )

    val routines by viewModel.routines.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_routine))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.my_customs_routines), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.create_presets_templates_to_quickly_start), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider()

            if (routines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_routines_created_yet_click_to_add_one))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(routines) { routine ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(routine.routineName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                
                                val exercises = routine.exercises.split("|")
                                Spacer(modifier = Modifier.height(8.dp))
                                exercises.forEachIndexed { index, ex ->
                                    Text("${index + 1}. $ex", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(80.dp)) // Padding for FAB
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, exercises -> 
                viewModel.addRoutine(name, exercises)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineDialog(onDismiss: () -> Unit, onSave: (String, List<String>) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var routineName by remember { mutableStateOf("") }
    var selectedExercises by remember { mutableStateOf(listOf<String>()) }
    var exerciseSearchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (step == 1) R.string.name_your_routine else R.string.select_exercises))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step == 1) {
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = { Text(stringResource(R.string.routine_name_e_g_push_day)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = exerciseSearchQuery,
                        onValueChange = { exerciseSearchQuery = it },
                        label = { Text(stringResource(R.string.search_exercise)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    val filtered = ExercisePresets.allExercises.filter { 
                        it.contains(exerciseSearchQuery, ignoreCase = true) && !selectedExercises.contains(it)
                    }.take(5)

                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(filtered) { ex ->
                            Text(
                                text = ex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExercises = selectedExercises + ex
                                        exerciseSearchQuery = "" // Reset search
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Divider()
                    Text(stringResource(R.string.routine_selected), fontWeight = FontWeight.Bold)
                    if (selectedExercises.isEmpty()) Text(stringResource(R.string.none_tap_above_to_add))
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                         items(selectedExercises) { ex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ex)
                                IconButton(onClick = { selectedExercises = selectedExercises.filter { it != ex } }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove))
                                }
                            }
                         }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (step == 1 && routineName.isNotBlank()) step = 2
                else if (step == 2 && selectedExercises.isNotEmpty()) onSave(routineName, selectedExercises)
            }) {
                Text(stringResource(if (step == 1) R.string.next else R.string.save_routine))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
