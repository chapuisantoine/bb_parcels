package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class AgentTabletListener : Listener {
    
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        
        if (!isAgentTablet(event.item)) return
        event.isCancelled = true
        
        val player = event.player
        if (!player.hasPermission("bbparcels.agentImmo")) {
            player.sendMessage("§cVous n'êtes pas habilité à utiliser cette tablette.")
            return  
        }       
        AgentMenuGui.open(player)        
    }
    
    companion object {
        private val TABLET_KEY = NamespacedKey(BbParcels.instance, "agent_tablet")

        fun createTablet(): ItemStack {
            val item = ItemStack(Material.BOOK)
            val meta = item.itemMeta!!
            meta.setDisplayName("§6Tablette Agent Immo")
            meta.lore = listOf("§7Clic droit pour ouvrir le menu")
            meta.persistentDataContainer.set(TABLET_KEY, PersistentDataType.BYTE, 1)
            item.itemMeta = meta
            return item
        }

        fun isAgentTablet(item: ItemStack?): Boolean =
            item?.itemMeta?.persistentDataContainer?.has(TABLET_KEY, PersistentDataType.BYTE) == true
    }
}