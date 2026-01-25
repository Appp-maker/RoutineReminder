
package com.example.routinereminder.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.routinereminder.R
import com.example.routinereminder.data.DefaultEventSettings
import com.example.routinereminder.data.NO_EVENT_FOOD_COLOR_ARGB
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.ui.components.SeriesColorPicker
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    initialItem: ScheduleItem?,
    defaultEventSettings: DefaultEventSettings,
    useGoogleBackupMode: Boolean, // Added parameter
    eventSetNames: List<String>,
    eventSetColors: List<Int>,
    onOpenDefaultSettings: () -> Unit,
    onDismissRequest: () -> Unit,
    onSave: (ScheduleItem) -> Unit
) {
    val isNewItem = initialItem == null

    var title by remember(initialItem) { mutableStateOf(initialItem?.name ?: "") }
    var notes by remember(initialItem) { mutableStateOf(initialItem?.notes ?: "") }

    var selectedDate by remember(initialItem) {
        val initialEventDate = initialItem?.takeIf { it.isOneTime && it.dateEpochDay != null }?.let { LocalDate.ofEpochDay(it.dateEpochDay!!) }
            ?: initialItem?.startEpochDay?.let { LocalDate.ofEpochDay(it) }
            ?: LocalDate.now()
        mutableStateOf(initialEventDate)
    }

    var selectedTime by remember(initialItem, defaultEventSettings) {
        val time = if (isNewItem) {
            if (defaultEventSettings.startTimeOptionName == "CURRENT_TIME") {
                LocalTime.now()
            } else {
                LocalTime.of(defaultEventSettings.hour, defaultEventSettings.minute)
            }
        } else {
            initialItem?.let { LocalTime.of(it.hour, it.minute) } ?: LocalTime.now()
        }
        mutableStateOf(time)
    }

    var durationInputHours by remember(initialItem, defaultEventSettings) {
        val hours = if (isNewItem) defaultEventSettings.durationHours else (initialItem?.durationMinutes ?: 60) / 60
        mutableStateOf(hours.toString())
    }
    var durationInputMinutes by remember(initialItem, defaultEventSettings) {
        val minutes = if (isNewItem) defaultEventSettings.durationMinutes else (initialItem?.durationMinutes ?: 60) % 60
        mutableStateOf(minutes.toString())
    }

    var isOneTime by remember(initialItem, defaultEventSettings) { mutableStateOf(initialItem?.isOneTime ?: defaultEventSettings.isOneTime) }

    var selectedRepeatDays: Set<DayOfWeek> by remember(initialItem) {
        mutableStateOf(initialItem?.takeIf { !it.isOneTime }?.repeatOnDays ?: emptySet<DayOfWeek>())
    }
    LaunchedEffect(isNewItem, isOneTime, selectedDate, defaultEventSettings) {
        if (isNewItem && !isOneTime && selectedRepeatDays.isEmpty()) {
             selectedRepeatDays = selectedRepeatDays + selectedDate.dayOfWeek
        }
    }

    var repeatEveryWeeks by remember(initialItem) { mutableStateOf(initialItem?.repeatEveryWeeks?.toString() ?: "1") }

    var addToCalendar by remember(initialItem, defaultEventSettings, useGoogleBackupMode) {
        mutableStateOf(
            if (useGoogleBackupMode) false
            else initialItem?.addToCalendarOnSave ?: defaultEventSettings.createCalendarEntry
        )
    }
    LaunchedEffect(useGoogleBackupMode) {
        if (useGoogleBackupMode) {
            addToCalendar = false
        }
    }

    var targetCalendarSystemForEntry by remember(initialItem?.targetCalendarSystem, defaultEventSettings.targetCalendarId, useGoogleBackupMode) {
        mutableStateOf(
            if (useGoogleBackupMode) null
            else initialItem?.targetCalendarSystem ?: defaultEventSettings.targetCalendarId
        )
    }

    var notifyEnabled by remember(initialItem, defaultEventSettings) { mutableStateOf(initialItem?.notifyEnabled ?: defaultEventSettings.systemNotification) }
    var showDetailsInNotification by remember(initialItem, defaultEventSettings) { mutableStateOf(initialItem?.showDetailsInNotification ?: defaultEventSettings.showDetailsInNotification) }
    var selectedColorArgb by remember(initialItem) {
        mutableStateOf(initialItem?.colorArgb ?: NO_EVENT_FOOD_COLOR_ARGB)
    }
    var selectedSetId by remember(initialItem) { mutableStateOf(initialItem?.setId) }
    var setMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    val datePickerDialog = remember(selectedDate, context) {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val newSelectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                selectedDate = newSelectedDate
                if (!isOneTime && selectedRepeatDays.isEmpty()){
                    selectedRepeatDays = emptySet<DayOfWeek>() + newSelectedDate.dayOfWeek
                }
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

    val timePickerDialog = remember(selectedTime, context) {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                selectedTime = LocalTime.of(hour, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            true
        )
    }

    val saveEntryAction = {
        val finalRepeatEveryWeeks = repeatEveryWeeks.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val hours = durationInputHours.toIntOrNull() ?: 0
        val parsedMinutes = durationInputMinutes.toIntOrNull() ?: 0
        val finalTotalDurationMinutes = ((hours * 60) + parsedMinutes).coerceAtLeast(1)

        val finalAddToCalendar = if (useGoogleBackupMode) false else addToCalendar
        val finalTargetCalendarSystem = if (useGoogleBackupMode || !finalAddToCalendar) null else targetCalendarSystemForEntry

        val baseItem = initialItem ?: ScheduleItem(
            name = "",
            hour = 0,
            minute = 0,
            notifyEnabled = defaultEventSettings.systemNotification,
            showDetailsInNotification = defaultEventSettings.showDetailsInNotification,
            isOneTime = defaultEventSettings.isOneTime,
            durationMinutes = (defaultEventSettings.durationHours * 60) + defaultEventSettings.durationMinutes,
            addToCalendarOnSave = if (useGoogleBackupMode) false else defaultEventSettings.createCalendarEntry,
            targetCalendarSystem = if (useGoogleBackupMode) null else defaultEventSettings.targetCalendarId
        )

        val itemToSave: ScheduleItem
        if (isOneTime) {
            itemToSave = baseItem.copy(
                id = initialItem?.id ?: 0L,
                name = title.trim(),
                notes = notes.trim().takeIf { it.isNotEmpty() },
                hour = selectedTime.hour,
                minute = selectedTime.minute,
                durationMinutes = finalTotalDurationMinutes,
                isOneTime = true,
                dateEpochDay = selectedDate.toEpochDay(),
                repeatOnDays = null,
                startEpochDay = null,
                repeatEveryWeeks = 1,
                addToCalendarOnSave = finalAddToCalendar,
                targetCalendarSystem = finalTargetCalendarSystem,
                notifyEnabled = notifyEnabled,
                showDetailsInNotification = if (notifyEnabled) showDetailsInNotification else false,
                colorArgb = selectedColorArgb,
                setId = selectedSetId
            )
        } else {
            val currentRepeatDaysToSave = selectedRepeatDays.takeIf { it.isNotEmpty() }
            var actualStartEpochDay: Long? = null
            if (currentRepeatDaysToSave != null && currentRepeatDaysToSave.isNotEmpty()) {
                var firstEffectiveDate = selectedDate
                if (firstEffectiveDate.dayOfWeek !in currentRepeatDaysToSave) {
                    var tempDate = firstEffectiveDate
                    while (tempDate.dayOfWeek !in currentRepeatDaysToSave) {
                        tempDate = tempDate.plusDays(1)
                    }
                    firstEffectiveDate = tempDate
                }
                actualStartEpochDay = firstEffectiveDate.toEpochDay()
            }

            itemToSave = baseItem.copy(
                id = initialItem?.id ?: 0L,
                name = title.trim(),
                notes = notes.trim().takeIf { it.isNotEmpty() },
                hour = selectedTime.hour,
                minute = selectedTime.minute,
                durationMinutes = finalTotalDurationMinutes,
                isOneTime = false,
                dateEpochDay = null,
                repeatOnDays = currentRepeatDaysToSave,
                startEpochDay = actualStartEpochDay,
                repeatEveryWeeks = finalRepeatEveryWeeks,
                addToCalendarOnSave = finalAddToCalendar,
                targetCalendarSystem = finalTargetCalendarSystem,
                notifyEnabled = notifyEnabled,
                showDetailsInNotification = if (notifyEnabled) showDetailsInNotification else false,
                colorArgb = selectedColorArgb,
                setId = selectedSetId
            )
        }
        if (itemToSave.name.isNotBlank()) {
            if (!itemToSave.isOneTime && itemToSave.repeatOnDays.isNullOrEmpty()){
                Toast.makeText(context, "A recurring event must have at least one day selected.", Toast.LENGTH_LONG).show()
            } else {
               onSave(itemToSave)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isNewItem) "Create New Entry" else "Edit Entry",
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequest) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (isNewItem) {
                                TextButton(
                                    onClick = onOpenDefaultSettings,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = stringResource(R.string.settings_default_events_shortcut),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.settings_default_events_shortcut))
                                }
                            }
                            TextButton(onClick = saveEntryAction) {
                                Text("SAVE")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    RichTextEditor(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes (optional)",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(8.dp))

                    Box(modifier = Modifier.clickable { timePickerDialog.show() }) {
                        OutlinedTextField(
                            value = selectedTime.format(timeFormatter),
                            onValueChange = { },
                            label = { Text("Time") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select time") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Text("Duration:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = durationInputHours,
                            onValueChange = { durationInputHours = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        TextField(
                            value = durationInputMinutes,
                            onValueChange = { durationInputMinutes = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Minutes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    val isUsingSetColor = selectedSetId != null
                    val eventSetColorArgb = selectedSetId?.let { id ->
                        eventSetColors.getOrNull(id - 1)
                    }
                    val displayColorArgb = eventSetColorArgb ?: selectedColorArgb

                    ExposedDropdownMenuBox(
                        expanded = setMenuExpanded,
                        onExpandedChange = { setMenuExpanded = !setMenuExpanded }
                    ) {
                        val selectedSetLabel = selectedSetId?.let { id ->
                            eventSetNames.getOrNull(id - 1)
                        } ?: stringResource(R.string.event_set_none)
                        OutlinedTextField(
                            value = selectedSetLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.event_set_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = setMenuExpanded,
                            onDismissRequest = { setMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.event_set_none)) },
                                onClick = {
                                    selectedSetId = null
                                    setMenuExpanded = false
                                }
                            )
                            eventSetNames.forEachIndexed { index, name ->
                                val setId = index + 1
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedSetId = setId
                                        setMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    if (isUsingSetColor) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.event_color_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                stringResource(R.string.event_set_color_predefined_helper),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    SeriesColorPicker(
                        label = stringResource(R.string.event_color_label),
                        selectedColor = Color(displayColorArgb),
                        onColorSelected = { selectedColorArgb = it.toArgb() },
                        enabled = !isUsingSetColor,
                        colorOptions = if (isUsingSetColor) listOf(Color(displayColorArgb)) else EventFoodColorOptions,
                        showLabel = !isUsingSetColor,
                        allowCustomColor = !isUsingSetColor
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { isOneTime = !isOneTime }
                    ) {
                        Checkbox(
                            checked = !isOneTime,
                            onCheckedChange = { isRepeating ->
                                isOneTime = !isRepeating
                                if (isRepeating && selectedRepeatDays.isEmpty()) {
                                    selectedRepeatDays = selectedRepeatDays + selectedDate.dayOfWeek
                                }
                            },
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(stringResource(R.string.event_repeats_weekly_label))
                    }
                    Spacer(Modifier.height(8.dp))

                    if (isOneTime) {
                        Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = selectedDate.format(dateFormatter),
                                onValueChange = { },
                                label = { Text("Date") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = { Icon(Icons.Default.DateRange, "Select Date") }
                            )
                        }
                    } else {
                        Text("Select Weekdays:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DayOfWeek.values().forEach { day ->
                                val isSelected = day in selectedRepeatDays
                                OutlinedButton(
                                    onClick = {
                                        if (isSelected) {
                                            selectedRepeatDays = selectedRepeatDays - day
                                        } else {
                                            selectedRepeatDays = selectedRepeatDays + day
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = repeatEveryWeeks,
                            onValueChange = { repeatEveryWeeks = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Every N weeks (1 = every)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                         Spacer(Modifier.height(8.dp))
                        Text("Starts in week of (optional for N-weeks):", style = MaterialTheme.typography.labelSmall)
                        Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = selectedDate.format(dateFormatter),
                                onValueChange = { },
                                label = { Text("Start anchor date") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = { Icon(Icons.Default.DateRange, "Select Date") }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable(enabled = !useGoogleBackupMode) {
                            if (!useGoogleBackupMode) {
                                addToCalendar = !addToCalendar
                            }
                        }
                    ) {
                        Checkbox(
                            checked = addToCalendar,
                            onCheckedChange = { if (!useGoogleBackupMode) addToCalendar = it },
                            enabled = !useGoogleBackupMode,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "Calendar entry",
                            color = if (useGoogleBackupMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else LocalContentColor.current
                        )
                    }

                    if (addToCalendar && !useGoogleBackupMode) {
                        Spacer(Modifier.height(8.dp))
                        Column(Modifier.selectableGroup()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (targetCalendarSystemForEntry == SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL),
                                        onClick = { targetCalendarSystemForEntry = SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL },
                                        enabled = !useGoogleBackupMode
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (targetCalendarSystemForEntry == SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL),
                                    onClick = null,
                                    enabled = !useGoogleBackupMode
                                )
                                Text(
                                    text = stringResource(R.string.settings_default_events_target_calendar_local_device),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = if (useGoogleBackupMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (targetCalendarSystemForEntry == SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY || targetCalendarSystemForEntry?.startsWith("google_calendar_id_") == true),
                                        onClick = { targetCalendarSystemForEntry = SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY },
                                        enabled = !useGoogleBackupMode
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (targetCalendarSystemForEntry == SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY || targetCalendarSystemForEntry?.startsWith("google_calendar_id_") == true),
                                    onClick = null,
                                    enabled = !useGoogleBackupMode
                                )
                                Text(
                                    text = stringResource(R.string.settings_default_events_select_gcal_primary),
                                    style = MaterialTheme. typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = if (useGoogleBackupMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Notification options:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { notifyEnabled = !notifyEnabled }
                    ) {
                        Checkbox(
                            checked = notifyEnabled,
                            onCheckedChange = { notifyEnabled = it },
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text("System notification")
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable(enabled = notifyEnabled) {
                            if (notifyEnabled) {
                                showDetailsInNotification = !showDetailsInNotification
                            }
                        }
                    ) {
                        Checkbox(
                            checked = showDetailsInNotification,
                            onCheckedChange = { if (notifyEnabled) showDetailsInNotification = it },
                            enabled = notifyEnabled,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "Show details in notification",
                            color = if (notifyEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}
