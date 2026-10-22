package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelWithOwner
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

class ConfirmUnrentGui(
    private val parcel: ParcelWithOwner
) : BbGui("[BB] Résilier la parcelle ?") {

    override val permission = "bbparcels.agentImmo"
    
    init {
        Utils.fillBorders(inventory)
        parcel.ownerUuid?.let {inventory.setItem(SLOT_HEAD, Utils.buildHeadItem(it))}
        inventory.setItem(SLOT_VALID, Utils.buildNavItem(Material.GREEN_CONCRETE, "Valider"))
        inventory.setItem(SLOT_CANCEL, Utils.buildNavItem(Material.RED_CONCRETE, "Annuler"))
    }


    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_VALID -> {
                player.closeInventory()
                unrentParcel(player, parcel)
            }
            SLOT_CANCEL -> ParcelDetailGui.open(player, parcel)
        }
    }

    private fun unrentParcel(player: Player, parcel: ParcelWithOwner) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            val removedMembers = rental?.let { rentalRepo.terminateWithMembers(it.id, parcel.id) }

            Bukkit.getScheduler().runTask(plugin, Runnable {
                when {
                    rental == null ->
                        player.sendMessage("§cAucune location active trouvée pour §e${parcel.label}§c.")
                    removedMembers == null ->
                        player.sendMessage("§cErreur lors de la résiliation (déjà résiliée ?).")
                    else -> {
                        val uuids = removedMembers.map { UUID.fromString(it) }
                        plugin.memberCache.onRentalEnded(parcel.label)
                        plugin.parcelCache.onParcelUnrented(parcel.label)
                        Bukkit.getWorld(parcel.world)?.let { world ->
                            val region = WorldGuard.getInstance().platform.regionContainer
                                .get(BukkitAdapter.adapt(world))
                                ?.getRegion(parcel.regionId)
                            if (region == null)
                                plugin.logger.warning("Région WorldGuard '${parcel.regionId}' introuvable lors du unrent !")
                            else
                                uuids.forEach { region.members.removePlayer(it) }
                        }

                        uuids.forEach {
                            Bukkit.getPlayer(it)?.sendMessage("§cLa location de la parcelle §e${parcel.label} §ca été résiliée.")
                        }

                        val tenantName = Utils.playerName(rental.tenantUuid)
                        player.sendMessage("§a✔ La location §e${parcel.label} §ade §e$tenantName §aa été résiliée.")
                    }
                }
                if (player.isOnline) AgentMenuGui.open(player)
            })
        })
    }
    
    companion object {
        private const val SLOT_HEAD = 13
        private const val SLOT_VALID = 30
        private const val SLOT_CANCEL = 32
        
        private val plugin get() = BbParcels.instance
        private val rentalRepo by lazy { RentalRepository(BbParcels.instance.databaseManager) }
    }
}