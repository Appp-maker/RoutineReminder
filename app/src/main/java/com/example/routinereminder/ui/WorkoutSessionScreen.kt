package com.example.routinereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.routinereminder.R
import kotlinx.coroutines.delay

private enum class WorkoutPhase {
    Ready,
    Set,
    Rest,
    Completed
}

@Composable
fun WorkoutSessionScreen(
    navController: NavController,
    planId: String,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val plan = uiState.plans.firstOrNull { it.id == planId }

    if (plan == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.workout_plan_missing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text(stringResource(R.string.workout_session_close_action))
                }
            }
        }
        return
    }

    if (plan.exercises.isEmpty()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.workout_plan_empty_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text(stringResource(R.string.workout_session_close_action))
                }
            }
        }
        return
    }

    var workoutStarted by remember { mutableStateOf(false) }
    var workoutPhase by remember { mutableStateOf(WorkoutPhase.Ready) }
    var workoutCompleted by remember { mutableStateOf(false) }
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var currentSetIndex by remember { mutableStateOf(0) }
    var totalElapsedSeconds by remember { mutableStateOf(0) }
    var setRemainingSeconds by remember { mutableStateOf<Int?>(null) }
    var restRemainingSeconds by remember { mutableStateOf<Int?>(null) }

    val exercise = plan.exercises[currentExerciseIndex]
    val totalSets = (exercise.sets ?: 1).coerceAtLeast(1)
    val imageModels = remember(exercise) {
        buildList {
            exercise.gifUrl?.let { add(it) }
            addAll(exercise.imageUrls)
        }
    }
    var currentImageIndex by remember(imageModels) { mutableStateOf(0) }

    fun startSet() {
        workoutPhase = WorkoutPhase.Set
        setRemainingSeconds = exercise.durationMinutes
        restRemainingSeconds = null
    }

    fun startRest() {
        workoutPhase = WorkoutPhase.Rest
        restRemainingSeconds = exercise.restSeconds?.takeIf { it > 0 }
        setRemainingSeconds = null
    }

    fun advanceToNextSetOrExercise() {
        if (currentSetIndex + 1 < totalSets) {
            currentSetIndex += 1
            startSet()
        } else if (currentExerciseIndex + 1 < plan.exercises.size) {
            currentExerciseIndex += 1
            currentSetIndex = 0
            workoutPhase = WorkoutPhase.Ready
        } else {
            workoutPhase = WorkoutPhase.Completed
            workoutCompleted = true
        }
    }

    fun handleAdvance() {
        if (!workoutStarted) {
            workoutStarted = true
            workoutPhase = WorkoutPhase.Ready
            return
        }
        when (workoutPhase) {
            WorkoutPhase.Ready -> startSet()
            WorkoutPhase.Set -> startRest()
            WorkoutPhase.Rest -> advanceToNextSetOrExercise()
            WorkoutPhase.Completed -> Unit
        }
    }

    fun handleBack() {
        if (!workoutStarted || workoutCompleted) {
            return
        }
        when (workoutPhase) {
            WorkoutPhase.Ready -> {
                when {
                    currentSetIndex > 0 -> {
                        currentSetIndex -= 1
                        workoutPhase = WorkoutPhase.Rest
                        restRemainingSeconds = exercise.restSeconds?.takeIf { it > 0 }
                        setRemainingSeconds = null
                    }
                    currentExerciseIndex > 0 -> {
                        currentExerciseIndex -= 1
                        val previousExercise = plan.exercises[currentExerciseIndex]
                        val previousTotalSets = (previousExercise.sets ?: 1).coerceAtLeast(1)
                        currentSetIndex = previousTotalSets - 1
                        workoutPhase = WorkoutPhase.Rest
                        restRemainingSeconds = previousExercise.restSeconds?.takeIf { it > 0 }
                        setRemainingSeconds = null
                    }
                }
            }
            WorkoutPhase.Set -> {
                workoutPhase = WorkoutPhase.Ready
                setRemainingSeconds = null
            }
            WorkoutPhase.Rest -> {
                workoutPhase = WorkoutPhase.Set
                restRemainingSeconds = null
                setRemainingSeconds = exercise.durationMinutes?.let { minutes -> minutes * 60 }
            }
            WorkoutPhase.Completed -> Unit
        }
    }

    LaunchedEffect(workoutStarted, workoutCompleted) {
        if (workoutStarted && !workoutCompleted) {
            while (true) {
                delay(1000)
                totalElapsedSeconds += 1
            }
        }
    }

    LaunchedEffect(workoutPhase, currentExerciseIndex, currentSetIndex) {
        if (workoutPhase == WorkoutPhase.Set) {
            val duration = exercise.durationMinutes
            if (duration != null && duration > 0) {
                setRemainingSeconds = duration
                while ((setRemainingSeconds ?: 0) > 0 && workoutPhase == WorkoutPhase.Set) {
                    delay(1000)
                    setRemainingSeconds = (setRemainingSeconds ?: 0) - 1
                }
                if ((setRemainingSeconds ?: 0) <= 0 && workoutPhase == WorkoutPhase.Set) {
                    startRest()
                }
            } else {
                setRemainingSeconds = null
            }
        } else {
            setRemainingSeconds = null
        }
    }

    LaunchedEffect(workoutPhase, currentExerciseIndex, currentSetIndex) {
        if (workoutPhase == WorkoutPhase.Rest) {
            val restSeconds = exercise.restSeconds?.takeIf { it > 0 }
            if (restSeconds != null) {
                restRemainingSeconds = restSeconds
                while ((restRemainingSeconds ?: 0) > 0 && workoutPhase == WorkoutPhase.Rest) {
                    delay(1000)
                    restRemainingSeconds = (restRemainingSeconds ?: 0) - 1
                }
                if ((restRemainingSeconds ?: 0) <= 0 && workoutPhase == WorkoutPhase.Rest) {
                    advanceToNextSetOrExercise()
                }
            } else {
                restRemainingSeconds = null
            }
        } else {
            restRemainingSeconds = null
        }
    }

    val timerActiveColor = MaterialTheme.colorScheme.secondaryContainer
    val timerInactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val descriptionText = exercise.instructions.takeIf { it.isNotEmpty() }
        ?.joinToString(separator = " ")
        ?: stringResource(R.string.workout_session_description_unavailable)

    LaunchedEffect(imageModels) {
        if (imageModels.size > 1) {
            while (true) {
                delay(2000)
                currentImageIndex = (currentImageIndex + 1) % imageModels.size
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(enabled = workoutStarted && !workoutCompleted) { handleBack() }
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.01f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(enabled = !workoutCompleted) { handleAdvance() }
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.01f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.workout_session_total_timer,
                                    formatDuration(totalElapsedSeconds)
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.workout_session_close_action)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${exercise.bodyPart} - ${exercise.target} - ${exercise.equipment}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    WorkoutExerciseMedia(
                        imageModels = imageModels,
                        currentImageIndex = currentImageIndex,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val setDisplay = setRemainingSeconds?.let(::formatDuration)
                            ?: exercise.durationMinutes?.let(::formatDuration)
                            ?: stringResource(R.string.workout_timer_idle)
                        val restDisplay = restRemainingSeconds?.let(::formatDuration)
                            ?: exercise.restSeconds?.takeIf { it > 0 }?.let(::formatDuration)
                            ?: stringResource(R.string.workout_timer_idle)
                        WorkoutTimerChip(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.workout_timer_set_label),
                            time = setDisplay,
                            isActive = workoutPhase == WorkoutPhase.Set,
                            activeColor = timerActiveColor,
                            inactiveColor = timerInactiveColor
                        )
                        WorkoutTimerChip(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.workout_timer_rest_label),
                            time = restDisplay,
                            isActive = workoutPhase == WorkoutPhase.Rest,
                            activeColor = timerActiveColor,
                            inactiveColor = timerInactiveColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WorkoutStatChip(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.workout_session_sets_label),
                            value = stringResource(
                                R.string.workout_session_sets_value,
                                currentSetIndex + 1,
                                totalSets
                            )
                        )
                        WorkoutStatChip(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.workout_session_reps_title),
                            value = exercise.repetitions?.toString()
                                ?: stringResource(R.string.workout_session_value_placeholder)
                        )
                    }

                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val instructionText = when {
                        workoutCompleted -> stringResource(R.string.workout_session_complete_message)
                        !workoutStarted -> stringResource(R.string.workout_session_tap_start)
                        workoutPhase == WorkoutPhase.Ready -> stringResource(R.string.workout_session_tap_set)
                        workoutPhase == WorkoutPhase.Set -> stringResource(R.string.workout_session_tap_rest)
                        workoutPhase == WorkoutPhase.Rest -> stringResource(R.string.workout_session_tap_next)
                        else -> ""
                    }
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    if (workoutCompleted) {
                        Button(onClick = { navController.popBackStack() }) {
                            Text(stringResource(R.string.workout_session_close_action))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutExerciseMedia(
    imageModels: List<String>,
    currentImageIndex: Int,
    modifier: Modifier = Modifier
) {
    val currentModel = imageModels.getOrNull(currentImageIndex)
    Surface(
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (currentModel == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.workout_session_media_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentModel)
                    .build(),
                contentDescription = stringResource(R.string.workout_exercise_gif_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.workout_session_media_loading),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTimerChip(
    modifier: Modifier = Modifier,
    title: String,
    time: String,
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    inactiveColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        color = if (isActive) activeColor else inactiveColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WorkoutStatChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
