package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.database.repository.ProfileInfo
import com.bbparcels.database.repository.ProfileRepository
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ChooseTenantGui (
    private val parcel: ParcelWithOwner,
    private val candidates: List<ProfileInfo>,
    private val page: Int
) : BbGui("[BB] Choisir un locataire") {
    
    override val permission = "bbparcels.agentImmo"
    
    private val totalPages = ((candidates.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)
    
    init {
        Utils.fillBorders(inventory)
        candidates.drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { i, candidate -> 
                inventory.setItem(CONTENT_SLOTS[i], Utils.buildHeadItem(candidate))
            }
        if (page > 0) 
            inventory.setItem(SLOT_PREV, Utils.buildNavItem(Material.ARROW, "§ePage précédente"))
        if (page < totalPages)
            inventory.setItem(SLOT_NEXT, Utils.buildNavItem(Material.ARROW, "§ePage suivante"))
        inventory.setItem(SLOT_RETURN, Utils.buildNavItem(Material.BARRIER, "§7Retour"))
    }
    
    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_PREV   -> ChooseTenantGui(parcel, candidates, page - 1).open(player)
            SLOT_NEXT   -> ChooseTenantGui(parcel, candidates, page + 1).open(player)
            SLOT_RETURN -> ParcelDetailGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                    val target = candidates.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot))?: return
                    ConfirmRentGui(parcel, target).open(player)
            }
        }
    }
    
    companion object {
        private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
        private const val SLOT_PREV = 39
        private const val SLOT_NEXT = 41
        private const val SLOT_RETURN = 43
        private const val RADIUS = 20.0
        
        private val plugin get() = BbParcels.instance
        private val profileRepo by lazy { ProfileRepository(BbParcels.instance.databaseManager) }
        
        fun open(player: Player, parcel: ParcelWithOwner) {
            val nearbyUuids = player.getNearbyEntities(RADIUS, RADIUS, RADIUS)
                .filterIsInstance<Player>()
                .map { it.uniqueId.toString() }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val candidates = profileRepo.findActiveProfiles(nearbyUuids)

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    ChooseTenantGui(parcel, candidates, 0).open(player)
                })
            })
        }
    }
}
