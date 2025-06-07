package com.luminoverse.animevibe.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accessId: String,
    val imageUrl: String?,
    val contentText: String,
    val type: String,
    val createdAt: Long = Instant.now().epochSecond,
    val sentAt: Long? = null
)