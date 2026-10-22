package com.bbparcels.gui.utils

import com.bbparcels.database.repository.ParcelStatus
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

object Utils {

    private val BORDER_SLOTS = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    
    fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
    fun fillBorders(inv: Inventory) {    
        for (slot in BORDER_SLOTS) {
            val material = if (slot % 2 == 0 ) Material.BLUE_STAINED_GLASS_PANE else Material.CYAN_STAINED_GLASS_PANE
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§r")
            item.itemMeta = meta
            inv.setItem(slot, item)
        }
    }
    
    fun buildHeadItemWithLore(uuid: String, isOwner: Boolean): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
        meta.setDisplayName("§e${playerName(uuid)}")
        if (!isOwner)
            meta.lore = listOf("§eCliquer pour enlever")
        item.itemMeta = meta
        return item
    }
    
    fun buildHeadItem(uuid: String): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
        meta.setDisplayName("§e${playerName(uuid)}")
        item.itemMeta = meta
        return item
    }
    
    fun statusLabel(status: ParcelStatus) = when (status) {
        ParcelStatus.AVAILABLE -> "§aDisponible"
        ParcelStatus.RENTED    -> "§cLouée"
        ParcelStatus.DISABLED  -> "§7Désactivée"
    }
    
    fun buildListMembersItem(memberUuids: List<String>): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta!!
        meta.setDisplayName("§fMembres :")
        meta.lore = if (memberUuids.isEmpty())
            listOf("§7     Aucun membre")
        else
            memberUuids.map { uuid ->
                val name = playerName(uuid)
                "§7     $name"
            }
        item.itemMeta = meta
        return item
    }
    
    fun playerName(uuid: String): String =
        Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name ?: "Inconnu (${uuid.take(8)})"
}