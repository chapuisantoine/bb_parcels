package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.ParcelWithOwner
import com.bbparcels.repository.ParcelRepository
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class OwnerGuiListener : Listener {
    
    private val parcelRepo = ParcelRepository(BbParcels.instance.databaseManager)
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title
        if (!title.startsWith("[BB]")) return
        event.isCancelled = true
        
        val player = event.whoClicked as? Player ?: return
        if (event.currentItem == null) return
        
        when (title) {
            "[BB] Ma parcelle" -> OwnerMenuGui.handleClick(player, event.rawSlot)
            "[BB] Liste des membres" -> ListMembersGui.handleClick(player, event.rawSlot)
            "[BB] Ajout de membres" -> ListAddMemberGui.handleClick(player, event.rawSlot)
            "[BB] Gestion des membres" -> ListManageMemberGui.handleClick(player, event.rawSlot)
        }
        
    }
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player        
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        
        val item = player.inventory.itemInMainHand       
        val label = getParcelLabel(item) ?: return
        
        event.isCancelled = true
        
        val parcel = parcelRepo.findAllWithOwners().firstOrNull { it.label == label } ?: return
        
        if (parcel.status != ParcelStatus.RENTED) return
        if (parcel.ownerUuid != player.uniqueId.toString()) return
        
        OwnerMenuGui.open(player, parcel =parcel)        
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
