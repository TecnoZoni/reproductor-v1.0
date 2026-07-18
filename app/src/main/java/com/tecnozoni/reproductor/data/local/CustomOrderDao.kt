package com.tecnozoni.reproductor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CustomOrderDao {

    @Query("SELECT * FROM custom_order ORDER BY position ASC")
    suspend fun getAll(): List<CustomOrderEntity>

    @Query("DELETE FROM custom_order")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CustomOrderEntity>)

    /** Reemplaza todo el orden de una (borra + inserta) en una transacción. */
    @Transaction
    suspend fun replaceAll(items: List<CustomOrderEntity>) {
        clear()
        insertAll(items)
    }
}
