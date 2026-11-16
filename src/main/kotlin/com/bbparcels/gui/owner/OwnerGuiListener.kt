package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ParcelStatus
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.database.repository.ProfileRepository
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class OwnerGuiListener : Listener {
    
    private val plugin = BbParcels.instance
    
    private val parcelRepo = ParcelRepository(BbParcels.instance.databaseManager)
    private val profileRepo = ProfileRepository(BbParcels.instance.databaseManager)
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player      
          
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        
        val item = player.inventory.itemInMainHand       
        val label = getParcelLabel(item) ?: return
        
        event.isCancelled = true

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabelWithOwner(label)
            val activeProfile = profileRepo.findActiveProfile(player.uniqueId.toString())
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val isTenant = parcel != null && parcel.status == ParcelStatus.RENTED &&
                    activeProfile != null && parcel.tenantProfile?.id == activeProfile.id
                if (isTenant) OwnerMenuGui.open(player, parcel)
                else player.sendMessage("§cCette clé n'est pas valide.")
            })
        })
    }
    
    companion object {
        private val PARCEL_KEY = NamespacedKey(BbParcels.instance, "parcel_label")

        fun createParcelKey(parcel: ParcelWithOwner): ItemStack {
            val item = ItemStack(Material.TRIPWIRE_HOOK)
            val meta = item.itemMeta!!
            meta.setDisplayName("§6Clé — §f${parcel.label}")
            meta.lore = listOf("§7Clic droit pour gérer votre parcelle")
            meta.persistentDataContainer.set(PARCEL_KEY, PersistentDataType.STRING, parcel.label)
            item.itemMeta = meta
            return item
        }
        
        fun getParcelLabel(item: ItemStack): String? {
            return item.itemMeta?.persistentDataContainer?.get(PARCEL_KEY, PersistentDataType.STRING)
        }
    }
}
