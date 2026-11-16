package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.*
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.owner.OwnerGuiListener
import com.bbparcels.gui.utils.Utils
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime

class ConfirmRentGui(
    private val parcel: ParcelWithOwner,
    private val target: ProfileInfo
) : BbGui("[BB] Louer la parcelle ?") {
    
    override val permission = "bbparcels.agentImmo"
            
    init {
        Utils.fillBorders(inventory)
        inventory.setItem(SLOT_HEAD, Utils.buildHeadItem(target))
        inventory.setItem(SLOT_VALID, Utils.buildNavItem(Material.GREEN_CONCRETE, "Valider"))
        inventory.setItem(SLOT_CANCEL, Utils.buildNavItem(Material.RED_CONCRETE, "Annuler"))
    }
    
    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_VALID  -> { 
                player.closeInventory()
                rentParcel(player) 
            }
            SLOT_CANCEL -> ParcelDetailGui.open(player, parcel)
        }
    }
    
    private fun rentParcel(player: Player) {
        if (Bukkit.getPlayer(target.accountId) == null) {
            player.sendMessage("§cLe joueur §e${target.displayName} §cn'est plus connecté.")
            return
        }

        val startDate  = LocalDateTime.now()

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            if (profileRepo.findActiveProfile(target.accountUuid)?.id != target.id) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    player.sendMessage("§c${target.displayName} §cn'est plus le personnage joué par ce joueur.")
                    ChooseTenantGui.open(player, parcel)
                })
                return@Runnable
            }
            val rentalId = rentalRepo.rentWithMember(
                Rental(parcelId = parcel.id, tenantProfileId = target.id, startDate = startDate),
                RentalMember(rentalId = 0, memberProfileId = target.id, role = RentalMemberRole.OWNER)
            )

            Bukkit.getScheduler().runTask(plugin, Runnable {
                when {
                    rentalId == 0 -> player.sendMessage("§cLa parcelle §e${parcel.label} §cn'est pas disponible.")
                    rentalId < 0  -> player.sendMessage("§cErreur lors de la location.")
                    else -> {
                        val regionOk = Bukkit.getWorld(parcel.world)?.let { world ->
                            WorldGuard.getInstance().platform.regionContainer
                                .get(BukkitAdapter.adapt(world))
                                ?.getRegion(parcel.regionId)
                                ?.members?.addPlayer(target.accountId)
                        } != null
                        plugin.parcelCache.onParcelRented(parcel.label, target.id)
                        plugin.memberCache.onMemberAdded(parcel.label, target)
                        
                        if (regionOk) {
                            player.sendMessage("§a✔ §e${target.displayName} §aest maintenant locataire de la parcelle §e${parcel.label} §a!")
                        }
                        else {
                            plugin.logger.warning("Région WorldGuard '${parcel.regionId}' introuvable !")
                            player.sendMessage("§6⚠ Location enregistrée, mais la région WorldGuard §e${parcel.regionId} §6est introuvable.")
                        }
                        val targetPlayer = Bukkit.getPlayer(target.accountId)
                        if (targetPlayer != null) {
                            player.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
                                .values.forEach { player.world.dropItemNaturally(player.location, it) }
                            player.sendMessage("§aVous avez reçu la clé de §e${parcel.label}§a : remettez-la à §e${target.displayName}§a.")
                            targetPlayer.sendMessage("§aVous êtes maintenant locataire de la parcelle §e${parcel.label}§a.")
                        }
                    }
                }
                AgentMenuGui.open(player)
            })
        })
    }
    
    companion object {
        private const val SLOT_HEAD = 13
        private const val SLOT_VALID = 30
        private const val SLOT_CANCEL = 32
        
        private val plugin get() = BbParcels.instance
        private val rentalRepo by lazy { RentalRepository(BbParcels.instance.databaseManager) }
        private val profileRepo by lazy { ProfileRepository(BbParcels.instance.databaseManager) }
    }
}