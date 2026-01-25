package com.example.routinereminder
import org.maplibre.android.maps.MapView
import com.example.routinereminder.ui.SessionSharePreviewScreen
import com.example.routinereminder.ui.shareBitmap
import com.example.routinereminder.ui.SessionStore
import com.example.routinereminder.ui.MapScreen
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.Manifest
import androidx.navigation.navArgument
import com.example.routinereminder.ui.CalorieTrackerViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


import androidx.lifecycle.repeatOnLifecycle

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import com.example.routinereminder.data.entities.ScheduleDone
import com.example.routinereminder.data.SettingsRepository
import androidx.navigation.NavController
import com.example.routinereminder.ui.Screen
import com.example.routinereminder.ui.BarcodeScannerScreen
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.material.icons.filled.LocationOn
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.routinereminder.ui.components.checklistCompletionState
import com.example.routinereminder.ui.components.formatChecklistLineText
import com.example.routinereminder.ui.components.parseChecklistLines
import com.example.routinereminder.ui.components.toggleChecklistLine
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.routinereminder.data.DefaultEventSettings
import com.example.routinereminder.data.ScheduleItem

import com.example.routinereminder.ui.CalorieTrackerScreen
import com.example.routinereminder.ui.AppTab
import com.example.routinereminder.ui.MainViewModel
import com.example.routinereminder.ui.SettingsCategory
import com.example.routinereminder.ui.SettingsScreen
import com.example.routinereminder.ui.WorkoutScreen
import com.example.routinereminder.ui.WorkoutSessionScreen
import com.example.routinereminder.ui.bundle.BundleDetailScreen
import com.example.routinereminder.ui.bundle.RecipeIngredientEditorScreen

import com.example.routinereminder.ui.components.EditItemDialog
import com.example.routinereminder.ui.components.isNoEventFoodColor
import com.example.routinereminder.ui.components.resolveEventFoodColor
import com.example.routinereminder.ui.components.SettingsIconButton
import com.example.routinereminder.ui.theme.AppPalette
import com.example.routinereminder.ui.theme.RoutineReminderTheme



import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.routinereminder.ui.bundle.BundleListScreen
import com.example.routinereminder.ui.bundle.CreateBundleScreen



@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var openMapOnLaunch by mutableStateOf(false)









    private val requestCalendarPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
            val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false

            if (writeGranted && readGranted) {
                Toast.makeText(this, "Calendar permissions granted.", Toast.LENGTH_LONG).show()
                viewModel.onAppResumed()
            } else {
                Toast.makeText(this, "Calendar permissions denied.", Toast.LENGTH_LONG).show()
            }
        }

    private fun requestCalendarPermissions() {
        requestCalendarPermissionsLauncher.launch(
            arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
        )
    }

    private val requestLocationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

    fun requestLocationPermissions() {
        requestLocationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppPaused()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openMapOnLaunch = intent?.getBooleanExtra(EXTRA_OPEN_MAP_TAB, false) == true

        // --- Google Account Picker launcher ---
        val accountPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val email = result.data?.dataString
                    viewModel.updateSelectedGoogleAccountName(email)
                }
            }


