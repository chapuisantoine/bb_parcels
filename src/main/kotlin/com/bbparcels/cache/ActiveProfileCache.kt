package com.bbparcels.cache

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ProfileInfo
import com.bbparcels.database.repository.ProfileRepository
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ActiveProfileCache(
    private val plugin: BbParcels,
    private val profileRepo: ProfileRepository
) : Listener {
    
    private val activeProfiles = ConcurrentHashMap<UUID, ProfileInfo>()
    
    fun get(playerUuid: UUID): ProfileInfo? = activeProfiles[playerUuid]
    
    fun refresh(playerUuid: UUID) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val profile = profileRepo.findActiveProfile(playerUuid.toString())
            if (profile == null) activeProfiles.remove(playerUuid) else activeProfiles[playerUuid] = profile
        })
    }
    
    fun refreshAllOnline() = Bukkit.getOnlinePlayers().forEach { refresh(it.uniqueId) }
    
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        // bb_identity valide le profil actif à la connexion : on le lit juste après lui
        Bukkit.getScheduler().runTaskLater(plugin, Runnable{ refresh(event.player.uniqueId)}, 40L)
    }
    
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        activeProfiles.remove(event.player.uniqueId)
    }
    
}