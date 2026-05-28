package com.bbparcels.model

import java.time.LocalDateTime

data class Rental(
    val id: Int = 0,
    val parcelId: Int,
    val tenantUuid: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime? = null,
    val status: RentalStatus = RentalStatus.ACTIVE    
)

enum class RentalStatus(val value: String) {
    ACTIVE(value = "active"),
    EXPIRED(value = "expired"),
    TERMINATED(value= "terminated")
        
}