// --- Trigger Google account selection ---
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.triggerGoogleAccountSelectionFlow
                    .receiveAsFlow()
                    .collectLatest {

                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = "vnd.android.cursor.dir/email_v2"
                        }
                        accountPickerLauncher.launch(intent)


                    }
            }
        }



        // --- Calendar permissions ---
        lifecycleScope.launch {
            viewModel.requestCalendarPermission.receiveAsFlow().collectLatest {
                requestCalendarPermissions()
            }
        }

        // --- UI Content ---
        setContent {
            MainAppUI(
                viewModel = viewModel,
                lifecycleScope = lifecycleScope,
                openMapOnLaunch = openMapOnLaunch,
                onMapLaunchConsumed = {
                    openMapOnLaunch = false
                    intent?.removeExtra(EXTRA_OPEN_MAP_TAB)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra(EXTRA_OPEN_MAP_TAB, false) == true) {
            openMapOnLaunch = true
        }
        intent?.let { setIntent(it) }
    }

    companion object {
        const val EXTRA_OPEN_MAP_TAB = "extra_open_map_tab"
    }
}



    // ... (The rest of your MainActivity.kt file remains unchanged) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppUI(
    viewModel: MainViewModel,
    lifecycleScope: LifecycleCoroutineScope,
    openMapOnLaunch: Boolean,
    onMapLaunchConsumed: () -> Unit
) {
    val appThemeColors by viewModel.appThemeColors.collectAsState()
    RoutineReminderTheme(appThemeColors = appThemeColors) {
        val context = LocalContext.current
        var showExactAlarmPermissionDialogState by remember { mutableStateOf(false) }
        var showSettingsScreen by remember { mutableStateOf(false) }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val currentBaseRoute = currentRoute?.substringBefore("?")
        val enabledTabsState by viewModel.enabledTabs.collectAsState()
        val enabledTabs = enabledTabsState ?: AppTab.defaultTabs
        val allTabScreens = remember { AppTab.entries.map { it.screen } }
        val enabledTabScreens = enabledTabs.map { it.screen }
        val mainTabRoutes = allTabScreens.map { it.route }
        val enabledTabRoutes = enabledTabScreens.map { it.route }

        val showBottomBar = enabledTabScreens.size > 1 && currentBaseRoute in enabledTabRoutes

        val showFab = currentRoute == Screen.RoutineReminder.route && enabledTabs.contains(AppTab.Routine)

        val currentScreen by remember(currentRoute) {
            derivedStateOf {
                Screen.allScreens.firstOrNull { it.route == currentRoute } ?: Screen.RoutineReminder
            }
        }

        LaunchedEffect(Unit) {
            viewModel.syncStatus.collectLatest { status ->
                if (status.isNotBlank()) {
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted: $isGranted")
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.showExactAlarmPermissionDialog.receiveAsFlow().collectLatest {
                showExactAlarmPermissionDialogState = true
            }
        }

        LaunchedEffect(enabledTabRoutes, currentRoute) {
            if (currentRoute in mainTabRoutes && currentRoute !in enabledTabRoutes) {
                val fallbackRoute = enabledTabRoutes.firstOrNull() ?: Screen.RoutineReminder.route
                navController.navigate(fallbackRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        LaunchedEffect(openMapOnLaunch, enabledTabRoutes) {
            if (openMapOnLaunch && enabledTabRoutes.contains(Screen.Map.route)) {
                navController.navigate(Screen.Map.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                onMapLaunchConsumed()
            }
        }

        var showEditDialog by remember { mutableStateOf(false) }
        var itemToEdit by remember { mutableStateOf<ScheduleItem?>(null) }
        var showDeleteCalendarConfirmDialog by remember { mutableStateOf(false) }
        var itemForCalendarDeleteConfirmation by remember { mutableStateOf<ScheduleItem?>(null) }
        val scheduleItems by viewModel.scheduleItems.collectAsState()
        val selectedDate by viewModel.selectedDate.collectAsState()
        val defaultEventSettings by viewModel.defaultEventSettings.collectAsState()
        val useGoogleBackupMode by viewModel.useGoogleBackupMode.collectAsState()
        val eventSetNames by viewModel.eventSetNames.collectAsState()
        val eventSetColors by viewModel.eventSetColors.collectAsState()
        val activeSetIds by viewModel.activeSetIds.collectAsState()
        val availableSetIds by viewModel.availableSetIds.collectAsState()
        val hasManualActiveSetsForDate by viewModel.hasManualActiveSetsForDate.collectAsState()

        if (enabledTabsState == null) {
            FirstLaunchTabSelectionDialog(
                selectedTabs = enabledTabs,
                onTabSelectionChange = { updatedTabs ->
                    if (updatedTabs.isNotEmpty()) {
                        viewModel.saveEnabledTabs(updatedTabs)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_tabs_select_at_least_one),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        if (showSettingsScreen) {
            SettingsScreen(
                viewModel = viewModel,
                from = "default",
                onDismiss = { showSettingsScreen = false }
            )
        }
        else {
            Scaffold(
                floatingActionButton = {
                    if (currentRoute == Screen.RoutineReminder.route) {
                        FloatingActionButton(
                            onClick = {
                                itemToEdit = null
                                showEditDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(Icons.Filled.Add, "Create new entry")
                        }
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        val barScreens = enabledTabScreens

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            barScreens.forEach { s ->
                                NavigationBarItem(
                                    icon = { s.icon?.invoke() },
                                    label = { Text(s.title) },
                                    selected = currentBaseRoute == s.route,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = {
                                        val targetRoute =
                                            if (s == Screen.CalorieTracker) {
                                                Screen.CalorieTracker.route
                                            } else {
                                                s.route
                                            }

                                        if (currentRoute != targetRoute) {
                                            navController.navigate(targetRoute) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }


                                )
                            }
                        }
                    }
                }

            )



            { paddingValues ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ✅ NavHost defines all your routes
                    NavHost(
                        navController = navController,
                        startDestination = enabledTabRoutes.firstOrNull()
                            ?: Screen.RoutineReminder.route
                    ) {
                        composable(Screen.RoutineReminder.route) {
                            MainScreenContent(
                                items = scheduleItems,
                                currentDate = selectedDate,
                                eventSetNames = eventSetNames,
                                eventSetColors = eventSetColors,
                                activeSetIds = activeSetIds,
                                availableSetIds = availableSetIds,
                                hasManualActiveSetsForDate = hasManualActiveSetsForDate,
                                onPreviousDay = { viewModel.selectPreviousDay() },
                                onNextDay = { viewModel.selectNextDay() },
                                onDateSelected = viewModel::selectDate,
                                onEditRequest = { item ->
                                    lifecycleScope.launch {
                                        viewModel.getScheduleItemForEditing(item.id)?.let {
                                            itemToEdit = it
                                            showEditDialog = true
                                        } ?: Toast.makeText(context, "Event no longer exists.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeleteItem = { itemToDelete ->
                                    if (itemToDelete.calendarEventId != null) {
                                        itemForCalendarDeleteConfirmation = itemToDelete
                                        showDeleteCalendarConfirmDialog = true
                                    } else {
                                        viewModel.deleteScheduleItem(itemToDelete, false)
                                    }
                                },
                                navController = navController,
                                onSettingsClick = {
                                    navController.navigate("settings/routine")

                                },
                                onMarkDone = { item ->
                                    viewModel.markScheduleItemDone(item, selectedDate.toEpochDay())
                                },
                                onUndoDone = { item ->
                                    viewModel.unmarkScheduleItemDone(item, selectedDate.toEpochDay())
                                },
                                viewModel = viewModel
                            )

                        }


//                        composable(Screen.CalorieTracker.route) {
//                            val caloriesEntry = remember(it) {
//                                navController.getBackStackEntry(Screen.CalorieTracker.route)
//                            }
//
//                            val calorieVm: CalorieTrackerViewModel =
//                                hiltViewModel(caloriesEntry)
//
//                            CalorieTrackerScreen(
//                                navController = navController,
//                                startMode = "default"
//                            )
//                        }
                        composable(
                            route = "bundle/{bundleId}/ingredient?ingredientId={ingredientId}",
                            arguments = listOf(
                                navArgument("ingredientId") {
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val bundleId =
                                backStackEntry.arguments?.getString("bundleId")?.toLongOrNull()
                                    ?: return@composable
                            val ingredientId =
                                backStackEntry.arguments?.getString("ingredientId")?.toLongOrNull()

                            RecipeIngredientEditorScreen(
                                navController = navController,
                                bundleId = bundleId,
                                ingredientId = ingredientId
                            )
                        }


//                        composable("bundle/{bundleId}") { backStackEntry ->
//                            val bundleId = backStackEntry.arguments?.getString("bundleId")!!.toLong()
//                            BundleDetailScreen(navController, bundleId)
//                        }
                        composable("bundle/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")?.toLong() ?: return@composable
                            BundleDetailScreen(
                                navController = navController,
                                bundleId = id
                            )
                        }

                        composable(Screen.BundleList.route) {
                            BundleListScreen(navController)
                        }
                        composable(Screen.CreateBundle.route) {
                            CreateBundleScreen(navController)
                        }
                        composable("bundle/{bundleId}") { backStackEntry ->
                            val bundleId = backStackEntry.arguments
                                ?.getString("bundleId")
                                ?.toLongOrNull()
                                ?: return@composable

                            BundleDetailScreen(
                                navController = navController,
                                bundleId = bundleId
                            )
                        }


                      composable(
                          route = "calories?mode={mode}",
                          arguments = listOf(
                              navArgument("mode") { defaultValue = "default" }
                          )
                      ) { backStackEntry ->

                          // 🔑 SINGLE shared VM for ALL calorie flows
                          val caloriesEntry = remember(backStackEntry) {
                              navController.getBackStackEntry(Screen.CalorieTracker.route)
                          }

                          val calorieVm: CalorieTrackerViewModel =
                              hiltViewModel(caloriesEntry)

                          val mode = backStackEntry.arguments?.getString("mode") ?: "default"

                          CalorieTrackerScreen(
                              navController = navController,
                              startMode = mode,
                              viewModel = calorieVm
                          )

                      }



                        composable(
                            route = "settings/{from}?category={category}",
                            arguments = listOf(
                                navArgument("from") { defaultValue = "default" },
                                navArgument("category") { defaultValue = "" }
                            )
                        ) { backStackEntry ->
                        val from = backStackEntry.arguments?.getString("from") ?: "default"
                        val initialCategory = backStackEntry.arguments
                            ?.getString("category")
                            ?.takeIf { value: String -> value.isNotBlank() }
                            ?.let { categoryName: String ->
                                runCatching {
                                    SettingsCategory.valueOf(categoryName.uppercase())
                                }.getOrNull()
                            }

                            SettingsScreen(
                                viewModel = viewModel,
                                from = from,
                                initialCategory = initialCategory,
                                onDismiss = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.BarcodeScanner.route) {
                            BarcodeScannerScreen(navController = navController)
                        }
                        composable(Screen.Map.route) {
                            val context = LocalContext.current
                            LaunchedEffect(Unit) {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    (context as? MainActivity)?.requestLocationPermissions()
                                }
                            }
                            MapScreen(navController = navController)
                        }

                        composable(Screen.Workout.route) {
                            WorkoutScreen(
                                navController = navController,
                                mainViewModel = viewModel
                            )
                        }
                        composable(
                            route = Screen.WorkoutSession.route,
                            arguments = listOf(navArgument("planId") { defaultValue = "" })
                        ) { backStackEntry ->
                            val planId = backStackEntry.arguments?.getString("planId").orEmpty()
                            if (planId.isNotBlank()) {
                                WorkoutSessionScreen(
                                    navController = navController,
                                    planId = planId
                                )
                            }
                        }

                        composable("share_preview/{sessionId}") { backStackEntry ->
                            val context = LocalContext.current
                            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                            val mapView = remember { MapView(context) } // optional reuse if needed

                            SessionSharePreviewScreen(
                                sessionId = sessionId,
                                mapView = mapView,
                                onShare = { bitmap ->
                                    shareBitmap(context, bitmap)
                                },
                                onCancel = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("history") {
                            com.example.routinereminder.ui.HistoryScreen(navController = navController)
                        }


                    }

                    // ✅ Dialogs stay the same
                    if (showEditDialog) {
                        EditItemDialog(
                            initialItem = itemToEdit,
                            defaultEventSettings = defaultEventSettings,
                            useGoogleBackupMode = useGoogleBackupMode,
                            eventSetNames = eventSetNames,
                            eventSetColors = eventSetColors,
                            onOpenDefaultSettings = {
                                showEditDialog = false
                                navController.navigate("settings/routine?category=default_events")
                            },
                            onDismissRequest = { showEditDialog = false },
                            onSave = {
                                viewModel.upsertScheduleItem(it)
                                showEditDialog = false
                            }
                        )
                    }

                    if (showExactAlarmPermissionDialogState) {
                        AlertDialog(
                            onDismissRequest = { showExactAlarmPermissionDialogState = false },
                            title = { Text("Permission Required") },
                            text = { Text("To ensure reminders are delivered on time, please grant permission to schedule exact alarms in the system settings.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showExactAlarmPermissionDialogState = false
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                        }
                                    }
                                ) {
                                    Text("Open Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExactAlarmPermissionDialogState = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }

                    if (showDeleteCalendarConfirmDialog && itemForCalendarDeleteConfirmation != null) {
                        DeleteFromCalendarConfirmationDialog(
                            itemTitle = itemForCalendarDeleteConfirmation!!.name,
                            isImported = itemForCalendarDeleteConfirmation!!.origin.startsWith("IMPORTED_"),
                            onConfirmDeleteBoth = {
                                viewModel.deleteScheduleItem(itemForCalendarDeleteConfirmation!!, true)
                                showDeleteCalendarConfirmDialog = false
                            },
                            onConfirmDeleteAppOnly = {
                                viewModel.deleteScheduleItem(itemForCalendarDeleteConfirmation!!, false)
                                showDeleteCalendarConfirmDialog = false
                            },
                            onDismiss = { showDeleteCalendarConfirmDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstLaunchTabSelectionDialog(
    selectedTabs: Set<AppTab>,
    onTabSelectionChange: (Set<AppTab>) -> Unit
) {
    val context = LocalContext.current
    var pendingSelection by remember { mutableStateOf(selectedTabs) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.settings_tabs_first_launch_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_tabs_first_launch_message))
                Spacer(modifier = Modifier.height(12.dp))
                AppTab.entries.forEach { tab ->
                    val checked = pendingSelection.contains(tab)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingSelection = if (checked) {
                                    pendingSelection - tab
                                } else {
                                    pendingSelection + tab
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                pendingSelection = if (isChecked) {
                                    pendingSelection + tab
                                } else {
                                    pendingSelection - tab
                                }
                            }
                        )
                        Text(
                            text = stringResource(tab.labelRes),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_tabs_disabled_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pendingSelection.isNotEmpty()) {
                        onTabSelectionChange(pendingSelection)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_tabs_select_at_least_one),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = pendingSelection.isNotEmpty()
            ) {
                Text(stringResource(R.string.settings_tabs_first_launch_confirm))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}


@Composable
fun DateDisplayHeader(
    currentDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateTextClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
        }

        Text(
            text = currentDate.format(formatter),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .clickable { onDateTextClick() },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
        }

        SettingsIconButton(onClick = onSettingsClick)

    }
}


@Composable
private fun ScheduleItemListContent(
    items: List<ScheduleItem>,
    currentDate: LocalDate,
    doneStates: Map<Pair<Long, Long>, Boolean>,
    eventSetColors: List<Int>,
    onLongPress: (ScheduleItem) -> Unit,
    viewModel: MainViewModel
)
 {
    val distinctItems = remember(items) { items.distinctBy { it.id } }
    val listState = rememberLazyListState()
    val today = remember(currentDate) { LocalDate.now() }
    val showNowIndicator = currentDate == today
    val nowIndicatorIndex = remember(distinctItems, currentDate) {
        if (!showNowIndicator) {
            -1
        } else {
            val nowTime = LocalTime.now()
            distinctItems.indexOfFirst { item ->
                val start = LocalTime.of(item.hour, item.minute)
                val end = start.plusMinutes(item.durationMinutes.toLong())
                nowTime.isBefore(end)
            }
        }
    }
    val displayItems: List<DisplayScheduleItem> = remember(
        distinctItems,
        showNowIndicator,
        nowIndicatorIndex
    ) {
        if (distinctItems.isEmpty()) {
            buildList {
                if (showNowIndicator) {
                    add(DisplayScheduleItem.NowIndicator)
                }
                add(DisplayScheduleItem.EmptyState)
            }
        } else if (!showNowIndicator || nowIndicatorIndex < 0) {
            distinctItems.map { item: ScheduleItem ->
                DisplayScheduleItem.Event(item)
            }
        } else {
            buildList {
                distinctItems.forEachIndexed { index: Int, item: ScheduleItem ->
                    if (index == nowIndicatorIndex) {
                        add(DisplayScheduleItem.NowIndicator)
                    }
                    add(DisplayScheduleItem.Event(item))
                }
            }
        }
    }

    LaunchedEffect(showNowIndicator, nowIndicatorIndex, displayItems.size) {
        if (showNowIndicator && nowIndicatorIndex >= 0) {
            listState.scrollToItem(nowIndicatorIndex)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        items(
            items = displayItems,
            key = { displayItem: DisplayScheduleItem ->
                when (displayItem) {
                    is DisplayScheduleItem.Event -> displayItem.item.id
                    DisplayScheduleItem.NowIndicator -> "now-indicator"
                    DisplayScheduleItem.EmptyState -> "empty-state"
                }
            }
        ) { displayItem: DisplayScheduleItem ->
            when (displayItem) {
                is DisplayScheduleItem.Event -> {
                    val item = displayItem.item
                    val key: Pair<Long, Long> = item.id to currentDate.toEpochDay()
                    val isDoneToday = doneStates[key] == true

                    ScheduleItemView(
                        item = item,
                        currentDate = currentDate,
                        isDoneToday = isDoneToday,
                        eventSetColors = eventSetColors,
                        onLongPress = onLongPress,
                        onChecklistUpdate = { updatedItem, updatedNotes, completion ->
                            viewModel.upsertScheduleItem(updatedItem.copy(notes = updatedNotes))
                            if (completion.hasCheckboxes) {
                                if (completion.allChecked) {
                                    viewModel.markScheduleItemDone(updatedItem, currentDate.toEpochDay())
                                } else {
                                    viewModel.unmarkScheduleItemDone(updatedItem, currentDate.toEpochDay())
                                }
                            }
                        }
                    )
                }
                DisplayScheduleItem.NowIndicator -> {
                    NowIndicatorRow()
                }
                DisplayScheduleItem.EmptyState -> {
                    Text(
                        text = "No entries for this day. Tap + to create a new entry or swipe for another day.",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NowIndicatorRow() {
    val lineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Now",
            style = MaterialTheme.typography.labelSmall,
            color = lineColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(lineColor)
        )
    }
}

private sealed class DisplayScheduleItem {
    data class Event(val item: ScheduleItem) : DisplayScheduleItem()
    data object NowIndicator : DisplayScheduleItem()
    data object EmptyState : DisplayScheduleItem()
}

@Composable
fun UnifiedDateHeader(
    currentDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateTextClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Day")
        }

        Text(
            text = currentDate.format(formatter),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .clickable { onDateTextClick() },
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        IconButton(onClick = onNextDay, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day")
        }

        SettingsIconButton(onClick = onSettingsClick)
    }
}

@Composable
private fun EventSetToggleRow(
    eventSetNames: List<String>,
    availableSetIds: Set<Int>,
    activeSetIds: Set<Int>,
    hasManualSetsForDate: Boolean,
    onToggleSet: (Int) -> Boolean,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val selectedSetNames = remember(eventSetNames, availableSetIds, activeSetIds) {
        availableSetIds.sorted().mapNotNull { setId ->
            if (activeSetIds.contains(setId)) {
                eventSetNames.getOrNull(setId - 1) ?: "Set $setId"
            } else {
                null
            }
        }
    }
    val compactNames = remember(selectedSetNames) {
        selectedSetNames.map { name ->
            if (name.startsWith("Set ") && name.length <= 5) {
                name.removePrefix("Set ")
            } else {
                name
            }
        }
    }
    val buttonLabel = if (compactNames.isEmpty()) {
        stringResource(R.string.event_sets_compact_placeholder)
    } else {
        stringResource(R.string.event_sets_compact_label, compactNames.joinToString(" / "))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .width(90.dp)
                .height(28.dp),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = buttonLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                modifier = Modifier.weight(1f, fill = false),
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp)
            )
        }
        if (hasManualSetsForDate) {
            Text(
                text = stringResource(R.string.event_sets_manual_override),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.event_sets_dialog_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.event_sets_dialog_body))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.event_sets_dialog_active_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    availableSetIds.sorted().forEach { setId ->
                        val name = eventSetNames.getOrNull(setId - 1) ?: "Set $setId"
                        val selected = activeSetIds.contains(setId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = onToggleSet(setId)
                                    if (!updated) {
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.event_sets_max_selected,
                                                SettingsRepository.MAX_ACTIVE_SETS_PER_DAY
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.event_sets_dialog_done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onOpenSettings()
                    }
                ) {
                    Text(stringResource(R.string.event_sets_dialog_settings_action))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    items: List<ScheduleItem>,
    currentDate: LocalDate,
    eventSetNames: List<String>,
    eventSetColors: List<Int>,
    activeSetIds: Set<Int>,
    availableSetIds: Set<Int>,
    hasManualActiveSetsForDate: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onEditRequest: (ScheduleItem) -> Unit,
    onDeleteItem: (ScheduleItem) -> Unit,
    navController: NavController,                      // <-- ADD THIS
    onSettingsClick: () -> Unit,                       // <-- leave as function
    onMarkDone: (ScheduleItem) -> Unit,
    onUndoDone: (ScheduleItem) -> Unit,
    viewModel: MainViewModel
)

 {
    var showItemActionDialog by remember { mutableStateOf(false) }
    var selectedItemForAction by remember { mutableStateOf<ScheduleItem?>(null) }
    var selectedIsDoneToday by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var totalHorizontalDrag by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }
    var showDatePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalHorizontalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalHorizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            totalHorizontalDrag > swipeThresholdPx -> onPreviousDay()
                            totalHorizontalDrag < -swipeThresholdPx -> onNextDay()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            UnifiedDateHeader(
                currentDate = currentDate,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onDateTextClick = { showDatePicker = true },
                onSettingsClick = { navController.navigate("settings/routine") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (availableSetIds.isNotEmpty()) {
                EventSetToggleRow(
                    eventSetNames = eventSetNames,
                    availableSetIds = availableSetIds,
                    activeSetIds = activeSetIds,
                    hasManualSetsForDate = hasManualActiveSetsForDate,
                    onToggleSet = { setId -> viewModel.toggleActiveSet(setId) },
                    onOpenSettings = { navController.navigate("settings/routine?category=event_sets") }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            ScheduleItemListContent(
                items = items,
                currentDate = currentDate,
                doneStates = viewModel.doneItemsForDay.collectAsState().value,
                eventSetColors = eventSetColors,
                onLongPress = { item ->
                    selectedItemForAction = item
                    coroutineScope.launch {
                        selectedIsDoneToday = viewModel.isDoneForDate(item.id, currentDate.toEpochDay())
                        showItemActionDialog = true
                    }
                },
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(72.dp))
        }

    }

    if (showItemActionDialog && selectedItemForAction != null) {
        ItemActionDialog(
            item = selectedItemForAction!!,
            isDoneToday = selectedIsDoneToday,
            onDismissRequest = { showItemActionDialog = false },
            onEdit = {
                onEditRequest(it)
                showItemActionDialog = false
            },
            onDelete = { itemFromDialog ->
                onDeleteItem(itemFromDialog)
                showItemActionDialog = false
            },
            onMarkDone = { itemFromDialog ->
                onMarkDone(itemFromDialog)
                showItemActionDialog = false
            },
            onUndoDone = { itemFromDialog ->
                onUndoDone(itemFromDialog)
                showItemActionDialog = false
            }
        )



    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleItemView(
    item: ScheduleItem,
    currentDate: LocalDate,
    isDoneToday: Boolean,
    eventSetColors: List<Int>,
    onLongPress: (ScheduleItem) -> Unit,
    onChecklistUpdate: (ScheduleItem, String, com.example.routinereminder.ui.components.ChecklistCompletion) -> Unit
) {
    var isExpanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var notesText by remember(item.id, item.notes) { mutableStateOf(item.notes.orEmpty()) }
    val timeString = String.format(Locale.getDefault(), "%02d:%02d", item.hour, item.minute)
    val realNowDateTime = LocalDateTime.now()
    val itemAbsoluteStartDateTime = currentDate.atTime(item.hour, item.minute)
    val itemAbsoluteEndDateTime = itemAbsoluteStartDateTime.plusMinutes(item.durationMinutes.toLong())
    val endTimeFormatted = remember(itemAbsoluteEndDateTime) {
        itemAbsoluteEndDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val isEffectivelyActiveNow = currentDate.isEqual(realNowDateTime.toLocalDate()) &&
            !realNowDateTime.isBefore(itemAbsoluteStartDateTime) &&
            realNowDateTime.isBefore(itemAbsoluteEndDateTime)
    val isEffectivelyPastNow = itemAbsoluteEndDateTime.isBefore(realNowDateTime)
    val rowBackgroundColor = if (isEffectivelyActiveNow) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f) else Color.Transparent
    val resolvedColorArgb = item.setId?.let { setId ->
        eventSetColors.getOrNull(setId - 1)
    } ?: item.colorArgb
    val seriesColor = resolveEventFoodColor(resolvedColorArgb, MaterialTheme.colorScheme.outlineVariant)
    val showSeriesColor = !isNoEventFoodColor(resolvedColorArgb)


// Base text color logic
    val baseTextColor = when {
        isDoneToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        isEffectivelyPastNow -> MaterialTheme.colorScheme.outline
        else -> LocalContentColor.current
    }

// Style for DONE state (grey + strikethrough)
    val doneAlpha = if (isDoneToday) 0.4f else 1f
    val doneDecoration = if (isDoneToday) TextDecoration.LineThrough else TextDecoration.None

    val titleTextColor = when {
        isDoneToday -> baseTextColor.copy(alpha = doneAlpha)
        isEffectivelyPastNow -> baseTextColor.copy(alpha = doneAlpha)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = doneAlpha)
    }
    val doneTextStyle = MaterialTheme.typography.titleMedium.copy(
        color = titleTextColor,
        textDecoration = doneDecoration
    )





    val prefixColor = when (item.origin) {
        "IMPORTED_GOOGLE" -> MaterialTheme.colorScheme.secondary
        "APP_CREATED_GOOGLE" -> MaterialTheme.colorScheme.secondary
        "IMPORTED_LOCAL" -> MaterialTheme.colorScheme.tertiary
        "APP_CREATED_LOCAL" -> MaterialTheme.colorScheme.tertiary
        "APP_CREATED" -> MaterialTheme.colorScheme.secondary
        else -> baseTextColor
    }
    val prefix = when (item.origin) {
        "IMPORTED_GOOGLE" -> "Google Calendar"
        "APP_CREATED_GOOGLE" -> "Google Calendar (App)"
        "IMPORTED_LOCAL" -> "Local Calendar"
        "APP_CREATED_LOCAL" -> "Local Calendar (App)"
        "APP_CREATED" -> "App Only"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .combinedClickable(
                onClick = { },
                onDoubleClick = { isExpanded = !isExpanded },
                onLongClick = { onLongPress(item) }
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            val indicatorColor = if (showSeriesColor) {
                seriesColor
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(4.dp)
                    .height(20.dp)
                    .background(indicatorColor.copy(alpha = doneAlpha), shape = MaterialTheme.shapes.extraSmall)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // LEFT COLUMN: start time + ONE TIME / REPEATS under it
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.titleMedium,
                    color = baseTextColor.copy(alpha = doneAlpha)
                )

                // Badge directly under the start time
                if (item.isOneTime || (item.repeatOnDays != null || item.repeatEveryWeeks > 1)) {
                    Spacer(modifier = Modifier.height(2.dp))

                    if (item.isOneTime) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)              // SAME WIDTH FOR ALL BADGES
                                .height(20.dp)             // UNIFIED HEIGHT
                                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall),
                            contentAlignment = Alignment.Center  // CENTER TEXT
                        ) {
                            Text(
                                text = "ONE TIME",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                    } else {
                        Box(
                            modifier = Modifier
                                .width(70.dp)              // SAME WIDTH
                                .height(20.dp)             // SAME HEIGHT
                                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall),
                            contentAlignment = Alignment.Center  // CENTER TEXT
                        ) {
                            Text(
                                text = "REPEATS",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }


                    }
                }
            }

           Spacer(modifier = Modifier.width(8.dp))

            // RIGHT COLUMN: title + end time (first line), prefix under title
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = item.name,
                        style = doneTextStyle,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // NEW end-time + prefix column
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = endTimeFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = baseTextColor.copy(alpha = doneAlpha)

                        )

                        prefix?.let {
                            val (bgColor, textColor) = when (item.origin) {
                                "APP_CREATED" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                "IMPORTED_GOOGLE" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                "APP_CREATED_GOOGLE" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                "IMPORTED_LOCAL" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                "APP_CREATED_LOCAL" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                else -> AppPalette.TagFallbackSoft to AppPalette.TextSoft
                            }

                            Text(
                                text = it.uppercase(),
                                color = textColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(bgColor, shape = MaterialTheme.shapes.extraSmall)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }


                    }
                }
            }
        }
        if (notesText.isNotBlank()) {
            val checklistLines = remember(notesText) { parseChecklistLines(notesText) }
            val visibleChecklistLines = if (isExpanded) checklistLines else checklistLines.take(3)
            val checklistTextIndent = 54.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackgroundColor)
                    .padding(vertical = 0.dp, horizontal = 2.dp)
                    .animateContentSize()
            ) {
                visibleChecklistLines.forEachIndexed { index, line ->
                    if (line.hasCheckbox) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = line.isChecked,
                                onCheckedChange = {
                                    val updatedNotes = toggleChecklistLine(notesText, index)
                                    notesText = updatedNotes
                                    onChecklistUpdate(
                                        item,
                                        updatedNotes,
                                        checklistCompletionState(updatedNotes)
                                    )
                                },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = formatChecklistLineText(line.text),
                                style = MaterialTheme.typography.bodySmall,
                                color = baseTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = formatChecklistLineText(line.text),
                            style = MaterialTheme.typography.bodySmall,
                            color = baseTextColor,
                            modifier = Modifier.padding(start = checklistTextIndent),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (checklistLines.size > 3) {
                    Text(
                        text = if (isExpanded) {
                            "Double tap to collapse description."
                        } else {
                            "Double tap to expand description."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

}


@Composable
fun ItemActionDialog(
    item: ScheduleItem,
    isDoneToday: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: (ScheduleItem) -> Unit,
    onDelete: (ScheduleItem) -> Unit,
    onMarkDone: (ScheduleItem) -> Unit,
    onUndoDone: (ScheduleItem) -> Unit
) {
    val isDone = isDoneToday



    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Action for '${item.name}'") },
        text = { Text("What would you like to do?") },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // EDIT
                Button(
                    onClick = { onEdit(item) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }

                Spacer(Modifier.height(6.dp))

                // DONE / UNDONE
                if (!isDone) {
                    Button(
                        onClick = { onMarkDone(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark as Done")
                    }
                } else {
                    Button(
                        onClick = { onUndoDone(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Undo Done")
                    }
                }

                Spacer(Modifier.height(6.dp))

                // DELETE
                TextButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )

}




@Composable
fun DeleteFromCalendarConfirmationDialog(
    itemTitle: String,
    isImported: Boolean,
    onConfirmDeleteBoth: () -> Unit,
    onConfirmDeleteAppOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete calendar entry?") },
        text = { Text(if (isImported) "Also delete the imported entry '$itemTitle' from your calendar?" else "Also delete the entry '$itemTitle' from your calendar?") },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = onConfirmDeleteBoth, modifier = Modifier.fillMaxWidth()) { Text("App & Calendar") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onConfirmDeleteAppOnly, modifier = Modifier.fillMaxWidth()) { Text("Block Import & Delete from App") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        },
        dismissButton = null
    )
}

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val cachePath = File(context.cacheDir, "shared_images")
    cachePath.mkdirs()
    val file = File(cachePath, "share_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
