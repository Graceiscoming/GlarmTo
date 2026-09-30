package com.example.glarmto.ui.workout

import androidx.compose.ui.platform.LocalContext
import com.example.glarmto.data.util.ResourceTexts
import com.example.glarmto.data.util.labelRes
import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glarmto.data.util.AiGeneratorContext
import com.example.glarmto.data.util.Equipment
import com.example.glarmto.data.util.GeneratedWorkout
import com.example.glarmto.data.util.MuscleGroup
import com.example.glarmto.data.util.WorkoutGenerator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkoutGeneratorSheet(
    onDismissRequest: () -> Unit,
    onWorkoutGenerated: (GeneratedWorkout) -> Unit,
    fetchContext: suspend () -> AiGeneratorContext
) {
    var timeMins by remember { mutableStateOf(45f) }

    val selectedEquipment = remember { mutableStateListOf<Equipment>() }
    val selectedMuscles = remember { mutableStateListOf<MuscleGroup>() }

    var previewWorkout by remember { mutableStateOf<GeneratedWorkout?>(null) }
    val appContext = LocalContext.current.applicationContext as android.app.Application
    val generatorTexts = remember(appContext) { ResourceTexts(appContext) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        val workout = previewWorkout
        if (workout == null) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.ai_workout_generator),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.tell_us_what_you_have_and_our_offline_ai_w))

                Divider()

                // 1. Time
                Text(stringResource(R.string.f_available_time_minutes, timeMins.roundToInt()), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Slider(
                    value = timeMins,
                    onValueChange = { timeMins = it },
                    valueRange = 10f..120f,
                    steps = 11 // 10, 20, 30...
                )

                // 2. Equipment
                Text(stringResource(R.string.available_equipment), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Equipment.values()) { eq ->
                        FilterChip(
                            selected = selectedEquipment.contains(eq),
                            onClick = {
                                if (selectedEquipment.contains(eq)) selectedEquipment.remove(eq)
                                else selectedEquipment.add(eq)
                            },
                            label = { Text(stringResource(eq.labelRes())) }
                        )
                    }
                }

                // 3. Muscle Focus
                Text(stringResource(R.string.target_muscles), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MuscleGroup.values()) { mc ->
                        FilterChip(
                            selected = selectedMuscles.contains(mc),
                            onClick = {
                                if (selectedMuscles.contains(mc)) selectedMuscles.remove(mc)
                                else selectedMuscles.add(mc)
                            },
                            label = { Text(stringResource(mc.labelRes())) }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            previewWorkout = WorkoutGenerator.generateWorkout(
                                texts = generatorTexts,
                                availableTimeMins = timeMins.roundToInt(),
                                equipmentConstraints = selectedEquipment,
                                focusMuscles = selectedMuscles,
                                context = fetchContext()
                            )
                            isGenerating = false
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.generate_workout), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    workout.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.f_minutes_exercises, workout.totalTimeMins, workout.exercises.size))

                workout.warnings.forEach { warning ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            warning,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }

                workout.insights.forEach { insight ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(
                            insight,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 13.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workout.exercises) { ex ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    val weightText = ex.suggestedWeight?.let { " @ ${it}kg" } ?: ""
                                    Text("${ex.targetSets} × ${ex.targetReps}$weightText", fontSize = 14.sp)
                                }
                                ex.note?.let {
                                    Text(
                                        it,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { previewWorkout = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.regenerate))
                    }
                    Button(
                        onClick = { onWorkoutGenerated(workout) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.start_workout), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
