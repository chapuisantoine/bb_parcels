package com.bbparcels.gui.utils

import com.bbparcels.database.repository.ParcelStatus
import com.bbparcels.database.repository.ProfileInfo
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

object Utils {

    private val BORDER_SLOTS = buildList {
        addAll((0..8).toList())
        addAll((45..53).toList())
        addAll(listOf(9, 18, 27, 36, 17, 26, 35, 44))
    }
    
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
    
    fun buildHeadItemWithLore(profile: ProfileInfo, isOwner: Boolean): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(profile.accountId)
        meta.setDisplayName("§e${profile.displayName}")
        if (!isOwner)
            meta.lore = listOf("§eCliquer pour enlever")
        item.itemMeta = meta
        return item
    }
    
    fun buildHeadItem(profile: ProfileInfo): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = Bukkit.getOfflinePlayer(profile.accountId)
        meta.setDisplayName("§e${profile.displayName}")
        item.itemMeta = meta
        return item
    }
    
    fun statusLabel(status: ParcelStatus) = when (status) {
        ParcelStatus.AVAILABLE -> "§aDisponible"
        ParcelStatus.RENTED    -> "§cLouée"
        ParcelStatus.DISABLED  -> "§7Désactivée"
    }
    
    fun buildListMembersItem(members: List<ProfileInfo>): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta!!
        meta.setDisplayName("§fMembres :")
        meta.lore = if (members.isEmpty())
            listOf("§7     Aucun membre")
        else
            members.map { member -> "§7     ${member.displayName}"}
        item.itemMeta = meta
        return item
    }
    
    fun playerName(uuid: String): String =
        Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name ?: "Inconnu (${uuid.take(8)})"
}