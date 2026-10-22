package com.bbparcels.cache

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.RentalMemberRepository
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class MemberCache(
    private val rentalMemberRepo: RentalMemberRepository,
    private val serverId: String = BbParcels.instance.pluginConfig.serverId
) {
    
    @Volatile
    private var membersByParcel: Map<String, List<String>> = emptyMap()
    
    fun load() {
        membersByParcel = rentalMemberRepo.getAllMembersParcels(serverId)
            .groupBy({ it.first })  { it.second } 
    }
    
    fun membersOf(label: String, sender: Player, prefix: String): List<String> =
        membersByParcel[label].orEmpty()
            .map { UUID.fromString(it) }
            .filter { it != sender.uniqueId }
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }
            .filter {it.startsWith(prefix, ignoreCase = true)}
            
    fun membersNotOf(label: String, sender: Player, prefix: String): List<String> {
        val members = membersByParcel[label].orEmpty().toSet()
        return Bukkit.getOnlinePlayers()
            .filter { it.uniqueId.toString() !in members }
            .filter { it.uniqueId != sender.uniqueId }
            .map { it.name }
            .filter { it.startsWith(prefix, ignoreCase = true) }
    }
    
    fun onMemberAdded(label: String, memberUuid: String) {
        val current = membersByParcel[label].orEmpty()
        if (memberUuid in current) return
        membersByParcel = membersByParcel + (label to (current + memberUuid))
    }
    
    fun onMemberRemoved(label: String, memberUuid: String) {
        val current = membersByParcel[label] ?: return
        membersByParcel = membersByParcel + (label to (current - memberUuid))
    }
    
    fun onRentalEnded(label: String) {
        membersByParcel = membersByParcel - label
    }
}