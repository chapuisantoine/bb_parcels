package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ParcelStatus
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

    
class AgentMenuGui private constructor(
    private val parcels: List<ParcelWithOwner>,
    private val page: Int
) : BbGui("[BB] Parcelles"){

    override val permission = "bbparcels.agentImmo"
    
    private val totalPages = ((parcels.size - 1) / CONTENT_SLOTS.size).coerceAtLeast(0)
    
    init {
        Utils.fillBorders(inventory)
        parcels.drop(page * CONTENT_SLOTS.size)
            .take(CONTENT_SLOTS.size)
            .forEachIndexed { i, parcel -> inventory.setItem(CONTENT_SLOTS[i], buildParcelItem(parcel)) }
            if (page > 0) inventory.setItem(SLOT_PREV, Utils.buildNavItem(Material.ARROW ,"§ePage précédente"))
            if (page < totalPages) inventory.setItem(SLOT_NEXT, Utils.buildNavItem(Material.ARROW, "§ePage suivante"))
    }

    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_PREV -> AgentMenuGui(parcels, page - 1).open(player)
            SLOT_NEXT -> AgentMenuGui(parcels, page + 1).open(player)
            in CONTENT_SLOTS -> {
                val parcel = parcels.getOrNull(page * CONTENT_SLOTS.size + CONTENT_SLOTS.indexOf(slot)) ?: return
                ParcelDetailGui.open(player, parcel)
            }
        }
    }
    
    private fun buildParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = when(parcel.status) {
            ParcelStatus.AVAILABLE -> Material.LIME_CONCRETE
            ParcelStatus.RENTED -> Material.RED_CONCRETE
            ParcelStatus.DISABLED -> Material.GRAY_CONCRETE
        }
        
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§f${parcel.label}")
        
        meta.lore = buildList { 
            add("§7Statut : §e${Utils.statusLabel(parcel.status)}")
            if (parcel.ownerUuid != null) {
                val ownerName = Utils.playerName(parcel.ownerUuid)
                add("§7Locataire : §a$ownerName")
            }
            
            add("")
            add("§eCliquez pour voir le détail")
        }
        item.itemMeta = meta
        return item
    }
    
    companion object {
        private val CONTENT_SLOTS = (10..16).toList() + (19..25).toList()
        private const val SLOT_PREV = 39
        private const val SLOT_NEXT = 41
        private val parcelRepo by lazy  {ParcelRepository(BbParcels.instance.databaseManager)}
        
        fun open(player: Player, page: Int = 0) {
            val plugin = BbParcels.instance
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val parcels = parcelRepo.findAllWithOwners()
            
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    val total = ((parcels.size -1) / CONTENT_SLOTS.size).coerceAtLeast(0)
                    AgentMenuGui(parcels, page.coerceIn(0, total)).open(player)
            })
        })
    }
    }
}