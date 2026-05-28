package com.bbparcels.model

data class Parcel(
    val id: Int = 0,
    val regionId: String,
    val world: String,
    val label: String,
    val description: String? = null,
    val status: ParcelStatus = ParcelStatus.AVAILABLE,
    val active: Boolean = true
)

enum class ParcelStatus(val value: String) {
    AVAILABLE("available"),
    RENTED("rented"),
    DISABLED("disabled")
}