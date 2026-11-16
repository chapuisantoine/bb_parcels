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
    private val candidates: List<ProfileInfo>,
    private val page: Int
) : BbGui("[BB] Ajout de membres") {
    
    private val totalPages = ((candidates.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)

    init {
        Utils.fillBorders(inventory)
        candidates.drop(page * CONTENT_SLOTS.size)
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
            SLOT_PREV   -> ListAddMemberGui(parcel, candidates, page - 1).open(player)
            SLOT_NEXT   -> ListAddMemberGui(parcel, candidates, page + 1).open(player)
            SLOT_RETURN -> OwnerMenuGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val target = candidates.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot))?: return 
                addMember(player, target, parcel)
            }
        }
    }
    
        
    private fun addMember(player: Player, target: ProfileInfo, parcel: ParcelWithOwner): Boolean {
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val playerProfile = profileRepo.findActiveProfile(player.uniqueId.toString())
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            val members = rentalMemberRepo.findMembers(parcel.id)
            val isNotOwner = rental == null || rental.tenantProfileId != playerProfile?.id
            val isAlreadyMember = members.any { it.id == target.id }
            val added = when {
                rental == null || isNotOwner || isAlreadyMember -> false
                else -> {
                    rentalMemberRepo.addMember(
                        RentalMember(
                            rentalId = rental.id,
                            memberProfileId = target.id,
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
                else -> "§a✔ Le joueur §e${target.displayName} §aa été ajouté à la parcelle §e${parcel.label} §a!"
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (added) {
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.addPlayer(target.accountId)
                    }
                    plugin.memberCache.onMemberAdded(parcel.label, target)
                    Bukkit.getPlayer(target.accountId)?.sendMessage("§aVous avez été ajouté à la parcelle §e${parcel.label}§a.")
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
        private const val RADIUS = 20.0
        
        private val plugin get() = BbParcels.instance
        
        private val rentalRepo by lazy {RentalRepository(BbParcels.instance.databaseManager)}
        private val rentalMemberRepo by lazy {RentalMemberRepository(BbParcels.instance.databaseManager)}
        private val profileRepo by lazy { ProfileRepository(BbParcels.instance.databaseManager) }

        
        fun open(player: Player, parcel: ParcelWithOwner) {
            val nearbyUuids = player.getNearbyEntities(RADIUS, RADIUS, RADIUS)
                .filterIsInstance<Player>()
                .map { it.uniqueId.toString() }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val members = rentalMemberRepo.findMembers(parcel.id)
                val candidates = profileRepo.findActiveProfiles(nearbyUuids)
                    .filter { it !in members}

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    ListAddMemberGui(parcel, candidates, 0).open(player)
                })
            })
        }
    }
}