package com.example.myfit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_steps ORDER BY date ASC")
    fun getAllDailySteps(): Flow<List<StepEntity>>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getStepEntityForDate(date: String): StepEntity?

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun getStepEntityFlowForDate(date: String): Flow<StepEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStep(stepEntity: StepEntity)
}
