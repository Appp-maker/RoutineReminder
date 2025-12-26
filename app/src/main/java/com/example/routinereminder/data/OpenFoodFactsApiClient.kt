package com.example.routinereminder.data

import com.example.routinereminder.data.entities.FoodProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class SearchResult(
    val products: List<OpenFoodFactsProductSimple>
)

@Serializable
data class OpenFoodFactsProductSimple(
    val product_name: String?,
    val nutriments: Nutriments?
)

class OpenFoodFactsApiClient {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchFood(query: String): List<FoodProduct> = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/cgi/search.pl".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("search_terms", query)
            ?.addQueryParameter("search_simple", "1")
            ?.addQueryParameter("action", "process")
            ?.addQueryParameter("json", "1")
        ?.addQueryParameter("page_size", "50")

        ?.build() ?: return@withContext emptyList()

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "RoutineReminder/1.0 (Android; contact: forsenkm@gmail.com)"
            )
            .build()


        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val searchResult = json.decodeFromString<SearchResult>(responseBody)

            return@withContext searchResult.products.mapNotNull { simpleProduct ->
                val nutriments = simpleProduct.nutriments

                FoodProduct(
                    name = simpleProduct.product_name ?: return@mapNotNull null,

                    caloriesPer100g = nutriments?.energyKcal100g ?: 0.0,
                    proteinPer100g = nutriments?.proteins100g ?: 0.0,
                    carbsPer100g = nutriments?.carbohydrates100g ?: 0.0,
                    fatPer100g = nutriments?.fat100g ?: 0.0,
                    fiberPer100g = nutriments?.fiber100g ?: 0.0,

                    saturatedFatPer100g = nutriments?.saturatedFat100g ?: 0.0,
                    addedSugarsPer100g = nutriments?.sugars100g ?: 0.0,
                    sodiumPer100g = nutriments?.sodium100g ?: 0.0,

                    servingSizeG = nutriments?.servingSize?.toDoubleOrNull()
                )
            }

        }
    }

    suspend fun getFoodProduct(barcode: String): FoodProduct? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val responseBody = response.body?.string() ?: return@withContext null
            val offProduct = json.decodeFromString<OpenFoodFactsProduct>(responseBody)

            if (offProduct.status == 0 || offProduct.product == null) return@withContext null

            val product = offProduct.product
            val nutriments = product.nutriments ?: return@withContext null

            return@withContext FoodProduct(
                name = product.productName ?: "Unknown",
                caloriesPer100g = nutriments.energyKcal100g ?: 0.0,
                proteinPer100g = nutriments.proteins100g ?: 0.0,
                carbsPer100g = nutriments.carbohydrates100g ?: 0.0,
                fatPer100g = nutriments.fat100g ?: 0.0,
                fiberPer100g = nutriments.fiber100g ?: 0.0,
                saturatedFatPer100g = nutriments.saturatedFat100g ?: 0.0,
                addedSugarsPer100g = nutriments.sugars100g ?: 0.0,
                sodiumPer100g = nutriments.sodium100g ?: 0.0,
                servingSizeG = nutriments.servingSize?.toDoubleOrNull()
            )
        }
    }
}
