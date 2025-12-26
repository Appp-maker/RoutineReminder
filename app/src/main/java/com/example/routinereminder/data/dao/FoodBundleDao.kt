package com.example.routinereminder.data.dao

import androidx.room.*
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodBundleItem

@Dao
interface FoodBundleDao {

    // ---------- Bundles (recipes) ----------

    @Query("SELECT * FROM food_bundles ORDER BY name ASC")
    suspend fun getAllBundles(): List<FoodBundle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundle(bundle: FoodBundle): Long

    @Delete
    suspend fun deleteBundle(bundle: FoodBundle)

    // ---------- Bundle items (ingredients) ----------

    @Query("SELECT * FROM food_bundle_items WHERE bundleId = :bundleId")
    suspend fun getItemsForBundle(bundleId: Long): List<FoodBundleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FoodBundleItem>)

    @Query("SELECT * FROM food_bundles WHERE id = :bundleId LIMIT 1")
    suspend fun getBundleById(bundleId: Long): FoodBundle?


    @Transaction
    suspend fun getBundleWithItems(bundleId: Long): FoodBundleWithItems {
        val bundle = getBundleById(bundleId)
            ?: return FoodBundleWithItems(
                bundle = FoodBundle(id = bundleId, name = "Unknown", description = ""),
                items = emptyList()
            )


        val items = getItemsForBundle(bundleId)
        return FoodBundleWithItems(bundle, items)
    }


    @Query("DELETE FROM food_bundle_items WHERE bundleId = :bundleId")
    suspend fun deleteItemsForBundle(bundleId: Long)
}
data class FoodBundleWithItems(
    val bundle: FoodBundle,
    val items: List<FoodBundleItem>
)
