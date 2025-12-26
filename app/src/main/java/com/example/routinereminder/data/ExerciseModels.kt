import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.routinereminder.data.LocalDateConverter
import com.example.routinereminder.data.MealEntry
import com.example.routinereminder.data.ScheduleItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(meal: MealEntry): Long

    @Update
    suspend fun update(meal: MealEntry)

    @Delete
    suspend fun delete(meal: MealEntry)

    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY id DESC")
    suspend fun getMealsForDate(date: LocalDate): List<MealEntry>

    @Query("SELECT * FROM meal_entries ORDER BY id DESC")
    fun observeAll(): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries ORDER BY id DESC")
    suspend fun getAllOnce(): List<MealEntry>

    suspend fun upsert(meal: MealEntry) {
        insert(meal)
    }
}

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(item: ScheduleItem): Long

    @Update
    suspend fun update(item: ScheduleItem)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM schedule_items WHERE id = :id")
    suspend fun getItemById(id: Long): ScheduleItem?

    @TypeConverters(LocalDateConverter::class)
    @Query("SELECT * FROM schedule_items WHERE dateEpochDay = :date")
    suspend fun getItemsForDate(date: LocalDate): List<ScheduleItem>

    @Query("SELECT * FROM schedule_items WHERE calendarEventId = :calendarEventId")
    suspend fun getItemByCalendarEventId(calendarEventId: Long): ScheduleItem?

    @Query("SELECT * FROM schedule_items WHERE calendarEventId IS NOT NULL")
    suspend fun getItemsWithCalendarIds(): List<ScheduleItem>

    @Query("SELECT * FROM schedule_items WHERE origin LIKE 'IMPORTED_%'")
    suspend fun getAllImportedItems(): List<ScheduleItem>

    @Query("SELECT * FROM schedule_items ORDER BY isOneTime ASC, dateEpochDay ASC, startEpochDay ASC, hour ASC, minute ASC")
    fun observeAll(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items ORDER BY isOneTime ASC, dateEpochDay ASC, startEpochDay ASC, hour ASC, minute ASC")
    suspend fun getAllOnce(): List<ScheduleItem>

    suspend fun upsert(item: ScheduleItem): Long {
        return insert(item)
    }

    @Query("DELETE FROM schedule_items WHERE origin = 'IMPORTED_GOOGLE'")
    suspend fun deleteGoogleCalendarEvents()

    @Query("DELETE FROM schedule_items WHERE origin = 'IMPORTED_LOCAL'")
    suspend fun deleteLocalCalendarEvents()

    @Query("SELECT * FROM schedule_items WHERE name = :name AND hour = :hour AND minute = :minute AND durationMinutes = :durationMinutes AND isOneTime = :isOneTime AND ((isOneTime = 1 AND dateEpochDay = :dateEpochDay) OR (isOneTime = 0 AND startEpochDay = :startEpochDay AND repeatOnDays = :repeatOnDaysString)) AND origin = 'APP_CREATED' AND calendarEventId IS NULL LIMIT 1")
    fun findMatchingAppOnlyItem(name: String, hour: Int, minute: Int, durationMinutes: Int, isOneTime: Boolean, dateEpochDay: Long?, startEpochDay: Long?, repeatOnDaysString: String?): ScheduleItem?

    @Query("SELECT * FROM schedule_items WHERE name = :name AND hour = :hour AND minute = :minute AND durationMinutes = :durationMinutes AND isOneTime = :isOneTime AND ((isOneTime = 1 AND dateEpochDay = :dateEpochDay) OR (isOneTime = 0 AND startEpochDay = :startEpochDay AND repeatOnDays = :repeatOnDaysString)) AND calendarEventId IS NULL LIMIT 1")
    fun findMatchingUnlinkedItemByContent(name: String, hour: Int, minute: Int, durationMinutes: Int, isOneTime: Boolean, dateEpochDay: Long?, startEpochDay: Long?, repeatOnDaysString: String?): ScheduleItem?
}