package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelWithOwner
import com.bbparcels.repository.RentalMemberRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

object ListManageMemberGui {
    
    private val rentalMemberRepo = RentalMemberRepository(BbParcels.instance.databaseManager)

    private val playerPages   = mutableMapOf<UUID, Int>()
    private val cachedParcel = mutableMapOf<UUID, ParcelWithOwner>()
    private val cachedMembers = mutableMapOf<UUID, List<Player>>()
    
    private val BORDER_SLOTS = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
    
    private const val SLOT_PREV = 39
    private const val SLOT_NEXT = 41
    private const val SLOT_RETURN = 43
    
    fun open(player: Player, members: List<Player>, parcel: ParcelWithOwner, page: Int = 0) {
        
        cachedMembers[player.uniqueId] = members
        cachedParcel[player.uniqueId] = parcel
        
        val totalPages  = ((members.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)
        val currentPage = page.coerceIn(0, totalPages)
        playerPages[player.uniqueId] = currentPage
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Gestion des membres")
        fillBorders(inv)
        
        members
            .drop(currentPage * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { index, member ->
                val isOwner = member.uniqueId.toString() == parcel.ownerUuid
                inv.setItem(CONTENT_SLOTS[index], buildHeadItem(member, isOwner))
            }
        
        if (currentPage > 0)
            inv.setItem(SLOT_PREV, buildNavItem(Material.ARROW, "§ePage précédente"))
        if (currentPage < totalPages)
            inv.setItem(SLOT_NEXT, buildNavItem(Material.ARROW, "§ePage suivante"))

        inv.setItem(SLOT_RETURN, buildNavItem(Material.BARRIER, "§7Retour"))
        
        player.openInventory(inv)
    }
    
    fun handleClick(player: Player, slot: Int) {
        val page    = playerPages[player.uniqueId] ?: 0
        val members = cachedMembers[player.uniqueId] ?: return
        val parcel  = cachedParcel[player.uniqueId] ?: return
        when (slot) {
            SLOT_PREV   -> open(player, members, parcel, page - 1)
            SLOT_NEXT   -> open(player, members, parcel, page + 1)
            SLOT_RETURN -> OwnerMenuGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val index  = page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)
                val target = members.getOrNull(index) ?: return
                deleteMember(player, target, parcel) //TODO faire une confirmation ?
            }
        }
    }
    
    private fun buildHeadItem(member: Player, isOwner: Boolean): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = member
        meta.setDisplayName("§e${member.name}")
        if (!isOwner)
            meta.lore = listOf("§eCliquer pour enlever")
        item.itemMeta = meta
        return item
    }
    
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
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
    
    private fun deleteMember(player: Player, target: Player, parcel: ParcelWithOwner): Boolean {

        if (player == target) {
            player.sendMessage("§cMais c'est vous !")
            return true
        }
        
        val membersUuid = rentalMemberRepo.findMembers(parcel.label)
        val targetUuid = membersUuid.firstOrNull { uuid ->
            Bukkit.getPlayer(UUID.fromString(uuid))?.name
                ?.equals(target.name, ignoreCase = true) == true
        }
        if (targetUuid == null) {
            player.sendMessage("§c§e${target.name} §cn'est pas membre de cette parcelle.")
            return true
        }

        val deleted = rentalMemberRepo.deleteMember(parcel.label, targetUuid)
        val world = Bukkit.getWorld(parcel.world)
        if (world != null) {
            val regionManager = WorldGuard.getInstance()
                .platform.regionContainer
                .get(BukkitAdapter.adapt(world))
            val region = regionManager?.getRegion(parcel.regionId)
            if (region != null) {
                region.members.removePlayer(UUID.fromString(targetUuid))
                regionManager.save()
            }
        }
        
        if (deleted) {
            player.sendMessage("§a✔ §e${target.name} §aa été retiré de la parcelle §e${parcel.label}§a.")
            target.sendMessage("§aVous avez été retiré de la parcelle §e${parcel.label}§a.")
        }
        else
            player.sendMessage("§cErreur lors de la suppression.")
        return true
    }
}