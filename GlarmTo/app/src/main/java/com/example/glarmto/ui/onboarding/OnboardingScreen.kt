package com.example.glarmto.ui.onboarding

import com.example.glarmto.ui.util.goalLabel
import com.example.glarmto.data.util.Goals
import com.example.glarmto.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glarmto.GlarmToApplication
import com.example.glarmto.data.local.entity.UserEntity
import com.example.glarmto.data.util.HealthCalculator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as GlarmToApplication
    val repository = application.repository
    val coroutineScope = rememberCoroutineScope()
    
    val username = repository.getCurrentUser() ?: ""

    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var workoutDays by remember { mutableStateOf(3f) }
    var goal by remember { mutableStateOf("Maintain") }

    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(stringResource(R.string.f_welcome, username), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.let_s_set_up_your_profile_to_calculate_you), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(
                selected = isMale,
                onClick = { isMale = true },
                label = { Text(stringResource(R.string.male)) }
            )
            FilterChip(
                selected = !isMale,
                onClick = { isMale = false },
                label = { Text(stringResource(R.string.female)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text(stringResource(R.string.age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text(stringResource(R.string.weight_kg)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text(stringResource(R.string.height_cm)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.primary_goal), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Goals.all.forEach { g ->
                FilterChip(
                    selected = goal == g,
                    onClick = { goal = g },
                    label = { Text(goalLabel(g)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.f_workout_days_per_week, workoutDays.roundToInt()), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Slider(
            value = workoutDays,
            onValueChange = { workoutDays = it },
            valueRange = 0f..7f,
            steps = 6,
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            Text(stringResource(R.string.please_enter_valid_numbers_for_all_fields), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val a = age.trim().toIntOrNull() ?: 0
                val w = weight.trim().toDoubleOrNull() ?: 0.0
                val h = height.trim().toDoubleOrNull() ?: 0.0

                if (a > 0 && w > 0 && h > 0) {
                    showError = false
                    val days = workoutDays.roundToInt()
                    val tdeeResult = HealthCalculator.calculateTdee(a, w, h, isMale, days, goal)

                    coroutineScope.launch {
                        val user = UserEntity(
                            username = username,
                            age = a,
                            isMale = isMale,
                            weight = w,
                            height = h,
                            dailyGoal = tdeeResult, // Calculated with goal and days
                            profileSetup = true,
                            goal = goal,
                            workoutDays = days
                        )
                        repository.updateUser(user)
                        onComplete()
                    }
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.save_profile_continue), fontSize = 16.sp)
        }
    }
    }
}
