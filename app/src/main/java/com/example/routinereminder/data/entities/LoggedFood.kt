package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ColumnInfo
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB_SQL
import java.time.DayOfWeek

@Entity(tableName = "logged_food")
data class LoggedFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ---- existing fields ----
    val date: String,

    @Embedded(prefix = "fp_")
    val foodProduct: FoodProduct,


    val portionSizeG: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val saturatedFatG: Double,
    val addedSugarsG: Double,
    val sodiumMg: Double,
    @ColumnInfo(defaultValue = "0")
    val isConsumed: Boolean = false,

    // ---- meal slot ----
    val mealSlot: String = "unspecified",
// ---- bundle marker ----
    val bundleName: String? = null,
    val bundleId: Long? = null,

    // ---- recurrence fields ----
    @ColumnInfo(defaultValue = DEFAULT_SERIES_COLOR_ARGB_SQL)
    val colorArgb: Int = DEFAULT_SERIES_COLOR_ARGB,
    val isOneTime: Boolean = false,
    val dateEpochDay: Long? = null,
    val startEpochDay: Long? = null,
    val repeatOnDays: Set<DayOfWeek>? = null,
    val repeatEveryWeeks: Int = 1
)
