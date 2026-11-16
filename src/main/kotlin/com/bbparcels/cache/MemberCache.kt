package com.bbparcels.cache

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ProfileInfo
import com.bbparcels.database.repository.RentalMemberRepository
import java.util.concurrent.ConcurrentHashMap

class MemberCache(
    private val rentalMemberRepo: RentalMemberRepository,
    private val serverId: String = BbParcels.instance.pluginConfig.serverId
) {
    
    private var membersByParcel = ConcurrentHashMap<String, List<ProfileInfo>>()
    
    fun load() {
        val fresh = rentalMemberRepo.getAllMembersParcels(serverId).groupBy({ it.first }, { it.second })
        membersByParcel.clear()
        membersByParcel.putAll(fresh)
    }
    
    fun membersOf(label: String): List<ProfileInfo> = membersByParcel[label].orEmpty()

    fun onMemberAdded(label: String, member: ProfileInfo) {
        membersByParcel.merge(label, listOf(member)) {
            old, new -> if (old.any { it.id == member.id }) old else old + new
        }
    }
    
    fun onMemberRemoved(label: String, profileId: Int) {
        membersByParcel.computeIfPresent(label) { _, list -> list.filterNot { it.id == profileId } }
    }
    
    fun onRentalEnded(label: String) {
        membersByParcel.remove(label)
    }
}