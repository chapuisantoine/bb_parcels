package com.bbparcels.model

data class ParcelWithOwner(
    val regionId: String,
    val world: String,
    val label: String,
    val status: ParcelStatus = ParcelStatus.AVAILABLE,
    val ownerUuid: String?    // null si pas de locataire actif
)