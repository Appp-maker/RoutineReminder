package com.example.routinereminder.data.dao

import androidx.room.*
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodBundleItem
import com.example.routinereminder.data.entities.FoodBundleWithItems

@Dao
interface FoodBundleDao {

    @Transaction
    @Query("SELECT * FROM food_bundles WHERE id = :bundleId")
    suspend fun getBundleWithItems(bundleId: Long): FoodBundleWithItems
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

    @Query("""
    UPDATE food_bundles
    SET name = :name,
        description = :description
    WHERE id = :id
""")
    suspend fun updateBundle(
        id: Long,
        name: String,
        description: String
    )

    @Query("DELETE FROM food_bundle_items WHERE bundleId = :bundleId")
    suspend fun deleteItemsForBundle(bundleId: Long)

    @Query("SELECT * FROM food_bundle_items WHERE id = :id")
    suspend fun getItemById(id: Long): FoodBundleItem?

    @Query("DELETE FROM food_bundle_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

}

