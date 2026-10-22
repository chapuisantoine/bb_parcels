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

class ListAddMemberGui (
    private val parcel: ParcelWithOwner,
    private val nearbyPlayers: List<String>,
    private val page: Int
) : BbGui("[BB] Ajout de membres") {
    
    private val totalPages = ((nearbyPlayers.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)

    init {
        Utils.fillBorders(inventory)
        nearbyPlayers.drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { i, player -> 
                inventory.setItem(CONTENT_SLOTS[i], Utils.buildHeadItem(player))
            }
        if (page > 0)
            inventory.setItem(SLOT_PREV, Utils.buildNavItem(Material.ARROW, "§ePage précédente"))
        if (page < totalPages)
            inventory.setItem(SLOT_NEXT, Utils.buildNavItem(Material.ARROW, "§ePage suivante"))
        inventory.setItem(SLOT_RETURN, Utils.buildNavItem(Material.BARRIER, "§7Retour"))
    }

    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_PREV   -> ListAddMemberGui(parcel, nearbyPlayers, page - 1).open(player)
            SLOT_NEXT   -> ListAddMemberGui(parcel, nearbyPlayers, page + 1).open(player)
            SLOT_RETURN -> OwnerMenuGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val target = Bukkit.getPlayer(
                    nearbyPlayers.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)) ?: return
                )?: return player.sendMessage("§cCe joueur n'est plus connecté.")
                addMember(player, target, parcel)
            }
        }
    }
    
        
    private fun addMember(player: Player, target: Player, parcel: ParcelWithOwner): Boolean {
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            val membersUuid = rentalMemberRepo.findMembers(parcel.id)
            val isNotOwner = rental == null || rental.tenantUuid != player.uniqueId.toString()
            val isAlreadyMember = target.uniqueId.toString() in membersUuid
            val added = when {
                rental == null || isNotOwner || isAlreadyMember -> false
                else -> {
                    rentalMemberRepo.addMember(
                        RentalMember(
                            rentalId = rental.id,
                            memberUuid = target.uniqueId.toString(),
                            role = RentalMemberRole.MEMBER
                        )
                    )
                }
            }
            
            val message = when {
                rental == null -> "§cCette parcelle n'a pas de location active."
                isAlreadyMember -> "§cCe joueur fait déjà partie de la parcelle."
                isNotOwner -> "§cVous n'êtes plus locataire de cette parcelle."
                !added -> "§cErreur lors de l'ajout du joueur."
                else -> "§a✔ Le joueur §e${target.name} §aa été ajouté à la parcelle §e${parcel.label} §a!"
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (added) {
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.addPlayer(target.uniqueId)
                    }
                    plugin.memberCache.onMemberAdded(parcel.label, target.uniqueId.toString())
                    target.sendMessage("§a✔ Vous avez été ajouté à la parcelle §e${parcel.label} §a!")
                }
                player.sendMessage(message)
                OwnerMenuGui.open(player, parcel)
            })
             
        })
        
        return true
    }
    
    companion object {
        private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
        private const val SLOT_PREV = 39
        private const val SLOT_NEXT = 41
        private const val SLOT_RETURN = 43
        
        private val plugin get() = BbParcels.instance
        
        private val rentalRepo by lazy {RentalRepository(BbParcels.instance.databaseManager)}
        private val rentalMemberRepo by lazy {RentalMemberRepository(BbParcels.instance.databaseManager)}
        
    }
}