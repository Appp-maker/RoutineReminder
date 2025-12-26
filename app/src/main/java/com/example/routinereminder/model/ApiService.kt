
package com.example.routinereminder.model

import com.example.routinereminder.data.ScheduleItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("sync")
    suspend fun syncTasks(@Body tasks: List<ScheduleItem>): Response<Unit>
}
