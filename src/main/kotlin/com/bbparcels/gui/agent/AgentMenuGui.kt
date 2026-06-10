package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.ParcelWithOwner
import com.bbparcels.repository.ParcelRepository
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object AgentMenuGui {
    
    private val parcelRepo = ParcelRepository(BbParcels.instance.databaseManager)
    
    private val playerPages = mutableMapOf<UUID, Int>()
    
    private val BORDER_SLOTS = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
    private const val SLOT_PREV = 39
    private const val SLOT_NEXT = 41
    
    //gestion globale de l'inventaire de l'app Immo
    fun open(player: Player, page: Int = 0) {
        val parcels = parcelRepo.findAllWithOwners()
        val totalPages = ((parcels.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)
        val currentPage = page.coerceIn(0, totalPages)
        playerPages[player.uniqueId] = currentPage
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Parcelles")
        
        fillBorders(inv)
        
        parcels
            .drop(currentPage * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { index, parcel ->
                inv.setItem(CONTENT_SLOTS[index], buildParcelItem(parcel)) 
        }
        
        if (currentPage > 0)
            inv.setItem(SLOT_PREV, buildNavItem(Material.ARROW, "§ePage précédente"))
        if (currentPage < totalPages)
            inv.setItem(SLOT_NEXT, buildNavItem(Material.ARROW, "§ePage suivante"))
        
        player.openInventory(inv)
    }
    
    // gestion des interactions au click sur les items de l'inventaire
    fun handleClick(player: Player, slot: Int) {
        val page = playerPages[player.uniqueId] ?: 0
        when (slot) {
            SLOT_PREV -> open(player, page - 1)
            SLOT_NEXT -> open(player, page + 1)
            in CONTENT_SLOTS -> {
                val parcels = parcelRepo.findAllWithOwners()
                val index = page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)
                val parcel = parcels.getOrNull(index) ?: return
                ParcelDetailGui.open(player, parcel)
            }
        }
    }
    
    // définition de l'item "application" de l'agent Immo
    private fun buildParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = when(parcel.status.name) {
            "AVAILABLE" -> Material.LIME_CONCRETE
            "RENTED" -> Material.RED_CONCRETE
            else -> Material.GRAY_CONCRETE
        }
        
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§f${parcel.label}")
        
        meta.lore = buildList { 
            add("§7Statut : §e${statusLabel(parcel.status)}")
            if (parcel.ownerUuid != null) {
                val ownerName = Bukkit.getOfflinePlayer(UUID.fromString(parcel.ownerUuid)).name ?: parcel.ownerUuid
                add("§7Locataire : §a$ownerName")
            }
            
            add("")
            add("§eCliquez pour voir le détail")
        }
        item.itemMeta = meta
        return item
    }
    
    // définition de l'item de pagination
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
    //contour de l'inventaire en Verre Bleu et Cyan
    private fun fillBorders(inv: Inventory) {    
        for (slot in BORDER_SLOTS) {
            val material = if (slot % 2 == 0 ) Material.BLUE_STAINED_GLASS_PANE else Material.CYAN_STAINED_GLASS_PANE
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§r")
            item.itemMeta = meta
            inv.setItem(slot, item)
        }
    }
    
    //traduction des status
    private fun statusLabel(status: ParcelStatus) = when (status) {
        ParcelStatus.AVAILABLE -> "§aDisponible"
        ParcelStatus.RENTED    -> "§cLouée"
        ParcelStatus.DISABLED  -> "§7Désactivée"
    }
}