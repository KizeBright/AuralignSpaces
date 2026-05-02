package com.auralign.spaces.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomConfig(
    val type: String = "Living Room",
    val width: Double = 5.0,
    val length: Double = 4.0,
    val height: Double = 3.0,
    val budget: Double = 50000.0,
    val photoUrl: String? = null,
    val designId: String? = null
) : Parcelable
