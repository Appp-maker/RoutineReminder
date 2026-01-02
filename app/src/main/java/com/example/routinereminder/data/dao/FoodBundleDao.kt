package com.example.routinereminder.data.dao

import androidx.room.*
import com.example.routinereminder.data.entities.FoodBundle
import com.example.routinereminder.data.entities.FoodBundleWithItems
import com.example.routinereminder.data.entities.RecipeIngredient

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

    // ---------- Recipe ingredients ----------

    @Query("SELECT * FROM recipe_ingredients WHERE bundleId = :bundleId")
    suspend fun getIngredientsForBundle(bundleId: Long): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(items: List<RecipeIngredient>)

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

    @Query("DELETE FROM recipe_ingredients WHERE bundleId = :bundleId")
    suspend fun deleteIngredientsForBundle(bundleId: Long)

    @Query("SELECT * FROM recipe_ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): RecipeIngredient?

    @Query("DELETE FROM recipe_ingredients WHERE id = :id")
    suspend fun deleteIngredientById(id: Long)

}
