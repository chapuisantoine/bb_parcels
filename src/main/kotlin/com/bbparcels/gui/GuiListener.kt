package com.bbparcels.gui

import com.bbparcels.BbParcels
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

class GuiListener(private val plugin: BbParcels): Listener {
    
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val gui = event.view.topInventory.holder as? BbGui ?: return
        event.isCancelled = true
        
        if (event.clickedInventory != event.view.topInventory) return
        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return
        if (gui.permission != null && !player.hasPermission(gui.permission!!)) {
            player.closeInventory()
            return
        }

        Bukkit.getScheduler().runTask(plugin, Runnable { gui.onClick(player, event.slot, item)})
    }
    
    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is BbGui) event.isCancelled = true
    }
}