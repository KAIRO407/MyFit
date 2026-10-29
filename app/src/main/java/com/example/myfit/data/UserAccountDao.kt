package com.example.myfit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_account WHERE id = 1")
    fun getAccountFlow(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_account WHERE id = 1")
    suspend fun getAccount(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: UserAccountEntity)

    @Query("DELETE FROM user_account WHERE id = 1")
    suspend fun deleteAccount()
}
