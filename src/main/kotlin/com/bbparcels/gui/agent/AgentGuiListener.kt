package com.bbparcels.gui.agent

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class AgentGuiListener : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title
        //val slot = event.rawSlot
        if (!title.startsWith("[BB]")) return
        event.isCancelled = true
        
        val player = event.whoClicked as? Player ?: return
        if (event.currentItem == null) return
        
        when (title) {
            "[BB] Parcelles"       -> AgentMenuGui.handleClick(player, event.rawSlot)
            "[BB] Détails de la parcelle"   -> ParcelDetailGui.hanbleClick(player, event.rawSlot, event.currentItem!!)
            "[BB] Membres de la parcelle" -> ListMembersGui.handleClick(player, event.rawSlot)
            "[BB] Louer la parcelle ?" -> ConfirmRentGui.handleClick(player, event.rawSlot)
            "[BB] Résilier la parcelle ?" -> ConfirmUnrentGui.handleClick(player, event.rawSlot)
            }
    }
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!player.hasPermission("bbparcels.agentImmo")) return
        
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        
        val item = player.inventory.itemInMainHand
        if (!isAgentTablet(item)) return
        
        event.isCancelled = true
        AgentMenuGui.open(player)        
    }
    
    
    companion object {
        const val TABLET_NAME = "§6Tablette Agent Immo"

        fun createTablet(): ItemStack {
            val item = ItemStack(Material.BOOK)
            val meta = item.itemMeta!!
            meta.setDisplayName(TABLET_NAME)
            meta.lore = listOf("§7Clic droit pour ouvrir le menu")
            item.itemMeta = meta
            return item
        }

        fun isAgentTablet(item: ItemStack): Boolean {
            if (item.type != Material.BOOK) return false
            return item.itemMeta?.displayName == TABLET_NAME
        }
    }
}