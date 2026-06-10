package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.ParcelWithOwner
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalMemberRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

object ConfirmUnrentGui {
    
    private val cachedParcel = mutableMapOf<UUID, ParcelWithOwner>()
    
    private val parcelRepo = ParcelRepository(BbParcels.instance.databaseManager)
    private val rentalRepo = RentalRepository(BbParcels.instance.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(BbParcels.instance.databaseManager)

    
    private val BORDER_SLOTS  = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    
    private const val SLOT_HEAD = 13
    private const val SLOT_VALID = 30
    private const val SLOT_UNVALID = 32
    
    fun open(player: Player, parcel: ParcelWithOwner) {
        cachedParcel[player.uniqueId]  = parcel
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Résilier la parcelle ?")
        
        fillBorders(inv)
        
        if (parcel.ownerUuid != null) {
            val tenant = Bukkit.getOfflinePlayer(UUID.fromString(parcel.ownerUuid))
            inv.setItem(SLOT_HEAD, buildHeadSkull(tenant.name ?: parcel.ownerUuid, UUID.fromString(parcel.ownerUuid)))
        }
        
        inv.setItem(SLOT_VALID, buildNavItem(Material.GREEN_CONCRETE, "Valider"))
        inv.setItem(SLOT_UNVALID, buildNavItem(Material.RED_CONCRETE, "Annuler"))
        
        player.openInventory(inv)
    }
    
    fun handleClick(player: Player, slot: Int) {
        val parcel = cachedParcel[player.uniqueId] ?: return
        when (slot) {
            SLOT_VALID -> {
                unrentParcel(player, parcel)
                AgentMenuGui.open(player)
                }
            SLOT_UNVALID -> ParcelDetailGui.open(player, parcel)
        }
    }
    
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
    private fun buildHeadSkull(name: String, uuid: UUID): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(uuid)
        meta.setDisplayName("§f$name")
        item.itemMeta = meta
        return item
    }
    
    private fun fillBorders(inv: Inventory) {
        for (slot in BORDER_SLOTS) {
            val material = if (slot % 2 == 0) Material.BLUE_STAINED_GLASS_PANE else Material.CYAN_STAINED_GLASS_PANE
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§r")
            item.itemMeta = meta
            inv.setItem(slot, item)
        }
    }
    
    private fun unrentParcel(player: Player, parcel: ParcelWithOwner): Boolean {
        if (parcel.status != ParcelStatus.RENTED){
            player.sendMessage("§cLa parcelle §e${parcel.label} §cn'est pas en cours de location.")
            return true
        }
        
        val rental = rentalRepo.findActiveByParcelId(parcel.id)
        if (rental == null) {
            player.sendMessage("§cAucune location active trouvée pour §e${parcel.label}§c.")
            return true
        }
        
        rentalRepo.terminateRental(rental.id)
        parcelRepo.updateParcelStatus(parcel.id, ParcelStatus.AVAILABLE)
        
        val allMemberUuids = rentalMemberRepo.findMembersByRentalId(rental.id)
        
        val world = Bukkit.getWorld(parcel.world)
        if (world != null) {
            val regionManager = WorldGuard.getInstance()
                .platform.regionContainer
                .get(BukkitAdapter.adapt(world))
            val region = regionManager?.getRegion(parcel.regionId)
            if (region != null) {
                allMemberUuids.forEach { uuid ->
                    region.members.removePlayer(UUID.fromString(uuid))
                }
                regionManager.save()
            }
        }
        
        rentalMemberRepo.deleteAllByRentalId(rental.id)
        val parcelOwner = Bukkit.getOfflinePlayer(rental.tenantUuid).player?: return true
        player.sendMessage("§a✔ La location §e${parcel.label} §ade §e${parcelOwner.name} §aa été résiliée.")
        return true
    }
    
}