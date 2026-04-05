package com.example.routinereminder.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class EventSetImageOption(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val textGlyph: String? = null
)

object EventSetImageCatalog {
    const val DEFAULT_KEY = "activity_walk"

    val options: List<EventSetImageOption> = buildList {
        add(EventSetImageOption(key = DEFAULT_KEY, label = "Walk", icon = Icons.Filled.DirectionsWalk))
        add(EventSetImageOption(key = "activity_run", label = "Run", icon = Icons.Filled.DirectionsRun))
        add(EventSetImageOption(key = "activity_bike", label = "Bike", icon = Icons.Filled.DirectionsBike))
        add(EventSetImageOption(key = "activity_car", label = "Drive", icon = Icons.Filled.DirectionsCar))
        add(EventSetImageOption(key = "activity_bus", label = "Bus", icon = Icons.Filled.DirectionsBus))
        add(EventSetImageOption(key = "activity_boat", label = "Boat", icon = Icons.Filled.DirectionsBoat))
        add(EventSetImageOption(key = "activity_flight", label = "Flight", icon = Icons.Filled.Flight))
        add(EventSetImageOption(key = "activity_home", label = "Home", icon = Icons.Filled.Home))
        add(EventSetImageOption(key = "activity_work", label = "Work", icon = Icons.Filled.Work))
        add(EventSetImageOption(key = "activity_school", label = "School", icon = Icons.Filled.School))
        add(EventSetImageOption(key = "activity_food", label = "Food", icon = Icons.Filled.Restaurant))
        add(EventSetImageOption(key = "activity_sports", label = "Sport", icon = Icons.Filled.SportsSoccer))
        add(EventSetImageOption(key = "activity_ball", label = "Ball", icon = Icons.Filled.SportsBasketball))
        add(EventSetImageOption(key = "activity_gym", label = "Gym", icon = Icons.Filled.FitnessCenter))
        add(EventSetImageOption(key = "activity_meditate", label = "Meditate", icon = Icons.Filled.SelfImprovement))

        ('A'..'Z').forEach { letter ->
            add(EventSetImageOption(key = "letter_$letter", label = "Letter $letter", textGlyph = letter.toString()))
        }
        ('0'..'9').forEach { digit ->
            add(EventSetImageOption(key = "number_$digit", label = "Number $digit", textGlyph = digit.toString()))
        }
    }

    private val optionsByKey: Map<String, EventSetImageOption> = options.associateBy { it.key }

    fun normalizeKey(key: String?): String {
        return if (key != null && optionsByKey.containsKey(key)) key else DEFAULT_KEY
    }

    fun optionFor(key: String?): EventSetImageOption {
        return optionsByKey[normalizeKey(key)] ?: options.first()
    }

    fun nextKey(currentKey: String?): String {
        val normalized = normalizeKey(currentKey)
        val currentIndex = options.indexOfFirst { it.key == normalized }
        if (currentIndex < 0) return options.first().key
        val nextIndex = (currentIndex + 1) % options.size
        return options[nextIndex].key
    }
}
