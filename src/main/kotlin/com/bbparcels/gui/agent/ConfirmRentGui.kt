package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.gui.owner.OwnerGuiListener
import com.bbparcels.model.*
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.LocalDateTime
import java.util.*

object ConfirmRentGui {
    
    private val logger = BbParcels.instance.logger
    
    private val cachedChoosenPlayer = mutableMapOf<UUID, Player>()
    private val cachedParcel = mutableMapOf<UUID, ParcelWithOwner>()
    
    private val parcelRepo = ParcelRepository(BbParcels.instance.databaseManager)
    private val rentalRepo = RentalRepository(BbParcels.instance.databaseManager)
    
    private val BORDER_SLOTS  = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    private const val SLOT_HEAD = 13
    private const val SLOT_VALID = 30
    private const val SLOT_UNVALID = 32
    
    private const val SLOT_RETURN = 43
    
    fun open(player: Player, parcel: ParcelWithOwner, choosenPlayer: Player) {
        cachedChoosenPlayer[player.uniqueId] = choosenPlayer
        cachedParcel[player.uniqueId]  = parcel
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Louer la parcelle ?")
        
        fillBorders(inv)
        
        inv.setItem(SLOT_HEAD, buildHeadSkull(choosenPlayer))
        inv.setItem(SLOT_VALID, buildNavItem(Material.GREEN_CONCRETE, "Valider"))
        inv.setItem(SLOT_UNVALID, buildNavItem(Material.RED_CONCRETE, "Annuler"))
        
        player.openInventory(inv)
    }
    
    fun handleClick(player: Player, slot: Int) {
        val parcel = cachedParcel[player.uniqueId] ?: return
        val choosenPlayer = cachedChoosenPlayer[player.uniqueId] ?: return
        when (slot) {
            SLOT_VALID -> {
                rentParcel(parcel, choosenPlayer, player)
                AgentMenuGui.open(player)
                choosenPlayer.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
                choosenPlayer.sendMessage("§aVous avez reçu la clé de la parcelle §e${parcel.label}§a.")
                }
            SLOT_UNVALID -> ParcelDetailGui.open(player, parcel)
        }
    }
    
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
    private fun buildHeadSkull(player: Player): ItemStack {
        val item    = ItemStack(Material.PLAYER_HEAD)
        val meta    = item.itemMeta as SkullMeta
        meta.owningPlayer = player
        meta.setDisplayName("§f${player.name}")
        item.itemMeta = meta
        return item
    }
    
    
    private fun fillBorders(inv: Inventory) {
        for (slot in BORDER_SLOTS) {
            val material = if (slot % 2 == 0) Material.BLUE_STAINED_GLASS_PANE else Material.CYAN_STAINED_GLASS_PANE
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§r")
            item.itemMeta = meta
            inv.setItem(slot, item)
        }
    }
    
    private fun rentParcel(parcel: ParcelWithOwner, target: Player, player: Player): Boolean {
        if (target == null) {
            player.sendMessage("§c Le joueur §e${target.name} §cn'est pas connecté.")
            return true
        }
        
        val parcelStatus = parcelRepo.findParcelStatusById(parcel.id)
        
        if (parcelStatus != ParcelStatus.AVAILABLE) {
            player.sendMessage("§cLa parcelle §e${parcel.label} §cn'est pas disponible.")
            return true
        }
        
        val startDate = LocalDateTime.now()
        
        val newIdRental = try {
            rentalRepo.rentWithMember(
                Rental(
                    parcelId   = parcel.id,
                    tenantUuid = player.uniqueId.toString(),
                    startDate  = startDate
                ),
                RentalMember(
                    rentalId   = 0,
                    memberUuid = player.uniqueId.toString(),
                    role       = RentalMemberRole.OWNER
                )
            )
        } catch (e: Exception) {
            logger.severe("Erreur lors de la création de la location : ${e.message}")
            player.sendMessage("§cErreur lors de l'insertion en base.")
            return true
        }
        
        if (newIdRental > 0) {
            parcelRepo.updateParcelStatus(parcel.id, ParcelStatus.RENTED)

            val world = Bukkit.getWorld(parcel.world)
            if (world != null) {
                val regionManager = WorldGuard.getInstance()
                    .platform.regionContainer
                    .get(BukkitAdapter.adapt(world))

                val region = regionManager?.getRegion(parcel.regionId)
                if (region != null) {
                    region.members.addPlayer(player.uniqueId)
                    regionManager.save()
                } else {
                    logger.warning("Région WorldGuard '${parcel.regionId}' introuvable !")
                }
            }

            player.sendMessage("§a✔ §e${target.name} §aest maintenant locataire de la Parcelle §e${parcel.label} §a!")
        }
        else
            player.sendMessage("§cErreur lors de la location.")
        
        return true
    }
    
    
}