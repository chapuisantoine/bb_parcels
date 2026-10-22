package com.bbparcels.gui.agent

import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ChooseTenantGui (
    private val parcel: ParcelWithOwner,
    private val members: List<String>,
    private val page: Int
) : BbGui("[BB] Choisir un locataire") {
    
    override val permission = "bbparcels.agentImmo"
    
    private val totalPages = ((members.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)
    
    init {
        Utils.fillBorders(inventory)
        members.drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { i, member -> 
                inventory.setItem(CONTENT_SLOTS[i], Utils.buildHeadItem(member))
            }
        if (page > 0) 
            inventory.setItem(SLOT_PREV, Utils.buildNavItem(Material.ARROW, "§ePage précédente"))
        if (page < totalPages)
            inventory.setItem(SLOT_NEXT, Utils.buildNavItem(Material.ARROW, "§ePage suivante"))
        inventory.setItem(SLOT_RETURN, Utils.buildNavItem(Material.BARRIER, "§7Retour"))
    }
    
    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_PREV   -> ChooseTenantGui(parcel, members, page - 1).open(player)
            SLOT_NEXT   -> ChooseTenantGui(parcel, members, page + 1).open(player)
            SLOT_RETURN -> ParcelDetailGui.open(player, parcel)
            in CONTENT_SLOTS -> {
                val target = Bukkit.getPlayer(
                    members.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)) ?: return
                )?: return player.sendMessage("§cCe joueur n'est plus connecté.")
                ConfirmRentGui(parcel, target).open(player)
            }
        }
    }
    
    companion object {
        private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
        private const val SLOT_PREV = 39
        private const val SLOT_NEXT = 41
        private const val SLOT_RETURN = 43
    }
}