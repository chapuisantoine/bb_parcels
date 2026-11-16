package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.*
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ListManageMemberGui(
    private val parcel: ParcelWithOwner,
    private val candidates: List<ProfileInfo>,
    private val page: Int
) : BbGui("[BB] Gestion des membres") {
    
    private val totalPages = ((candidates.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)

    init {
        Utils.fillBorders(inventory)
        candidates
            .drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { index, member ->
                val isOwner = member == parcel.tenantProfile
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
            SLOT_PREV   -> ListManageMemberGui(parcel, candidates, page - 1).open(player)
            SLOT_NEXT   -> ListManageMemberGui(parcel, candidates, page + 1).open(player)
            SLOT_RETURN -> OwnerMenuGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val target = candidates.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)) ?: return
                removeMember(player, target, parcel)
            }
        }
    }
        
    private fun removeMember(player: Player, target: ProfileInfo, parcel: ParcelWithOwner): Boolean {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val playerProfile = profileRepo.findActiveProfile(player.uniqueId.toString())
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            val isTenant = rental != null && playerProfile != null && rental.tenantProfileId == playerProfile.id
            val listMembers = if (isTenant) rentalMemberRepo.findMembers(parcel.id) else emptyList()
            val targetIsTenant = rental != null && target.id == rental.tenantProfileId
            val isNotMember = listMembers.none { it.id == target.id }
            val deleted = when {
                !isTenant || targetIsTenant || isNotMember -> false
                else -> rentalMemberRepo.deleteMember(parcel.label, target.id)
            }
            val message = when {
                !isTenant -> "§cVous n'êtes plus locataire de cette parcelle."
                targetIsTenant -> "§cImpossible de retirer le locataire de la parcelle."
                isNotMember -> "§e${target.displayName} §cn'est pas membre de cette parcelle."
                !deleted -> "§cErreur lors de la suppression."
                else -> "§a✔ §e${target.displayName} §aa été retiré de la parcelle §e${parcel.label}§a."
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (deleted) {
                    Bukkit.getWorld(parcel.world)?.let { world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.removePlayer(target.accountId)
                    }
                    plugin.memberCache.onMemberRemoved(parcel.label, target.id)
                    Bukkit.getPlayer(target.accountId)?.sendMessage("§aVous avez été retiré de la parcelle §e${parcel.label}§a.")
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
        private val profileRepo by lazy { ProfileRepository(BbParcels.instance.databaseManager) }


    }
}