package com.tecnozoni.reproductor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila = "esta canción va en esta posición del orden personalizado".
 * NO guardamos la canción (esa vive en MediaStore), solo su _ID y su posición.
 */
@Entity(tableName = "custom_order")
data class CustomOrderEntity(
    @PrimaryKey val mediaId: Long,
    val position: Int,
)
