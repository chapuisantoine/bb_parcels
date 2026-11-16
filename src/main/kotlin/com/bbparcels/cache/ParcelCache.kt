package com.bbparcels.cache

import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ParcelStatus
import java.util.concurrent.ConcurrentHashMap

class ParcelCache(
    private val parcelRepo: ParcelRepository
) {
    
    private data class Entry(val status: ParcelStatus, val tenantProfileId: Int?)
    
    private val parcels = ConcurrentHashMap<String, Entry>()
    
    fun load() {
        val fresh = parcelRepo.findAllWithOwners().associate { it.label to Entry(it.status, it.tenantProfile?.id) }
        parcels.clear()
        parcels.putAll(fresh)
    }
    
    fun parcelLabels(prefix: String) = matching(prefix) { true }
    fun parcelsLabelsOfStatus(status: ParcelStatus, prefix: String) = matching(prefix) { it.status == status }
    fun parcelsLabelsNotOfStatus(status: ParcelStatus, prefix: String) = matching(prefix) { it.status != status }
    fun parcelsRentedBy(profileId: Int, prefix: String) = matching(prefix) { it.tenantProfileId == profileId }

    
    private fun matching(prefix: String, filter: (Entry) -> Boolean): List<String> =
        parcels.filter { (label, entry) -> filter(entry) && label.startsWith(prefix, ignoreCase = true) }
            .keys.sorted()
              
    fun onParcelAdded(label: String) { 
        parcels[label] = Entry(ParcelStatus.AVAILABLE, null) 
    }
    fun onParcelStatusChanged(label: String, status: ParcelStatus) {
        parcels.computeIfPresent(label) { _, e -> e.copy(status = status)}
    }
    
    fun onParcelRented(label: String, profileId: Int) { 
        parcels.computeIfPresent(label) {_, _ -> Entry(ParcelStatus.RENTED, profileId)}
    }
    
    fun onParcelUnrented(label: String) {
        parcels.computeIfPresent(label) { _, _ -> Entry(ParcelStatus.AVAILABLE, null)}
    }
    
    fun onParcelRemoved(label: String){ 
        parcels.remove(label) 
    }
}