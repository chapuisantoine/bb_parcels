package com.bbparcels.cache

import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ParcelStatus

class ParcelCache(
    private val parcelRepo: ParcelRepository
) {
    
    @Volatile
    private var parcelsByLabel: Map<String, ParcelStatus> = emptyMap()
    
    private var parcelsByOwner: Map<String, String> = emptyMap()
    
    fun load() {
        parcelsByLabel = parcelRepo.findAll().associate { it.label to it.status }
        
        parcelsByOwner = parcelRepo.findAllWithOwners()
            .mapNotNull { p -> p.ownerUuid?.let { p.label to it} }
            .toMap()
    }
    
    fun parcelLabels(prefix: String): List<String> =
        parcelsByLabel.keys.filter { it.startsWith(prefix, ignoreCase = true)}
    
    fun parcelsLabelsOfStatus(status: ParcelStatus, prefix: String): List<String> =
        parcelsByLabel.filterValues { it == status }
            .keys.filter { it.startsWith(prefix, ignoreCase = true) }
    
    fun parcelsLabelsNotOfStatus(status: ParcelStatus, prefix: String): List<String> =
        parcelsByLabel.filterValues { it != status }
            .keys.filter { it.startsWith(prefix, ignoreCase = true) }
            
    fun parcelsByOwner(senderUuid: String, prefix: String): List<String> =
        parcelsByOwner.filterValues { it == senderUuid }
            .keys.filter {it.startsWith(prefix, ignoreCase = true)}
            
    fun onParcelAdded(label: String) {
        parcelsByLabel = parcelsByLabel + (label to ParcelStatus.AVAILABLE)
    }
    
    fun onParcelStatusChanged(label: String, newStatus: ParcelStatus) {
        parcelsByLabel = parcelsByLabel + (label to newStatus)
    }
    
    fun onParcelRented(label: String, ownerUuid: String) {
        parcelsByOwner = parcelsByOwner + (label to ownerUuid)
        parcelsByLabel = parcelsByLabel + (label to ParcelStatus.RENTED)
    }
    
    fun onParcelUnrented(label: String) {
        parcelsByOwner = parcelsByOwner - label
        parcelsByLabel = parcelsByLabel + (label to ParcelStatus.AVAILABLE)
    }
    
    fun onParcelRemoved(label: String) {
        parcelsByLabel = parcelsByLabel - label
    }
}