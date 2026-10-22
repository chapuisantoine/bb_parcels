package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.database.repository.RentalMemberRepository
import com.bbparcels.database.repository.RentalRepository
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class ListManageMemberGui(
    private val parcel: ParcelWithOwner,
    private val members: List<String>,
    private val page: Int
) : BbGui("[BB] Gestion des membres") {
    
    private val totalPages = ((members.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)

    init {
        Utils.fillBorders(inventory)
        members
            .drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { index, member ->
                val isOwner = member == parcel.ownerUuid
                inventory.setItem(CONTENT_SLOTS[index], Utils.buildHeadItemWithLore(member, isOwner))
            }
        if (page > 0)
            inventory.setItem(SLOT_PREV, Utils.buildNavItem(Material.ARROW, "§ePage précédente"))
        if (page < totalPages)
            inventory.setItem(SLOT_NEXT, Utils.buildNavItem(Material.ARROW, "§ePage suivante"))

        inventory.setItem(SLOT_RETURN, Utils.buildNavItem(Material.BARRIER, "§7Retour"))
    }

    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_PREV   -> ListManageMemberGui(parcel, members, page - 1).open(player)
            SLOT_NEXT   -> ListManageMemberGui(parcel, members, page + 1).open(player)
            SLOT_RETURN -> OwnerMenuGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val targetUuid = members.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)) ?: return
                removeMember(player, targetUuid, parcel)
            }
        }
    }
        
    private fun removeMember(player: Player, targetUuid: String, parcel: ParcelWithOwner): Boolean {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            val isTenant = rental != null && rental.tenantUuid == player.uniqueId.toString()
            val listMembers = if (isTenant) rentalMemberRepo.findMembers(parcel.id) else emptyList()
            val targetIsTenant = rental != null && targetUuid == rental.tenantUuid
            
            val deleted = when {
                !isTenant || targetIsTenant || targetUuid !in listMembers -> false
                else -> rentalMemberRepo.deleteMember(parcel.label, targetUuid)
            }
            val targetName = Utils.playerName(targetUuid)
            val uuid = UUID.fromString(targetUuid)
            val message = when {
                !isTenant -> "§cVous n'êtes plus locataire de cette parcelle."
                targetIsTenant -> "§cImpossible de retirer le locataire de la parcelle."
                targetUuid !in listMembers -> "§e$targetName §cn'est pas membre de cette parcelle."
                !deleted -> "§cErreur lors de la suppression."
                else -> "§a✔ §e$targetName §aa été retiré de la parcelle §e${parcel.label}§a."
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (deleted) {
                    Bukkit.getWorld(parcel.world)?.let { world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.removePlayer(uuid)
                    }
                    plugin.memberCache.onMemberRemoved(parcel.label, targetUuid)
                    Bukkit.getPlayer(uuid)?.sendMessage("§aVous avez été retiré de la parcelle §e${parcel.label}§a.")
                }
                player.sendMessage(message)
                if (isTenant) OwnerMenuGui.open(player, parcel)
            })
        })
        return true
    }

    companion object {
        private val plugin get() = BbParcels.instance
        
        private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
        private const val SLOT_PREV = 39
        private const val SLOT_NEXT = 41
        private const val SLOT_RETURN = 43
        
        private val rentalRepo by lazy { RentalRepository(BbParcels.instance.databaseManager)}
        private val rentalMemberRepo by lazy {RentalMemberRepository(BbParcels.instance.databaseManager)}

    }
}